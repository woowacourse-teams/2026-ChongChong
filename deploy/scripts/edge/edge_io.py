"""기존 SSH, curl, Docker 명령을 사용하는 edge 운영 경계."""
from dataclasses import dataclass
import hashlib
import json
import os
from pathlib import Path
import re
import shlex
import shutil
import subprocess
import tarfile
import tempfile
import uuid
from typing import Dict, Optional, Sequence

from edge_state import ControlError, Identity, atomic_write

FILES = ("backend.env", "image.env", "docker-compose.yml", "docker-compose.backend.yml")


def fingerprint(bundle: Path) -> str:
    digest = hashlib.sha256()
    for name in FILES:
        digest.update(name.encode() + b"\0" + (bundle / name).read_bytes() + b"\0")
    return digest.hexdigest()


def command(args: Sequence[str], *, data: Optional[bytes] = None, timeout: int = 30) -> bytes:
    try:
        result = subprocess.run(args, input=data, capture_output=True, timeout=timeout, check=False)
    except (OSError, subprocess.TimeoutExpired) as error:
        raise ControlError("명령을 실행할 수 없거나 제한 시간을 초과했습니다: " + args[0]) from error
    if result.returncode:
        # 환경·Docker/SSH 오류에는 비밀값이 포함될 수 있어 stderr를 그대로 출력하지 않는다.
        raise ControlError("명령 실패: " + args[0])
    return result.stdout


@dataclass(frozen=True)  # noqa: SLOTS_OK — 기존 배포 환경의 Python 3.9 호환성 유지.
class Config:
    hosts: Dict[str, str]
    server_name: str
    ssh_user: str
    ssh_key: str
    known_hosts: str
    failure_threshold: int = 3
    success_threshold: int = 3
    soak_seconds: int = 600


def configuration(root: Path) -> Config:
    keys = ("BACKEND_A_HOST", "BACKEND_B_HOST", "SERVER_NAME", "SSH_USER", "SSH_KEY", "SSH_KNOWN_HOSTS",
            "EDGE_FAILURE_THRESHOLD", "EDGE_SUCCESS_THRESHOLD", "EDGE_SOAK_SECONDS")
    script = 'source "$1"; shift; for key in "$@"; do printf "%s\\0" "${!key:-}"; done'
    values = command(["bash", "-c", script, "edge-config", str(root / "config/edge.env"), *keys]).decode().split("\0")
    settings = dict(zip(keys, values))
    for key in keys[:6]:
        if not settings[key]:
            raise ControlError("필수 edge 설정 누락: " + key)
    for key in keys[:2]:
        if not re.fullmatch(r"[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+", settings[key]):
            raise ControlError("backend 주소 형식 오류")
    if settings[keys[0]] == settings[keys[1]]:
        raise ControlError("backend 주소가 서로 같음")
    if not re.fullmatch(r"[A-Za-z0-9.-]+", settings["SERVER_NAME"]):
        raise ControlError("SERVER_NAME 형식 오류")
    if not re.fullmatch(r"[a-z_][a-z0-9_-]*", settings["SSH_USER"]):
        raise ControlError("SSH_USER 형식 오류")
    counts = [int(settings[key] or default) for key, default in zip(keys[6:], (3, 3, 600))]
    if min(counts) < 1:
        raise ControlError("임계값과 안정화 시간은 양수여야 합니다")
    return Config({"a": settings[keys[0]], "b": settings[keys[1]]}, settings[keys[2]],
                  settings[keys[3]], settings[keys[4]], settings[keys[5]], *counts)


class Operations:
    def __init__(self, root: Path, config: Optional[Config] = None):
        self.root = root
        self.config = config or configuration(root)
        self.upstream = root / "edge-routing/upstream.conf"

    def ssh(self, slot: str, script: str, data: Optional[bytes] = None, timeout: int = 30) -> bytes:
        return command(["ssh", "-o", "BatchMode=yes", "-o", "StrictHostKeyChecking=yes",
                        "-o", "ConnectTimeout=5", "-o", "ServerAliveInterval=5", "-o", "ServerAliveCountMax=2",
                        "-o", "UserKnownHostsFile=" + self.config.known_hosts, "-i", self.config.ssh_key,
                        self.config.ssh_user + "@" + self.config.hosts[slot], script], data=data, timeout=timeout)

    def ready(self, slot: str) -> bool:
        try:
            for path in ("/actuator/health/readiness", "/actuator/health"):
                body = command(["curl", "-fsS", "--connect-timeout", "2", "--max-time", "3",
                                "http://" + self.config.hosts[slot] + ":8080" + path], timeout=4)
                if json.loads(body).get("status") != "UP":
                    return False
            return True
        except (ControlError, ValueError, AttributeError):
            return False

    def verify(self, slot: str) -> bool:
        try:
            response = command(["curl", "-fsS", "--connect-timeout", "2", "--max-time", "5", "-i",
                                "--resolve", self.config.server_name + ":443:127.0.0.1",
                                "https://" + self.config.server_name + "/actuator/health"], timeout=6).decode()
            headers, body = response.split("\r\n\r\n", 1)
            expected = "x-deploy-backend: " + self.config.hosts[slot] + ":8080"
            return expected in [line.lower().strip() for line in headers.splitlines()] and json.loads(body).get("status") == "UP"
        except (ControlError, ValueError, AttributeError):
            return False

    def routed_slot(self) -> str:
        content = self.upstream.read_text().strip()
        for slot, host in self.config.hosts.items():
            if content == "upstream chongchong_backend { server " + host + ":8080; }":
                return slot
        raise ControlError("알 수 없는 upstream 설정입니다")

    def replace(self, slot: str) -> None:
        previous = self.upstream.read_text()
        atomic_write(self.upstream, "upstream chongchong_backend { server " + self.config.hosts[slot] + ":8080; }\n")
        try:
            command(["docker", "exec", "chongchong-nginx", "nginx", "-t"])
            command(["docker", "exec", "chongchong-nginx", "nginx", "-s", "reload"])
        except ControlError:
            atomic_write(self.upstream, previous)
            raise

    def drained(self) -> bool:
        workers = command(["docker", "top", "chongchong-nginx", "-eo", "pid,args"])
        return b"worker process is shutting down" not in workers

    def identity(self, slot: str) -> Identity:
        script = ('import hashlib,json,pathlib,subprocess; '
                  'p=pathlib.Path("/opt/chongchong/current-backend-release"); d=hashlib.sha256(); '
                  '[d.update(n.encode()+b"\\0"+(p/n).read_bytes()+b"\\0") for n in ' + repr(FILES) + ']; '
                  'c=json.loads(subprocess.check_output(["docker","inspect","chongchong-backend"]))[0]; '
                  'i=json.loads(subprocess.check_output(["docker","image","inspect",c["Image"]]))[0]; '
                  'assert c["State"]["Running"]; '
                  'r=c["Config"]["Image"]; '
                  'r=r if "@sha256:" in r else i["RepoDigests"][0]; '
                  'normalize=lambda v:v.removeprefix("docker.io/").removeprefix("index.docker.io/"); '
                  'assert normalize(r) in [normalize(v) for v in i["RepoDigests"]]; '
                  'print(json.dumps({"release":d.hexdigest(),"image":c["Image"],"reference":r}))')
        data = json.loads(self.ssh(slot, "sudo -n flock -s -n /opt/chongchong/releases/deploy.lock python3 -c " + shlex.quote(script)))
        identity = Identity(**data)
        if not re.fullmatch(r"[a-f0-9]{64}", identity.release) or not re.fullmatch(r"sha256:[a-f0-9]{64}", identity.image):
            raise ControlError("backend 릴리스 식별자가 잘못되었습니다")
        if not re.fullmatch(r"[A-Za-z0-9._:/-]+@sha256:[a-f0-9]{64}", identity.reference):
            raise ControlError("backend 이미지 digest가 없습니다")
        return identity

    def retain(self, slot: str, identity: Identity, clean: bool = False, protected=()) -> None:
        from dataclasses import asdict
        runner = Path(__file__).with_name("backend_retention.py")
        action = "clean" if clean else "record"
        payload = {**asdict(identity), "protected_references": list(protected)}
        self.ssh(slot, "sudo -n python3 - " + action + " " + shlex.quote(json.dumps(payload)),
                 runner.read_bytes(), timeout=120)

    def snapshot(self, artifact: Path, saved: bool = False) -> Path:
        directory = self.root / "edge-state/bundles"
        directory.mkdir(parents=True, exist_ok=True, mode=0o700)
        bundle = Path(tempfile.mkdtemp(prefix="release-", dir=str(directory)))
        for name in FILES:
            source = (self.root / "config/backend.env" if name == "backend.env" and not saved
                      else artifact / "config" / name if not saved and name.startswith("docker-compose")
                      else artifact / name)
            shutil.copyfile(source, bundle / name)
            (bundle / name).chmod(0o600)
        if (artifact / "compatibility.json").exists():
            shutil.copyfile(artifact / "compatibility.json", bundle / "compatibility.json")
        if (artifact / "image-pin").exists():
            shutil.copyfile(artifact / "image-pin", bundle / "image-pin")
        return bundle

    def release(self, slot: str, bundle: Path) -> Identity:
        runner = Path(__file__).with_name("backend-release.sh")
        if not runner.exists():
            runner = Path(__file__).resolve().parents[1] / "backend/backend-release.sh"
        remote_runner = "/opt/chongchong/deploy-control/backend-release-" + uuid.uuid4().hex + ".sh"
        self.ssh(slot, "sudo -n install -d -m 700 /opt/chongchong/deploy-control")
        self.ssh(slot, "sudo -n tee " + remote_runner + " >/dev/null", runner.read_bytes())
        with tempfile.TemporaryFile() as archive:
            with tarfile.open(fileobj=archive, mode="w") as tar:
                for name in (*FILES, "image-pin"):
                    if (bundle / name).exists():
                        tar.add(bundle / name, arcname=name)
            archive.seek(0)
            self.ssh(slot, "sudo -n bash " + remote_runner + " " + slot + " " +
                     self.config.hosts[slot], archive.read(), timeout=240)
        return self.identity(slot)

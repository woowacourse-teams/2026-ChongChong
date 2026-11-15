"""검증된 성공 릴리스 기록과 앱 이미지 정리. backend 배포와 같은 잠금을 사용한다."""
import fcntl
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import tempfile

FILES = ("backend.env", "image.env", "docker-compose.yml", "docker-compose.backend.yml")


def docker(*args):
    result = subprocess.run(["docker", *args], capture_output=True, text=True, timeout=30)
    if result.returncode:
        raise RuntimeError("Docker 확인/정리 실패")
    return result.stdout


def repository(reference):
    value = reference.split("@", 1)[0]
    if ":" in value.rsplit("/", 1)[-1]:
        value = value.rsplit(":", 1)[0]
    return value.removeprefix("docker.io/").removeprefix("index.docker.io/")


def save(path, records):
    fd, name = tempfile.mkstemp(dir=path.parent, prefix=".history-")
    with os.fdopen(fd, "w") as stream:
        json.dump(records, stream)
        stream.flush()
        os.fsync(stream.fileno())
    os.replace(name, path)


def retain(root, expected, clean=False):
    directory = root / "releases"
    with (directory / "deploy.lock").open("a") as lock, (directory / "retention.lock").open("a") as retention_lock:
        # 이미지 조회의 shared lock은 허용하고 실제 배포의 exclusive lock은 막는다.
        fcntl.flock(lock, fcntl.LOCK_SH | fcntl.LOCK_NB)
        fcntl.flock(retention_lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
        current = (root / "current-backend-release").resolve(strict=True)
        if current.parent != directory.resolve() or current.is_symlink():
            raise ValueError("릴리스 경로 오류")
        digest = hashlib.sha256()
        for name in FILES:
            digest.update(name.encode() + b"\0" + (current / name).read_bytes() + b"\0")
        container = json.loads(docker("inspect", "chongchong-backend"))[0]
        if (digest.hexdigest() != expected["release"] or container["Image"] != expected["image"] or
                not container["State"]["Running"]):
            raise ValueError("실행 릴리스 불일치; 정리 중지")
        if not re.fullmatch(r"[A-Za-z0-9._:/-]+@sha256:[a-f0-9]{64}", expected["reference"]):
            raise ValueError("불변 이미지 참조 필요")
        actual = json.loads(docker("image", "inspect", expected["image"]))[0]
        normalize = lambda value: value.removeprefix("docker.io/").removeprefix("index.docker.io/")
        if normalize(expected["reference"]) not in [normalize(r) for r in actual.get("RepoDigests", [])]:
            raise ValueError("실행 이미지 digest 불일치")
        path = directory / "successful-images.json"
        records = json.loads(path.read_text()) if path.exists() else []
        if not isinstance(records, list) or any(not isinstance(r, dict) or set(r) != {"release", "image", "reference"} or
                                               not all(isinstance(v, str) and v for v in r.values()) or
                                               not re.fullmatch(r"[a-f0-9]{64}", r["release"]) or
                                               not re.fullmatch(r"sha256:[a-f0-9]{64}", r["image"]) or
                                               not re.fullmatch(r"[A-Za-z0-9._:/-]+@sha256:[a-f0-9]{64}", r["reference"])
                                               for r in records):
            raise ValueError("성공 이력 오류")
        record = {key: expected[key] for key in ("release", "image", "reference")}
        records = [r for r in records if (r["release"], r["reference"]) != (record["release"], record["reference"])] + [record]
        save(path, records)
        if not clean:
            return []
        protected = {r["image"] for r in records[-3:]}
        references = expected.get("protected_references", [])
        if not isinstance(references, list) or any(not isinstance(ref, str) or not re.fullmatch(
                r"[A-Za-z0-9._:/-]+@sha256:[a-f0-9]{64}", ref) for ref in references):
            raise ValueError("복구 이미지 보호 참조 오류")
        for ref in references:
            protected.add(json.loads(docker("image", "inspect", ref))[0]["Id"])
        ids = docker("ps", "-aq").split()
        if ids:
            protected.update(c["Image"] for c in json.loads(docker("inspect", *ids)))
        repo = repository(expected["reference"])
        candidates = docker("image", "ls", "--no-trunc", "--quiet", repo).split()
        removed = []
        for image_id in sorted(set(candidates) - protected):
            info = json.loads(docker("image", "inspect", image_id))[0]
            refs = list(info.get("RepoTags") or []) + list(info.get("RepoDigests") or [])
            if not refs or any(repository(ref) != repo for ref in refs):
                continue
            # --force를 사용하지 않는다. 동시 컨테이너 사용은 Docker도 삭제를 거부한다.
            for reference in (info.get("RepoTags") or info.get("RepoDigests") or []):
                docker("image", "rm", reference)
            removed.append(image_id)
        return removed


def main():
    os.umask(0o077)
    if len(sys.argv) != 3 or sys.argv[1] not in ("record", "clean"):
        raise ValueError("record|clean 및 검증된 identity 필요")
    removed = retain(Path("/opt/chongchong"), json.loads(sys.argv[2]), sys.argv[1] == "clean")
    print(json.dumps({"removed": removed}))


if __name__ == "__main__":
    try:
        main()
    except (OSError, ValueError, KeyError, IndexError, TypeError, AttributeError, RuntimeError, subprocess.TimeoutExpired):
        print("릴리스 기록/정리 실패; 서비스는 유지됩니다", file=sys.stderr)
        sys.exit(1)

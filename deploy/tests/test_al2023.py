import os
from pathlib import Path
import subprocess
import tempfile
import unittest

SCRIPT = Path(__file__).resolve().parents[1] / "scripts/setup/check-al2023.sh"


class AL2023CheckTest(unittest.TestCase):
    def exercise(self, role, failure="", os_id="amzn", arch="aarch64"):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            release = root / "os-release"
            release.write_text("ID=" + os_id + "\nVERSION_ID=2023\n")
            script = root / "check.sh"
            script.write_text(SCRIPT.read_text().replace("source /etc/os-release", 'source "' + str(release) + '"'))
            binary = root / "bin"
            binary.mkdir()
            fake = """#!/bin/bash
name=${0##*/}
printf '%s %s\n' "$name" "$*" >> "$CALL_LOG"
if [[ "$name" == uname ]]; then echo "$TEST_ARCH"; fi
if [[ -n "$FAIL_COMMAND" && "$name $*" == *"$FAIL_COMMAND"* ]]; then exit 1; fi
"""
            for name in ("uname", "docker", "python3", "curl", "flock", "timeout", "systemctl", "ssh", "certbot"):
                target = binary / name
                target.write_text(fake)
                target.chmod(0o755)
            log = root / "calls"
            result = subprocess.run(["bash", str(script), role], capture_output=True, text=True,
                                    env={**os.environ, "PATH": str(binary) + ":" + os.environ["PATH"],
                                         "CALL_LOG": str(log), "TEST_ARCH": arch, "FAIL_COMMAND": failure})
            return result, log.read_text() if log.exists() else ""

    def test_backend_does_not_require_edge_services(self):
        result, calls = self.exercise("backend", "codedeploy")
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertNotIn("codedeploy", calls)
        self.assertNotIn("certbot-renew", calls)

    def test_edge_requires_active_and_enabled_services(self):
        for failure in ("codedeploy-agent", "chongchong-certbot-renew.timer", "docker compose", "is-enabled --quiet docker"):
            with self.subTest(failure=failure):
                result, _ = self.exercise("edge", failure)
                self.assertNotEqual(result.returncode, 0)
        result, calls = self.exercise("edge")
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertNotIn("enable --now", calls)
        self.assertNotIn("restart", calls)

    def test_wrong_os_architecture_or_role_fails_before_docker(self):
        for role, os_id, arch in (("other", "amzn", "aarch64"), ("edge", "ubuntu", "aarch64"), ("edge", "amzn", "x86_64")):
            result, calls = self.exercise(role, os_id=os_id, arch=arch)
            self.assertNotEqual(result.returncode, 0)
            self.assertNotIn("docker", calls)

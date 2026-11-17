"""빌드 이미지와 다른 저장소의 digest를 선택하는 회귀를 검사한다."""
import os
from pathlib import Path
import subprocess
import tempfile
from typing import Optional
import unittest


SCRIPT = Path(__file__).resolve().parents[1] / "scripts/edge/record-image-pin.sh"
DIGEST = "a" * 64


class ImagePinTest(unittest.TestCase):
    def run_pin(self, digests: str, image: str = "team/backend:commit", exit_code: int = 0) -> tuple[int, Optional[str]]:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            docker = root / "docker"
            docker.write_text('#!/bin/sh\nprintf "%s\\n" "$TEST_DIGESTS"\nexit "$TEST_EXIT"\n')
            docker.chmod(0o700)
            output = root / "image-pin"
            result = subprocess.run(
                ["bash", str(SCRIPT), image, str(output)], capture_output=True, text=True,
                env={**os.environ, "PATH": str(root) + os.pathsep + os.environ["PATH"],
                     "TEST_DIGESTS": digests, "TEST_EXIT": str(exit_code)},
            )
            return result.returncode, output.read_text() if output.exists() else None

    def test_selects_digest_for_the_built_repository(self):
        # Given: another repository appears first in Docker's digest list.
        digests = "other/backend@sha256:" + "b" * 64 + "\nteam/backend@sha256:" + DIGEST
        # When: the build records the tag it just pushed.
        status, pin = self.run_pin(digests)
        # Then: deployment is pinned to the matching repository.
        self.assertEqual((status, pin), (0, "team/backend@sha256:" + DIGEST + "\n"))

    def test_accepts_explicit_docker_hub_prefix(self):
        # Given: Docker normalizes the repository with its registry prefix.
        digest = "docker.io/team/backend@sha256:" + DIGEST
        # When: the artifact is generated for the same repository.
        status, pin = self.run_pin(digest, "docker.io/team/backend:commit")
        # Then: both references resolve to the same pinned image.
        self.assertEqual((status, pin), (0, "team/backend@sha256:" + DIGEST + "\n"))

    def test_rejects_absent_wrong_or_malformed_digest(self):
        for digest in ("", "other/backend@sha256:" + DIGEST, "team/backend@sha256:short"):
            with self.subTest(digest=digest):
                # Given: no valid digest matches the built repository.
                # When: pin generation runs.
                status, pin = self.run_pin(digest)
                # Then: no deployable pin is produced.
                self.assertNotEqual(status, 0)
                self.assertIsNone(pin)

    def test_docker_failure_does_not_create_artifact(self):
        # Given: Docker emits output but exits with an error.
        # When: pin generation runs.
        status, pin = self.run_pin("team/backend@sha256:" + DIGEST, exit_code=1)
        # Then: partial output cannot authorize a deployment.
        self.assertNotEqual(status, 0)
        self.assertIsNone(pin)


if __name__ == "__main__":
    unittest.main()

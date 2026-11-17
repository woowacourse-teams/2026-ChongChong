"""실제 HTTP 응답과 curl로 준비 상태 판정 경계를 검증한다."""
from __future__ import annotations

from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import pathlib
import sys
import tempfile
import threading
import unittest
from typing import Final, Mapping, Optional, Sequence
from unittest.mock import patch

SCRIPTS: Final = pathlib.Path(__file__).resolve().parents[1] / "scripts/edge"
sys.path.insert(0, str(SCRIPTS))

import edge_io


class HealthProbeTest(unittest.TestCase):
    def probe(self, responses: Mapping[str, tuple[int, bytes]]) -> bool:
        """실제 curl 명령의 포트만 테스트 서버의 임시 포트로 연결한다."""
        class Handler(BaseHTTPRequestHandler):
            def do_GET(self) -> None:
                status, body = responses[self.path]
                self.send_response(status)
                self.send_header("Content-Length", str(len(body)))
                self.end_headers()
                self.wfile.write(body)

            def log_message(self, format: str, *args: str) -> None:
                return None

        real_command = edge_io.command
        with tempfile.TemporaryDirectory() as directory:
            with ThreadingHTTPServer(("127.0.0.1", 0), Handler) as server:
                thread = threading.Thread(target=server.serve_forever, daemon=True)
                thread.start()
                port = server.server_address[1]

                def command(args: Sequence[str], *, data: Optional[bytes] = None,
                            timeout: int = 30) -> bytes:
                    routed = [arg.replace("127.0.0.1:8080", "127.0.0.1:" + str(port)) for arg in args]
                    return real_command(routed, data=data, timeout=timeout)

                try:
                    config = edge_io.Config({"a": "127.0.0.1", "b": "127.0.0.2"},
                                            "localhost", "test", "unused", "unused")
                    with patch.object(edge_io, "command", side_effect=command):
                        return edge_io.Operations(pathlib.Path(directory), config).ready("a")
                finally:
                    server.shutdown()
                    thread.join(timeout=5)

    def test_backend_is_eligible_only_when_both_http_health_checks_are_up(self) -> None:
        # Given: both endpoints report UP over a real local HTTP connection.
        responses = {
            "/actuator/health/readiness": (200, b'{"status":"UP"}'),
            "/actuator/health": (200, b'{"status":"UP"}'),
        }
        # When / Then: the real curl-backed probe accepts the backend.
        self.assertTrue(self.probe(responses))

    def test_readiness_up_does_not_hide_shared_dependency_failure(self) -> None:
        # Given: readiness is UP but the aggregate dependency check fails.
        responses = {
            "/actuator/health/readiness": (200, b'{"status":"UP"}'),
            "/actuator/health": (503, b'{"status":"DOWN"}'),
        }
        # When / Then: the backend is not accepted as a failover target.
        self.assertFalse(self.probe(responses))

    def test_invalid_health_responses_are_not_accepted(self) -> None:
        for response in ((200, b'{"status":"DOWN"}'), (200, b'not json'),
                         (200, b'[]'), (503, b'{"status":"UP"}')):
            with self.subTest(response=response):
                # Given: readiness returns a malformed or unhealthy response.
                responses = {
                    "/actuator/health/readiness": response,
                    "/actuator/health": (200, b'{"status":"UP"}'),
                }
                # When / Then: an HTTP 200 alone cannot admit the backend.
                self.assertFalse(self.probe(responses))


class ReleaseSnapshotTest(unittest.TestCase):
    def test_normal_artifact_uses_config_compose_and_current_environment(self):
        with tempfile.TemporaryDirectory() as directory:
            root = pathlib.Path(directory)
            (root / "config").mkdir()
            (root / "config/backend.env").write_text("current settings")
            artifact = root / "artifact"
            (artifact / "config").mkdir(parents=True)
            for name in ("docker-compose.yml", "docker-compose.backend.yml"):
                (artifact / "config" / name).write_text("current compose " + name)
            (artifact / "image.env").write_text("image")
            (artifact / "image-pin").write_text("digest")
            (artifact / "backend.env").write_text("stale artifact settings")
            ops = edge_io.Operations(root, edge_io.Config({}, "", "", "", ""))
            bundle = ops.snapshot(artifact)
            self.assertEqual((bundle / "backend.env").read_text(), "current settings")
            self.assertEqual((bundle / "docker-compose.yml").read_text(), "current compose docker-compose.yml")
            self.assertEqual((bundle / "image-pin").read_text(), "digest")
            self.assertEqual((bundle / "backend.env").stat().st_mode & 0o777, 0o600)

    def test_restore_keeps_saved_environment_and_flat_compose(self):
        with tempfile.TemporaryDirectory() as directory:
            root = pathlib.Path(directory)
            (root / "config").mkdir()
            (root / "config/backend.env").write_text("new settings")
            saved = root / "saved"
            saved.mkdir()
            for name in edge_io.FILES:
                (saved / name).write_text("old " + name)
            ops = edge_io.Operations(root, edge_io.Config({}, "", "", "", ""))
            bundle = ops.snapshot(saved, saved=True)
            for name in edge_io.FILES:
                self.assertEqual((bundle / name).read_text(), "old " + name)
                self.assertEqual((saved / name).read_text(), "old " + name)


if __name__ == "__main__":
    unittest.main()

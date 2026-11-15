"""운영 CLI의 실제 별도 프로세스에서 hold/status 동작을 확인한다."""
from __future__ import annotations

import json
import os
import pathlib
import subprocess
import tempfile
import unittest

from test_edge_failover import SCRIPTS, TestOperations
from edge_control import Controller
from edge_state import Slot, State


class EdgeCliTest(unittest.TestCase):
    def setUp(self) -> None:
        temporary = tempfile.TemporaryDirectory()
        self.addCleanup(temporary.cleanup)
        self.root = pathlib.Path(temporary.name)
        self.ops = TestOperations(self.root)
        self.ops.replace("a")
        self.controller = Controller(self.root, self.ops)
        self.controller.store.write(State("a", {
            slot: Slot("release-v1", "sha256:v1", True, "bundle-v1") for slot in ("a", "b")
        }))
        (self.root / "config").mkdir()
        (self.root / "config/edge.env").write_text(
            "BACKEND_A_HOST=10.0.0.1\nBACKEND_B_HOST=10.0.0.2\nSERVER_NAME=localhost\n"
            "SSH_USER=test\nSSH_KEY=/unused/key\nSSH_KNOWN_HOSTS=/unused/known_hosts\n"
        )

    def run_cli(self, action: str) -> subprocess.CompletedProcess[str]:
        return subprocess.run(
            ["bash", str(SCRIPTS / "edge-control.sh"), action],
            env={**os.environ, "CHONGCHONG_APP_ROOT": str(self.root)},
            capture_output=True, text=True, timeout=10,
        )

    def test_hold_command_is_visible_to_a_separate_status_process(self) -> None:
        # Given: an initialized controller has automatic monitoring enabled.
        # When: an operator invokes the installed command boundary.
        held = self.run_cli("hold")
        self.assertEqual(held.returncode, 0, held.stderr)
        status = self.run_cli("status")
        # Then: a separate process reads the durable hold and unchanged route.
        self.assertEqual(status.returncode, 0, status.stderr)
        data = json.loads(status.stdout)
        self.assertTrue(data["hold"])
        self.assertEqual(data["active"], "a")

    def test_restore_missing_bundle_preserves_hold_in_real_cli(self) -> None:
        self.controller.hold(True)
        result = subprocess.run(
            ["bash", str(SCRIPTS / "edge-control.sh"), "restore", str(self.root / "missing"),
             str(self.root / "approval.json")],
            env={**os.environ, "CHONGCHONG_APP_ROOT": str(self.root)},
            capture_output=True, text=True, timeout=10)
        self.assertNotEqual(result.returncode, 0)
        self.assertTrue(self.controller.status().hold)
        self.assertNotIn("Traceback", result.stderr)
        self.assertEqual(self.controller.status().active, "a")

    def test_resume_command_cannot_clear_an_unresolved_switch(self) -> None:
        # Given: interrupted routing intent still needs reconciliation.
        state = self.controller.status()
        state.pending = {"from": "a", "to": "b", "phase": "degraded"}
        state.hold = True
        self.controller.store.write(state)
        # When: an operator tries to resume directly.
        result = self.run_cli("resume")
        # Then: the command refuses and preserves the intervention state.
        self.assertNotEqual(result.returncode, 0)
        self.assertTrue(self.controller.status().hold)
        self.assertIsNotNone(self.controller.status().pending)


if __name__ == "__main__":
    unittest.main()

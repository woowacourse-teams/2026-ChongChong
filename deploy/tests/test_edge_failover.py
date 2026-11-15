"""실제 파일과 프로세스 잠금으로 failover 제어기의 복구 경계를 검증한다."""
from __future__ import annotations

import json
import pathlib
import select
import subprocess
import sys
import tempfile
import unittest
from typing import Callable, Final, Optional
from unittest.mock import patch

SCRIPTS: Final = pathlib.Path(__file__).resolve().parents[1] / "scripts/edge"
sys.path.insert(0, str(SCRIPTS))

from edge_control import Controller
from edge_io import Config, Operations
from edge_state import ControlError, Identity, Slot, State, Store, atomic_write


class TestOperations(Operations):
    """원격 호스트 경계만 대체하며 상태·파일·잠금은 실제 구현을 사용한다."""

    def __init__(self, root: pathlib.Path) -> None:
        super().__init__(root, Config({"a": "10.0.0.1", "b": "10.0.0.2"},
                                    "localhost", "test", "unused", "unused"))
        self.health = {"a": True, "b": True}
        self.identities = {
            slot: Identity("release-v1", "sha256:v1", "test/app@sha256:v1")
            for slot in ("a", "b")
        }
        self.transitions: list[str] = []
        self.on_probe: Optional[Callable[[], None]] = None
        self.is_drained = True

    def retain(self, slot, identity, clean=False, protected=()):
        pass

    def ready(self, slot: str) -> bool:
        callback, self.on_probe = self.on_probe, None
        if callback is not None:
            callback()
        return self.health[slot]

    def identity(self, slot: str) -> Identity:
        return self.identities[slot]

    def verify(self, slot: str) -> bool:
        return self.routed_slot() == slot and self.health[slot]

    def replace(self, slot: str) -> None:
        self.transitions.append(slot)
        atomic_write(self.upstream,
                     "upstream chongchong_backend { server " + self.config.hosts[slot] + ":8080; }\n")

    def drained(self) -> bool:
        return self.is_drained


class FailoverControllerTest(unittest.TestCase):
    def setUp(self) -> None:
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.root = pathlib.Path(self.temporary.name)
        self.ops = TestOperations(self.root)
        self.ops.replace("a")
        self.ops.transitions.clear()
        self.controller = Controller(self.root, self.ops)
        self.controller.store.write(State("a", {
            "a": Slot("release-v1", "sha256:v1", True, "bundle-v1", "test/app@sha256:v1"),
            "b": Slot("release-v1", "sha256:v1", True, "bundle-v1", "test/app@sha256:v1"),
        }))

    def test_persistent_active_failure_switches_to_healthy_standby(self) -> None:
        # Given: active A has failed while B remains healthy.
        self.ops.health["a"] = False
        # When: failures span three independent controller instances.
        for _ in range(3):
            Controller(self.root, self.ops).monitor()
        # Then: B serves and the failed A cannot automatically rejoin.
        self.assertEqual(self.ops.routed_slot(), "b")
        self.assertEqual(self.ops.transitions, ["b"])
        state = self.controller.status()
        self.assertEqual(state.active, "b")
        self.assertFalse(state.slots["a"].eligible)

    def test_recovered_old_active_does_not_cause_automatic_failback(self) -> None:
        # Given: failover already selected B.
        self.ops.health["a"] = False
        for _ in range(3):
            self.controller.monitor()
        self.ops.transitions.clear()
        self.ops.health["a"] = True
        # When: A becomes healthy for repeated probes.
        for _ in range(4):
            self.controller.monitor()
        # Then: B remains active without another reload.
        self.assertEqual(self.ops.routed_slot(), "b")
        self.assertEqual(self.ops.transitions, [])

    def test_intermittent_failure_does_not_accumulate_into_failover(self) -> None:
        # Given: each A failure is followed by a healthy sample.
        # When: repeated isolated failures are observed.
        for healthy in (False, True, False, True, False):
            self.ops.health["a"] = healthy
            self.controller.monitor()
        # Then: the active route remains A.
        self.assertEqual(self.ops.routed_slot(), "a")
        self.assertEqual(self.ops.transitions, [])

    def test_both_failed_backends_do_not_trigger_switching(self) -> None:
        # Given: a shared outage makes both backends unhealthy.
        self.ops.health = {"a": False, "b": False}
        # When: failure thresholds are exceeded.
        for _ in range(4):
            self.controller.monitor()
        # Then: no unhealthy fallback is chosen.
        self.assertEqual(self.ops.routed_slot(), "a")
        self.assertEqual(self.ops.transitions, [])

    def test_excluded_deployment_target_cannot_receive_failover(self) -> None:
        # Given: B is explicitly excluded while being deployed.
        state = self.controller.status()
        state.phase = "deploying"
        state.slots["b"].eligible = False
        self.controller.store.write(state)
        self.ops.health["a"] = False
        # When: active A fails beyond the threshold.
        for _ in range(4):
            self.controller.monitor()
        # Then: readiness UP cannot override B's deployment exclusion.
        self.assertEqual(self.ops.routed_slot(), "a")
        self.assertEqual(self.ops.transitions, [])

    def test_hold_survives_controller_restart_and_prevents_switch(self) -> None:
        # Given: the operator persisted a hold before A failed.
        self.controller.hold(True)
        self.ops.health["a"] = False
        # When: new controller instances continue monitoring.
        for _ in range(4):
            Controller(self.root, self.ops).monitor()
        # Then: manual intervention remains authoritative.
        self.assertTrue(self.controller.status().hold)
        self.assertEqual(self.ops.transitions, [])

    def test_changed_standby_identity_is_not_promoted(self) -> None:
        # Given: B is healthy but its actual image differs from the verified one.
        self.ops.identities["b"] = Identity("release-v1", "sha256:unexpected")
        self.ops.health["a"] = False
        # When: the active failure reaches the threshold.
        for _ in range(3):
            self.controller.monitor()
        # Then: B is excluded and never receives requests.
        self.assertFalse(self.controller.status().slots["b"].eligible)
        self.assertEqual(self.ops.transitions, [])

    def test_probe_result_is_discarded_when_operator_changes_generation(self) -> None:
        # Given: the next sample would trigger failover.
        state = self.controller.status()
        state.failures["a"] = 2
        state.successes["b"] = 2
        self.controller.store.write(state)
        self.ops.health["a"] = False
        self.ops.on_probe = lambda: Controller(self.root, self.ops).hold(True)
        # When: hold changes the generation while probes run outside the lock.
        self.controller.monitor()
        # Then: the stale result cannot overwrite operator state or switch.
        self.assertTrue(self.controller.status().hold)
        self.assertEqual(self.ops.transitions, [])

    def test_restart_reconciles_intent_after_process_dies_during_switch(self) -> None:
        # Given: a child exits after writing B's route, before committing state.
        child_code = """
import os,pathlib,sys
sys.path.insert(0,sys.argv[1])
from test_edge_failover import TestOperations,Controller
class CrashAfterReplace(TestOperations):
    def replace(self, slot):
        super().replace(slot)
        os._exit(77)
root=pathlib.Path(sys.argv[2])
controller=Controller(root,CrashAfterReplace(root))
controller.ops.health['a']=False
for _ in range(3):
    controller.monitor()
"""
        result = subprocess.run([sys.executable, "-c", child_code, str(pathlib.Path(__file__).parent),
                                 str(self.root)], capture_output=True, timeout=10)
        self.assertEqual(result.returncode, 77, result.stderr)
        self.assertEqual(self.controller.status().active, "a")
        self.assertIsNotNone(self.controller.status().pending)
        self.assertFalse(self.controller.status().slots["a"].eligible)
        # When: a new process view reconciles the persisted intent and real route.
        Controller(self.root, self.ops).reconcile()
        # Then: state adopts verified B and stays held for explicit resumption.
        state = self.controller.status()
        self.assertEqual(state.active, "b")
        self.assertIsNone(state.pending)
        self.assertTrue(state.hold)

    def test_explicit_rejoin_restores_same_version_standby_without_failback(self) -> None:
        # Given: failed A recovered with exactly B's running version.
        self.ops.health["a"] = False
        for _ in range(3):
            self.controller.monitor()
        self.ops.health["a"] = True
        self.ops.transitions.clear()
        # When: the operator explicitly requalifies A after consecutive checks.
        with patch("edge_control.time.sleep"):
            self.controller.rejoin("a")
        # Then: A becomes eligible, while B continues serving traffic.
        state = self.controller.status()
        self.assertTrue(state.slots["a"].eligible)
        self.assertEqual((state.active, state.phase), ("b", "stable"))
        self.assertEqual(self.ops.transitions, [])

    def test_rejoin_rejects_a_healthy_backend_running_a_different_version(self) -> None:
        # Given: the inactive server is healthy but runs an unverified image.
        self.ops.identities["b"] = Identity("release-v2", "sha256:v2", "test/app@sha256:v2")
        state = self.controller.status()
        state.slots["b"].eligible = False
        self.controller.store.write(state)
        # When: the operator requests standby rejoin.
        with self.assertRaises(ControlError):
            self.controller.rejoin("b")
        # Then: a healthy response cannot override the version requirement.
        self.assertFalse(self.controller.status().slots["b"].eligible)
        self.assertEqual(self.ops.transitions, [])


class StateStoreTest(unittest.TestCase):
    def test_lock_excludes_another_process_and_recovers_after_process_death(self) -> None:
        # Given: a separate process owns the actual operating-system lock.
        with tempfile.TemporaryDirectory() as directory:
            root = pathlib.Path(directory)
            child_code = (
                "import pathlib,sys; sys.path.insert(0,sys.argv[1]); "
                "from edge_state import Store; "
                "store=Store(pathlib.Path(sys.argv[2])); "
                "lock=store.lock('routing'); lock.__enter__(); "
                "print('locked',flush=True); sys.stdin.read()"
            )
            with subprocess.Popen(
                [sys.executable, "-c", child_code, str(SCRIPTS), str(root)],
                stdin=subprocess.PIPE, stdout=subprocess.PIPE, text=True,
            ) as child:
                assert child.stdout is not None
                self.assertTrue(select.select([child.stdout], [], [], 5)[0])
                self.assertEqual(child.stdout.readline().strip(), "locked")
                store = Store(root)
                with self.assertRaises(ControlError):
                    with store.lock("routing", blocking=False):
                        self.fail("a second process acquired an occupied routing lock")

                # When: the lock owner dies without running cleanup.
                child.kill()
                child.wait(timeout=5)

            # Then: the kernel releases the lock for the next controller.
            with store.lock("routing", blocking=False):
                store.write(State("a", {
                    "a": Slot("release-v1", "sha256:v1", True, "bundle-v1"),
                    "b": Slot("release-v1", "sha256:v1", True, "bundle-v1"),
                }))
            self.assertEqual(Store(root).read().active, "a")

    def test_durable_state_preserves_pending_switch_and_hold_for_restart(self) -> None:
        # Given: a switch intent was persisted before a process restart.
        with tempfile.TemporaryDirectory() as directory:
            root = pathlib.Path(directory)
            state = State("a", {
                "a": Slot("release-v1", "sha256:v1", True, "bundle-v1"),
                "b": Slot("release-v1", "sha256:v1", True, "bundle-v1"),
            }, hold=True, pending={"from": "a", "to": "b", "phase": "degraded"})
            Store(root).write(state)

            # When: a new store instance reads the same durable state.
            restored = Store(root).read()

            # Then: restart cannot erase the intervention or unfinished intent.
            self.assertTrue(restored.hold)
            self.assertEqual(restored.pending, {"from": "a", "to": "b", "phase": "degraded"})
            self.assertEqual(restored.generation, 1)
            self.assertEqual((root / "edge-state/failover.json").stat().st_mode & 0o777, 0o600)

    def test_invalid_state_cannot_be_used_as_a_route(self) -> None:
        # Given: persisted state names an unknown active backend.
        with tempfile.TemporaryDirectory() as directory:
            store = Store(pathlib.Path(directory))
            store.write(State("a", {
                "a": Slot("release-v1", "sha256:v1", True, "bundle-v1"),
                "b": Slot("release-v1", "sha256:v1", True, "bundle-v1"),
            }))
            data = json.loads(store.path.read_text())
            data["active"] = "outside"
            store.path.write_text(json.dumps(data))

            # When / Then: loading it fails instead of inventing a fallback.
            with self.assertRaises(ControlError):
                store.read()

    def test_malformed_hold_and_pending_state_are_rejected(self) -> None:
        for invalid in ({"hold": "false"}, {"hold": 0},
                        {"pending": {"from": "a", "to": "a", "phase": "degraded"}},
                        {"pending": {"from": "a", "to": "b", "phase": "unknown"}}):
            with self.subTest(invalid=invalid), tempfile.TemporaryDirectory() as directory:
                # Given: persisted control state contains a wrongly typed safety field.
                store = Store(pathlib.Path(directory))
                store.write(State("a", {
                    slot: Slot("release-v1", "sha256:v1", True, "bundle-v1") for slot in ("a", "b")
                }))
                data = json.loads(store.path.read_text())
                data.update(invalid)
                store.path.write_text(json.dumps(data))
                # When / Then: loading fails instead of coercing it into permission.
                with self.assertRaises(ControlError):
                    store.read()


if __name__ == "__main__":
    unittest.main()

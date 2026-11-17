"""배포 중 후보 제외와 안정화 후 동기화의 상태 전이를 검증한다."""
from __future__ import annotations

import json
import pathlib
import tempfile
import threading
from concurrent.futures import ThreadPoolExecutor
import time
import unittest
from typing import Callable, Optional
from unittest.mock import patch

from test_edge_failover import TestOperations
from edge_control import Controller
from edge_io import FILES, Operations, fingerprint
from edge_state import ControlError, Identity


class ReleaseOperations(TestOperations):
    def __init__(self, root: pathlib.Path) -> None:
        super().__init__(root)
        self.releases: list[str] = []
        self.retained = []
        self.fail_cleanup = False
        self.on_release: Optional[Callable[[], None]] = None

    def retain(self, slot, identity, clean=False, protected=()):
        self.retained.append((slot, clean, identity))
        if clean and self.fail_cleanup:
            raise ControlError("cleanup failed")

    def release(self, slot: str, bundle: pathlib.Path) -> Identity:
        self.releases.append(slot)
        callback, self.on_release = self.on_release, None
        if callback is not None:
            callback()
        reference = (bundle / "image-pin").read_text().strip()
        identity = Identity(fingerprint(bundle), reference.split("@", 1)[1], reference)
        self.identities[slot] = identity
        return identity


class ReleaseControllerTest(unittest.TestCase):
    def setUp(self) -> None:
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.root = pathlib.Path(self.temporary.name)
        (self.root / "config").mkdir()
        (self.root / "config/backend.env").write_text("DB_URL=test\n")
        self.old_artifact = self.artifact("old", "1")
        self.ops = ReleaseOperations(self.root)
        self.ops.replace("a")
        reference = (self.old_artifact / "image-pin").read_text().strip()
        identity = Identity(fingerprint(self.old_artifact), "sha256:" + "1" * 64, reference)
        self.ops.identities = {"a": identity, "b": identity}
        self.controller = Controller(self.root, self.ops)
        self.controller.bootstrap(self.old_artifact)
        self.ops.transitions.clear()
        self.next_artifact = self.artifact("new", "2")
        self.manifest(False)

    def artifact(self, name: str, digest: str) -> pathlib.Path:
        artifact = self.root / name
        artifact.mkdir()
        (artifact / "config").mkdir()
        for filename in FILES:
            (artifact / filename).write_text("test\n")
            if filename.startswith("docker-compose"):
                (artifact / "config" / filename).write_text("test\n")
        (artifact / "backend.env").write_text("DB_URL=test\n")
        (artifact / "image.env").write_text("BACKEND_IMAGE=test/app\nBACKEND_IMAGE_TAG=" + name + "\n")
        (artifact / "image-pin").write_text("test/app@sha256:" + digest * 64 + "\n")
        return artifact

    def manifest(self, allow_rollback: bool) -> None:
        current = self.controller.status().slots["a"]
        (self.next_artifact / "compatibility.json").write_text(json.dumps({
            "previous_release": current.release,
            "previous_image": current.reference,
            "next_release": fingerprint(self.next_artifact),
            "next_image": (self.next_artifact / "image-pin").read_text().strip(),
            "coexistence_evidence": "local fixture: old/new worker and schema compatibility",
            "allow_automatic_rollback": allow_rollback,
            "rollback_evidence": "local fixture: old application reads new writes" if allow_rollback else "",
        }))

    def complete_soak(self) -> None:
        state = self.controller.status()
        state.soak_since = time.time() - 601
        state.last_probe = time.time()
        state.successes[state.active] = 3
        self.controller.store.write(state)

    def restore_inputs(self):
        self.controller.deploy(self.next_artifact)
        self.complete_soak()
        self.controller.finalize()
        state = self.controller.status()
        saved = pathlib.Path(state.rollback_bundle)
        approval = self.root / "restore-approval.json"
        approval.write_text(json.dumps({
            "previous_release": state.slots[state.active].release,
            "previous_image": state.slots[state.active].reference,
            "next_release": fingerprint(saved),
            "next_image": (saved / "image-pin").read_text().strip(),
            "coexistence_evidence": "tested restored app on current database",
            "allow_automatic_rollback": False,
        }))
        return saved, approval

    def assert_failover_during_retention(self, before_deploy: bool, clean: bool) -> None:
        if not before_deploy:
            self.controller.deploy(self.next_artifact)
            self.complete_soak()
        active = self.controller.status().active
        paused, resume, monitored = threading.Event(), threading.Event(), threading.Event()
        original = self.ops.retain

        def delayed_retain(slot, identity, clean=False, protected=()):
            if clean == pause_clean and not paused.is_set():
                paused.set()
                if not resume.wait(5):
                    raise ControlError("테스트 정리 작업 대기 시간 초과")
            original(slot, identity, clean, protected)

        def monitor_failure() -> None:
            self.ops.health[active] = False
            for _ in range(3):
                Controller(self.root, self.ops).monitor()
            monitored.set()

        pause_clean = clean
        action = (lambda: self.controller.deploy(self.next_artifact)) if before_deploy else self.controller.finalize
        with patch.object(self.ops, "retain", side_effect=delayed_retain), ThreadPoolExecutor(max_workers=2) as workers:
            work = workers.submit(action)
            try:
                self.assertTrue(paused.wait(3))
                monitor = workers.submit(monitor_failure)
                self.assertTrue(monitored.wait(1), "이미지 작업이 끝나기 전에 장애 전환해야 한다")
                monitor.result(timeout=1)
                self.assertNotEqual(self.ops.routed_slot(), active)
            finally:
                resume.set()
            if before_deploy:
                with self.assertRaises(ControlError):
                    work.result(timeout=3)
            else:
                work.result(timeout=3)
        state = self.controller.status()
        self.assertEqual(state.phase, "degraded")
        self.assertNotEqual(state.active, active)
        self.assertIn("장애로 전환", state.reason)
        self.assertFalse(state.slots[active].eligible)
        if before_deploy:
            self.assertEqual(self.ops.releases, [])

    def test_predeploy_retention_does_not_block_failover(self) -> None:
        self.assert_failover_during_retention(before_deploy=True, clean=False)

    def test_final_retention_does_not_block_failover(self) -> None:
        self.assert_failover_during_retention(before_deploy=False, clean=False)

    def test_cleanup_does_not_block_failover_or_overwrite_its_state(self) -> None:
        self.assert_failover_during_retention(before_deploy=False, clean=True)

    def test_stopped_standby_can_be_redeployed_after_preserving_active_image(self) -> None:
        self.ops.health["b"] = False
        self.controller.monitor()

        def recover_standby() -> None:
            self.assertEqual(self.ops.routed_slot(), "a")
            self.assertFalse(self.controller.status().slots["b"].eligible)
            self.assertEqual([slot for slot, _, _ in self.ops.retained], ["a"])
            self.ops.health["b"] = True

        self.ops.on_release = recover_standby
        self.controller.deploy(self.next_artifact)
        self.assertEqual(self.ops.releases, ["b"])
        self.assertEqual(self.controller.status().active, "b")
        self.assertEqual(self.controller.status().phase, "soaking")

    def test_stopped_standby_stays_excluded_if_redeployment_is_unhealthy(self) -> None:
        self.ops.health["b"] = False
        with patch("edge_release.time.sleep"), self.assertRaises(ControlError):
            self.controller.deploy(self.next_artifact)
        self.assertEqual(self.ops.releases, ["b"])
        self.assertEqual(self.ops.routed_slot(), "a")
        self.assertFalse(self.controller.status().slots["b"].eligible)

    def test_stopped_standby_does_not_bypass_active_image_preservation_failure(self) -> None:
        self.ops.health["b"] = False
        with patch.object(self.ops, "retain", side_effect=ControlError("active image record failed")):
            with self.assertRaises(ControlError):
                self.controller.deploy(self.next_artifact)
        self.assertEqual(self.ops.releases, [])
        self.assertEqual(self.ops.routed_slot(), "a")

    def test_cleanup_only_after_successful_sync_and_failure_does_not_rollback(self):
        self.controller.deploy(self.next_artifact)
        self.assertFalse(any(clean for _, clean, _ in self.ops.retained))
        self.complete_soak()
        self.ops.fail_cleanup = True
        self.controller.finalize()
        state = self.controller.status()
        self.assertEqual((state.active, state.phase), ("b", "stable"))
        self.assertIn("정리 실패", state.reason)
        self.assertEqual(self.ops.transitions, ["b"])

    def test_active_health_failure_during_sync_prevents_cleanup(self):
        self.controller.deploy(self.next_artifact)
        self.complete_soak()
        self.ops.on_release = lambda: self.ops.health.update(b=False)
        with self.assertRaises(ControlError):
            self.controller.finalize()
        self.assertFalse(any(clean for _, clean, _ in self.ops.retained))
        self.assertEqual(self.controller.status().phase, "degraded")

    def test_failed_sync_never_cleans_images(self):
        self.controller.deploy(self.next_artifact)
        self.complete_soak()
        with patch.object(self.ops, "release", side_effect=ControlError("failed")):
            with self.assertRaises(ControlError):
                self.controller.finalize()
        self.assertFalse(any(clean for _, clean, _ in self.ops.retained))
        self.assertEqual(self.controller.status().active, "b")

    def test_restore_uses_saved_environment_and_syncs_after_soak(self):
        from edge_release import deploy
        saved, approval = self.restore_inputs()
        self.controller.hold(True)
        (self.root / "config/backend.env").write_text("DB_URL=changed-current-config\n")
        deploy(self.controller, saved, approval)
        state = self.controller.status()
        self.assertTrue(state.restoring)
        self.assertFalse(state.automatic_rollback)
        self.assertEqual((pathlib.Path(state.desired) / "backend.env").read_text(), "DB_URL=test\n")
        self.assertEqual(state.slots[state.active].release, fingerprint(saved))
        self.complete_soak()
        self.controller.finalize()
        state = self.controller.status()
        self.assertEqual(state.phase, "stable")
        self.assertFalse(state.restoring)
        self.assertEqual(state.slots["a"].reference, state.slots["b"].reference)

    def test_failed_restored_version_holds_without_returning_to_defective_version(self):
        from edge_release import deploy
        saved, approval = self.restore_inputs()
        deploy(self.controller, saved, approval)
        restored = self.controller.status().active
        self.ops.health[restored] = False
        releases = list(self.ops.releases)
        for _ in range(3):
            self.controller.monitor()
        self.controller.finalize()
        self.assertTrue(self.controller.status().hold)
        self.assertEqual(self.controller.status().active, restored)
        self.assertEqual(self.ops.releases, releases)

    def test_restore_rejects_stale_approval_and_automatic_return_to_defective_version(self):
        from edge_release import deploy
        saved, approval = self.restore_inputs()
        original = json.loads(approval.read_text())
        for updates in ({"previous_release": "stale"},
                        {"allow_automatic_rollback": True, "rollback_evidence": "not permitted"}):
            approval.write_text(json.dumps({**original, **updates}))
            before = list(self.ops.releases)
            with self.assertRaises(ControlError):
                deploy(self.controller, saved, approval)
            self.assertEqual(self.ops.releases, before)

    def test_deploy_excludes_target_before_release_and_old_version_after_switch(self) -> None:
        # Given: the new release has coexistence evidence but no rollback approval.
        observed = []
        self.ops.on_release = lambda: observed.append(self.controller.status().slots["b"].eligible)
        # When: B is deployed and selected.
        self.controller.deploy(self.next_artifact)
        # Then: B was excluded before mutation and old A cannot receive automatic fallback.
        state = self.controller.status()
        self.assertEqual(observed, [False])
        self.assertEqual((state.active, state.phase), ("b", "soaking"))
        self.assertFalse(state.slots["a"].eligible)
        self.assertEqual(self.ops.releases, ["b"])

    def test_compatible_old_version_can_take_over_during_soak(self) -> None:
        # Given: this exact release pair has rollback evidence and B is active.
        self.manifest(True)
        self.controller.deploy(self.next_artifact)
        self.ops.health["b"] = False
        # When: B fails repeatedly during stabilization.
        for _ in range(3):
            self.controller.monitor()
        # Then: A is restored without redeploying or syncing the failed new release.
        self.assertEqual(self.controller.status().active, "a")
        self.assertEqual(self.ops.releases, ["b"])
        self.assertEqual(self.controller.status().phase, "degraded")
        self.assertTrue(self.controller.status().hold)
        self.ops.health["b"] = True
        self.controller.monitor()
        self.controller.finalize()
        self.assertEqual(self.ops.releases, ["b"])
        self.assertEqual(self.controller.status().active, "a")

    def test_changed_rollback_approval_prevents_switch(self) -> None:
        self.manifest(True)
        self.controller.deploy(self.next_artifact)
        state = self.controller.status()
        state.compatibility["previous_image"] = "test/app@sha256:" + "0" * 64
        self.controller.store.write(state)
        self.ops.health["b"] = False
        for _ in range(3):
            self.controller.monitor()
        self.assertEqual(self.controller.status().active, "b")

    def test_unapproved_old_version_is_not_used_when_new_version_fails(self) -> None:
        # Given: old-version rollback was not approved for the release pair.
        self.controller.deploy(self.next_artifact)
        self.ops.health["b"] = False
        # When: the new active version fails repeatedly.
        for _ in range(4):
            self.controller.monitor()
        # Then: the healthy old version still cannot bypass compatibility policy.
        self.assertEqual(self.controller.status().active, "b")
        self.assertEqual(self.ops.transitions, ["b"])

    def test_manifest_for_another_release_is_rejected_before_backend_mutation(self) -> None:
        # Given: the manifest was prepared for different next-release contents.
        (self.next_artifact / "image.env").write_text("BACKEND_IMAGE_TAG=unexpected\n")
        # When / Then: compatibility validation fails before any deployment.
        with self.assertRaises(ControlError):
            self.controller.deploy(self.next_artifact)
        self.assertEqual(self.ops.releases, [])
        self.assertEqual(self.controller.status().active, "a")

    def test_same_tag_with_changed_image_pin_cannot_reuse_prior_approval(self) -> None:
        # Given: mutable tag/configuration are unchanged, but the resolved image differs.
        (self.next_artifact / "image-pin").write_text("test/app@sha256:" + "3" * 64 + "\n")
        # When: a manifest approved the previous digest of that same tag.
        with self.assertRaises(ControlError):
            self.controller.deploy(self.next_artifact)
        # Then: matching configuration fingerprints alone cannot authorize deployment.
        self.assertEqual(self.ops.releases, [])
        self.assertEqual(self.ops.transitions, [])

    def test_malformed_optional_evidence_fails_before_backend_mutation(self) -> None:
        for evidence in (None, {}, []):
            with self.subTest(evidence=evidence):
                # Given: rollback is denied but its optional evidence has an invalid type.
                path = self.next_artifact / "compatibility.json"
                manifest = json.loads(path.read_text())
                manifest["rollback_evidence"] = evidence
                path.write_text(json.dumps(manifest))
                # When: compatibility metadata crosses the deployment boundary.
                with self.assertRaises(ControlError):
                    self.controller.deploy(self.next_artifact)
                # Then: no backend changed and the original durable state remains readable.
                self.assertEqual(self.ops.releases, [])
                self.assertEqual(self.controller.status().active, "a")

    def test_failed_edge_verification_cannot_restore_unapproved_old_version(self) -> None:
        # Given: old-version rollback is denied and B fails edge verification after reload.
        with patch.object(self.ops, "verify", side_effect=lambda slot: slot == "a"):
            # When: deployment switches to B but cannot verify the edge response.
            with self.assertRaises(ControlError):
                self.controller.deploy(self.next_artifact)
        # Then: failure handling does not silently bypass the rollback policy.
        self.assertEqual(self.ops.transitions, ["b"])
        state = self.controller.status()
        self.assertTrue(state.hold)
        self.assertEqual(state.phase, "recovery")
        self.assertIsNotNone(state.pending)

    def test_reconcile_keeps_verified_old_route_after_nginx_check_failure(self) -> None:
        # 실제 replace의 nginx -t 실패는 파일을 A로 되돌리고 reload하지 않는다.
        with patch.object(self.ops, "replace", side_effect=lambda slot: Operations.replace(self.ops, slot)), \
                patch("edge_io.command", side_effect=ControlError("nginx -t failed")):
            with self.assertRaises(ControlError):
                self.controller.deploy(self.next_artifact)
        self.assertEqual(self.ops.routed_slot(), "a")
        self.assertFalse(self.controller.status().slots["a"].eligible)
        with patch.object(self.ops, "replace", side_effect=AssertionError("기존 라우팅을 다시 변경하면 안 된다")):
            Controller(self.root, self.ops).reconcile()
        state = self.controller.status()
        self.assertEqual(state.active, "a")
        self.assertIsNone(state.pending)
        self.assertTrue(state.hold)
        self.assertTrue(state.slots["a"].eligible)
        self.assertFalse(state.slots["b"].eligible)

    def test_reconcile_rejects_old_route_file_without_matching_live_response(self) -> None:
        with patch.object(self.ops, "replace", side_effect=ControlError("reload result unknown")):
            with self.assertRaises(ControlError):
                self.controller.deploy(self.next_artifact)
        with patch.object(self.ops, "verify", return_value=False), \
                patch.object(self.ops, "replace", side_effect=AssertionError("승인 없는 재전환 금지")), \
                patch("edge_control.time.sleep"):
            with self.assertRaises(ControlError):
                self.controller.reconcile()
        self.assertIsNotNone(self.controller.status().pending)

    def deploy_same_tag_rebuild(self) -> None:
        (self.next_artifact / "image.env").write_text((self.old_artifact / "image.env").read_text())
        self.manifest(True)
        self.controller.deploy(self.next_artifact)
        state = self.controller.status()
        self.assertEqual(state.slots["a"].release, state.slots["b"].release)
        self.assertNotEqual(state.slots["a"].image, state.slots["b"].image)

    def test_same_tag_rebuild_automatic_rollback_holds_after_switch(self) -> None:
        self.deploy_same_tag_rebuild()
        self.ops.health["b"] = False
        for _ in range(3):
            self.controller.monitor()
        state = self.controller.status()
        self.assertEqual(state.active, "a")
        self.assertTrue(state.hold)
        self.assertIn("자동 롤백 완료", state.reason)

    def test_same_tag_rebuild_cannot_bypass_previous_image_approval(self) -> None:
        self.deploy_same_tag_rebuild()
        state = self.controller.status()
        state.compatibility["previous_image"] = "test/app@sha256:" + "9" * 64
        self.controller.store.write(state)
        self.ops.health["b"] = False
        for _ in range(3):
            self.controller.monitor()
        self.assertEqual(self.controller.status().active, "b")

    def test_same_tag_rebuild_manual_rollback_rejects_synchronized_new_image(self) -> None:
        self.deploy_same_tag_rebuild()
        self.complete_soak()
        self.controller.finalize()
        with self.assertRaisesRegex(ControlError, "보존 bundle"):
            self.controller.rollback()
        self.assertEqual(self.controller.status().active, "b")

    def test_same_tag_rebuild_manual_rollback_accepts_retained_old_image(self) -> None:
        self.deploy_same_tag_rebuild()
        self.controller.rollback()
        self.assertEqual(self.controller.status().active, "a")
        self.assertTrue(self.controller.status().hold)

    def test_operator_hold_during_release_prevents_traffic_switch(self) -> None:
        # Given: the operator holds the system while the remote release runs.
        self.ops.on_release = lambda: self.controller.hold(True)
        # When: deployment attempts to finish using its earlier observation.
        with self.assertRaises(ControlError):
            self.controller.deploy(self.next_artifact)
        # Then: hold survives and the mutated target stays excluded.
        state = self.controller.status()
        self.assertTrue(state.hold)
        self.assertFalse(state.slots["b"].eligible)
        self.assertEqual(state.active, "a")
        self.assertEqual(self.ops.transitions, [])

    def test_finalize_waits_for_stabilization_window(self) -> None:
        # Given: B has just received traffic.
        self.controller.deploy(self.next_artifact)
        # When: finalization runs before the configured 600-second window.
        self.controller.finalize()
        # Then: old A remains intact.
        self.assertEqual(self.ops.releases, ["b"])
        self.assertEqual(self.controller.status().phase, "soaking")

    def test_finalize_preserves_previous_backend_while_connections_are_draining(self) -> None:
        # Given: stabilization elapsed, but old Nginx workers still own requests.
        self.controller.deploy(self.next_artifact)
        self.complete_soak()
        self.ops.is_drained = False
        # When: finalization considers replacing old A.
        self.controller.finalize()
        # Then: A is not stopped/recreated while its requests remain in flight.
        self.assertEqual(self.ops.releases, ["b"])
        self.assertEqual(self.controller.status().phase, "soaking")

    def test_finalize_restores_same_version_standby_after_soak_and_drain(self) -> None:
        # Given: B is stable and old requests finished.
        self.controller.deploy(self.next_artifact)
        self.complete_soak()
        # When: finalization synchronizes the inactive A.
        self.controller.finalize()
        # Then: both verified versions match while traffic stays on B.
        state = self.controller.status()
        self.assertEqual((state.active, state.phase), ("b", "stable"))
        self.assertEqual(self.ops.releases, ["b", "a"])
        self.assertEqual(self.ops.transitions, ["b"])
        self.assertTrue(state.slots["a"].eligible)
        self.assertEqual(state.slots["a"].release, state.slots["b"].release)
        self.assertEqual(state.slots["a"].image, state.slots["b"].image)

    def test_gap_in_monitoring_restarts_stabilization_window(self) -> None:
        # Given: time elapsed but the monitor missed more than 30 seconds.
        self.controller.deploy(self.next_artifact)
        state = self.controller.status()
        state.soak_since, state.last_probe = 1, 1
        self.controller.store.write(state)
        # When: a new health observation arrives after the gap.
        with patch("edge_control.time.time", return_value=1000):
            self.controller.monitor()
        # Then: elapsed wall time alone does not qualify the new version as stable.
        self.assertEqual(self.controller.status().soak_since, 1000)


if __name__ == "__main__":
    unittest.main()

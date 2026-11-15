"""배포 작업 잠금을 유지하고 네트워크 작업 중 routing 잠금은 놓는다."""
import json
from pathlib import Path
import re
import time
import uuid
from typing import TYPE_CHECKING, Dict, Optional

from edge_io import fingerprint
from edge_state import ControlError, Identity, Slot, State, atomic_write, other

if TYPE_CHECKING:
    from edge_control import Controller


def compatibility(bundle: Path, state: State) -> Dict[str, str]:
    try:
        manifest = json.loads((bundle / "compatibility.json").read_text())
        pin = (bundle / "image-pin").read_text().strip()
    except (OSError, ValueError) as error:
        raise ControlError("compatibility.json과 불변 image-pin이 필요합니다") from error
    if not isinstance(manifest, dict):
        raise ControlError("compatibility.json은 JSON object여야 합니다")
    if not re.fullmatch(r"[A-Za-z0-9._:/-]+@sha256:[a-f0-9]{64}", pin):
        raise ControlError("image-pin은 repository@sha256:digest여야 합니다")
    expected = {"previous_release": state.slots[state.active].release,
                "previous_image": state.slots[state.active].reference,
                "next_release": fingerprint(bundle), "next_image": pin}
    if any(manifest.get(key) != value for key, value in expected.items()):
        raise ControlError("호환성 근거의 이전/다음 설정 및 이미지가 실제 릴리스와 다릅니다")
    if not isinstance(manifest.get("coexistence_evidence"), str) or not manifest["coexistence_evidence"].strip():
        raise ControlError("공유 DB에서 구/신 버전 공존 검증 근거가 필요합니다")
    approval = manifest.get("allow_automatic_rollback", False)
    if not isinstance(approval, bool):
        raise ControlError("allow_automatic_rollback은 boolean이어야 합니다")
    rollback_evidence = manifest.get("rollback_evidence", "")
    if not isinstance(rollback_evidence, str):
        raise ControlError("rollback_evidence는 문자열이어야 합니다")
    if approval and not rollback_evidence.strip():
        raise ControlError("구버전 자동 복귀 검증 근거가 필요합니다")
    expected["coexistence_evidence"] = manifest["coexistence_evidence"]
    expected["rollback_evidence"] = rollback_evidence
    expected["allow_automatic_rollback"] = "true" if approval else "false"
    return expected


def wait_ready(controller: "Controller", slot: str) -> None:
    for attempt in range(30):
        if controller.ops.ready(slot):
            return
        if attempt < 29:
            time.sleep(2)
    raise ControlError("배포한 backend가 readiness/health 검사를 통과하지 못했습니다")


def finish_failure(controller: "Controller", operation: str, target: str) -> None:
    """새로운 감시기 전환을 오래된 배포 정리 코드가 덮어쓰지 않는다."""
    with controller.store.lock("routing"):
        state = controller.store.read()
        if state.operation != operation:
            return
        state.slots[target].eligible = False
        state.operation = ""
        if not state.pending:
            state.phase = "degraded"
        if state.restoring:
            state.hold = True
        state.reason = "배포/동기화 실패; 대상 제외 유지, 보존 bundle 확인 필요"
        controller.store.write(state)


def deploy(controller: "Controller", artifact: Path, approval: Optional[Path] = None) -> None:
    with controller.store.lock("deploy", False):
        restoring = approval is not None
        if restoring:
            artifact = artifact.resolve(strict=True)
            if artifact.parent != (controller.root / "edge-state/bundles").resolve():
                raise ControlError("보관된 edge bundle만 복구할 수 있습니다")
        bundle = controller.ops.snapshot(artifact, saved=True) if restoring else controller.ops.snapshot(artifact)
        if restoring:
            atomic_write(bundle / "compatibility.json", approval.read_text())
        if not (bundle / "compatibility.json").exists():
            approval = controller.root / "config/compatibility" / (fingerprint(bundle) + ".json")
            if approval.exists():
                atomic_write(bundle / "compatibility.json", approval.read_text())
        with controller.store.lock("routing"):
            state = controller.store.read()
            if (state.hold and not restoring) or state.pending or state.phase not in ("stable", "degraded"):
                raise ControlError("현재 상태에서는 배포할 수 없습니다")
            if not controller.ops.ready(state.active) or not controller.ops.verify(state.active) or not controller.matches(state, state.active):
                raise ControlError("현재 활성 backend 상태/릴리스를 확인할 수 없습니다")
            manifest = compatibility(bundle, state)
            if restoring and manifest["allow_automatic_rollback"] != "false":
                raise ControlError("복구 작업은 결함 버전으로 자동 복귀할 수 없습니다")
            if not controller.ops.drained():
                raise ControlError("이전 Nginx worker가 아직 연결을 처리 중입니다")
            if state.phase == "stable":
                for slot in ("a", "b"):
                    value = state.slots[slot]
                    if value.eligible and controller.ops.ready(slot) and controller.matches(state, slot):
                        controller.ops.retain(slot, Identity(value.release, value.image, value.reference))
            state.restoring = restoring
            state.hold = False
            target = other(state.active)
            previous = state.active
            operation = uuid.uuid4().hex
            state.phase, state.operation = "deploying", operation
            state.slots[target].eligible = False
            state.previous, state.desired = previous, str(bundle)
            state.rollback_bundle = state.slots[previous].bundle
            state.compatibility = manifest
            state.automatic_rollback = manifest["allow_automatic_rollback"] == "true"
            controller.store.write(state)
        try:
            identity = controller.ops.release(target, bundle)
            if identity.release != manifest["next_release"] or identity.reference != manifest["next_image"]:
                raise ControlError("배포한 릴리스가 승인된 설정/이미지와 다릅니다")
            wait_ready(controller, target)
            with controller.store.lock("routing"):
                state = controller.store.read()
                if state.operation != operation or state.phase != "deploying" or state.active != previous or state.hold:
                    raise ControlError("배포 도중 상태가 변경되어 전환하지 않습니다")
                state.slots[target] = Slot(identity.release, identity.image, True, str(bundle), identity.reference)
                state.slots[previous].eligible = state.automatic_rollback
                state.soak_since = time.time()
                state.last_probe = 0
                controller.switch(state, target, "soaking")
        except (ControlError, OSError, ValueError, KeyError, TypeError):
            finish_failure(controller, operation, target)
            raise


def finalize(controller: "Controller") -> None:
    with controller.store.lock("deploy", False):
        with controller.store.lock("routing"):
            state = controller.store.read()
            if state.phase != "soaking" or state.hold or state.pending:
                return
            now = time.time()
            if (now - state.soak_since < controller.ops.config.soak_seconds or now - state.last_probe > 30 or
                    state.successes[state.active] < controller.ops.config.success_threshold):
                return
            if not controller.ops.ready(state.active) or not controller.ops.verify(state.active) or not controller.matches(state, state.active):
                state.soak_since = now
                controller.store.write(state)
                return
            if not controller.ops.drained():
                return
            active, target = state.active, other(state.active)
            bundle = Path(state.desired)
            if fingerprint(bundle) != state.slots[active].release or (bundle / "image-pin").read_text().strip() != state.slots[active].reference:
                raise ControlError("보관된 릴리스 bundle이 변경되었습니다")
            operation = uuid.uuid4().hex
            state.phase, state.operation = "syncing", operation
            state.slots[target].eligible = False
            controller.store.write(state)
        try:
            identity = controller.ops.release(target, bundle)
            wait_ready(controller, target)
            with controller.store.lock("routing"):
                state = controller.store.read()
                if state.operation != operation or state.phase != "syncing" or state.active != active or state.hold:
                    raise ControlError("동기화 도중 상태가 변경되었습니다")
                if (identity.release, identity.image) != (state.slots[active].release, state.slots[active].image):
                    raise ControlError("양쪽 실제 이미지/설정이 일치하지 않습니다")
                if (not controller.matches(state, active) or not controller.ops.ready(active) or
                        not controller.ops.verify(active)):
                    raise ControlError("동기화 도중 활성 서버의 릴리스/health/응답을 확인할 수 없습니다")
                state.slots[target] = Slot(identity.release, identity.image, True, str(bundle), identity.reference)
                state.slots[active].eligible = True
                state.restoring = False
                state.phase, state.operation, state.reason = "stable", "", "동일 릴리스 대기 복구 완료"
                controller.store.write(state)
                try:
                    for slot in (active, target):
                        value = state.slots[slot]
                        controller.ops.retain(slot, Identity(value.release, value.image, value.reference))
                    for slot in (active, target):
                        if not controller.ops.ready(slot) or not controller.matches(state, slot):
                            raise ControlError("정리 전 backend 재검증 실패")
                        value = state.slots[slot]
                        controller.ops.retain(slot, Identity(value.release, value.image, value.reference), clean=True,
                                              protected=[(Path(state.rollback_bundle) / "image-pin").read_text().strip()])
                except (ControlError, OSError, ValueError):
                    state.reason = "동기화 성공; 이미지 정리 실패, 서비스 유지 및 운영자 확인 필요"
                    controller.store.write(state)
        except (ControlError, OSError, ValueError, KeyError, TypeError):
            finish_failure(controller, operation, target)
            raise

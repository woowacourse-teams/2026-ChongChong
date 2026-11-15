"""배포와 장애 전환을 조율하는 단일 upstream 상태 머신."""
from dataclasses import asdict
import json
from pathlib import Path
import time
import uuid
from typing import Optional

from edge_io import Operations, fingerprint
from edge_state import ControlError, Slot, State, Store, atomic_write, other


class Controller:
    def __init__(self, root: Path, operations: Optional[Operations] = None):
        self.root = root
        self.ops = operations or Operations(root)
        self.store = Store(root)

    def status(self) -> State:
        with self.store.lock("routing"):
            return self.store.read()

    def hold(self, enabled: bool) -> None:
        with self.store.lock("routing"):
            state = self.store.read()
            if not enabled and (state.pending or state.phase == "recovery"):
                raise ControlError("먼저 reconcile로 미완료 전환을 확인해야 합니다")
            state.hold = enabled
            self.store.write(state)

    def bootstrap(self, artifact: Path) -> None:
        with self.store.lock("deploy", False), self.store.lock("routing"):
            if self.store.path.exists() or self.store.path.is_symlink() or (self.store.directory / "pending-upstream.conf").exists():
                raise ControlError("기존 상태 또는 미완료 legacy 전환이 있어 초기화할 수 없습니다")
            bundle = self.ops.snapshot(artifact)
            expected = fingerprint(bundle)
            identities = {slot: self.ops.identity(slot) for slot in ("a", "b")}
            if any(value.release != expected for value in identities.values()) or identities["a"].image != identities["b"].image:
                raise ControlError("양쪽 backend의 실제 이미지와 설정이 일치하지 않습니다")
            if identities["a"].reference != identities["b"].reference or ((bundle / "image-pin").exists() and
                    (bundle / "image-pin").read_text().strip() != identities["a"].reference):
                raise ControlError("양쪽 backend와 준비한 이미지 digest가 일치하지 않습니다")
            if not all(self.ops.ready(slot) for slot in ("a", "b")):
                raise ControlError("양쪽 backend가 정상이어야 초기화할 수 있습니다")
            active = self.ops.routed_slot()
            if not self.ops.verify(active):
                raise ControlError("현재 edge routing을 검증할 수 없습니다")
            atomic_write(bundle / "image-pin", identities["a"].reference + "\n")
            slots = {slot: Slot(value.release, value.image, True, str(bundle), value.reference) for slot, value in identities.items()}
            self.store.write(State(active, slots))

    def matches(self, state: State, slot: str) -> bool:
        try:
            actual = self.ops.identity(slot)
            expected = state.slots[slot]
            return (actual.release, actual.image) == (expected.release, expected.image)
        except (ControlError, ValueError, KeyError, TypeError):
            return False

    def verify_switch(self, slot: str) -> bool:
        """reload 반영을 최대 세 번 확인한다. 요청당 timeout 6초와 대기 2초로 제한한다."""
        for attempt in range(3):
            if self.ops.verify(slot):
                return True
            if attempt < 2:
                time.sleep(1)
        return False

    def switch(self, state: State, target: str, phase: str) -> None:
        """호출자가 routing 잠금을 보유한다. intent를 먼저 저장해 재시작 시 조정한다."""
        previous = state.active
        if not state.slots[target].eligible or not self.ops.ready(target) or not self.matches(state, target):
            raise ControlError("전환 대상이 적격하지 않거나 릴리스가 변경되었습니다")
        state.pending = {"from": previous, "to": target, "phase": phase}
        self.store.write(state)
        try:
            self.ops.replace(target)
            if not self.verify_switch(target):
                raise ControlError("전환 후 edge 응답을 검증할 수 없습니다")
        except ControlError:
            if state.slots[previous].eligible and self.ops.ready(previous) and self.matches(state, previous):
                self.ops.replace(previous)
                if self.verify_switch(previous):
                    state.pending = None
                    state.phase = "degraded"
                    self.store.write(state)
                    raise
            state.phase = "recovery"
            state.hold = True
            state.reason = "전환 실패: 양쪽 routing 상태를 수동 확인해야 합니다"
            self.store.write(state)
            raise
        state.active = target
        state.phase = phase
        state.pending = None
        state.operation = ""
        state.failures = {"a": 0, "b": 0}
        state.successes = {"a": 0, "b": 0}
        self.store.write(state)

    def monitor(self) -> None:
        initial = self.status()
        if initial.hold or initial.pending:
            return
        health = {slot: self.ops.ready(slot) for slot in ("a", "b")}
        with self.store.lock("routing"):
            state = self.store.read()
            if state.generation != initial.generation or state.hold or state.pending:
                return
            if self.ops.routed_slot() != state.active:
                state.hold, state.phase, state.reason = True, "recovery", "상태와 upstream 불일치"
                self.store.write(state)
                return
            now = time.time()
            if now - state.last_probe > 30:
                state.failures = {"a": 0, "b": 0}
                state.successes = {"a": 0, "b": 0}
            if state.phase == "soaking" and (not health[state.active] or now - state.last_probe > 30):
                state.soak_since = now
            state.last_probe = now
            for slot in ("a", "b"):
                state.failures[slot] = 0 if health[slot] else state.failures[slot] + 1
                state.successes[slot] = state.successes[slot] + 1 if health[slot] else 0
            self.store.write(state)
            if state.restoring and state.failures[state.active] >= self.ops.config.failure_threshold:
                state.hold = True
                state.reason = "복구 버전 health 실패; 동기화 중지, 운영자 확인 필요"
                self.store.write(state)
                return
            target = other(state.active)
            if (state.failures[state.active] < self.ops.config.failure_threshold or
                    state.successes[target] < self.ops.config.success_threshold or not state.slots[target].eligible):
                return
            if not self.matches(state, target):
                state.slots[target].eligible = False
                state.reason = "대기 서버 릴리스 변경 또는 조회 실패"
                self.store.write(state)
                return
            failed = state.active
            candidate, current = state.slots[target], state.slots[failed]
            reverting = (candidate.release, candidate.image, candidate.reference) != (
                current.release, current.image, current.reference)
            if reverting and (not state.automatic_rollback or target != state.previous or
                              state.slots[target].release != state.compatibility.get("previous_release") or
                              state.slots[target].reference != state.compatibility.get("previous_image")):
                state.reason = "구버전 호환성 승인 불일치; 자동 복귀 중단"
                self.store.write(state)
                return
            state.slots[failed].eligible = False
            if reverting:
                state.hold = True
            self.switch(state, target, "degraded")
            state.reason = ("구버전 자동 롤백 완료; 운영자 확인 후 resume 필요" if reverting else
                            "활성 backend 장애로 전환; 자동 원복 없음")
            self.store.write(state)

    def reconcile(self) -> None:
        """장기 작업이 끝난 뒤만 실행하며, 불확실하면 자동 재배포하지 않는다."""
        with self.store.lock("deploy", False), self.store.lock("routing"):
            state = self.store.read()
            routed = self.ops.routed_slot()
            allowed = {state.active}
            if state.pending:
                allowed = {slot for slot in (state.pending["from"], state.pending["to"]) if state.slots[slot].eligible}
            keeping_active = (state.pending is not None and routed == state.active == state.pending["from"]
                              and self.verify_switch(routed))
            if (routed not in allowed and not keeping_active) or not self.matches(state, routed) or not self.ops.ready(routed):
                raise ControlError("실제 routing과 릴리스를 확인할 수 없어 수동 복구가 필요합니다")
            # 설정 파일뿐 아니라 실제 edge가 기존 서버를 계속 가리키면 재승격하지 않는다.
            if not keeping_active:
                self.ops.replace(routed)
                if not self.verify_switch(routed):
                    raise ControlError("edge 응답 확인 실패")
            state.slots[routed].eligible = True
            state.active = routed
            state.pending = None
            state.phase = "degraded"
            state.operation = ""
            state.hold = True
            state.reason = "복구 확인 완료; status 확인 후 resume 필요"
            self.store.write(state)

    def deploy(self, artifact: Path) -> None:
        from edge_release import deploy
        deploy(self, artifact)

    def rejoin(self, target: str) -> None:
        """동일 릴리스로 회복한 대기 서버만 명시적으로 복귀시킨다. routing은 바꾸지 않는다."""
        with self.store.lock("deploy", False), self.store.lock("routing"):
            state = self.store.read()
            if target not in ("a", "b") or target == state.active or state.pending or state.hold or state.phase not in ("stable", "degraded"):
                raise ControlError("지금은 해당 서버를 대기로 복귀시킬 수 없습니다")
            identity = self.ops.identity(target)
            active = state.slots[state.active]
            if (identity.release, identity.image, identity.reference) != (active.release, active.image, active.reference) or not self.matches(state, state.active):
                raise ControlError("대기 복귀는 활성 서버와 동일 이미지/설정인 경우만 가능합니다")
            for attempt in range(self.ops.config.success_threshold):
                if not self.ops.ready(target) or not self.ops.ready(state.active):
                    raise ControlError("양쪽 backend 건강 상태 확인 실패")
                if attempt + 1 < self.ops.config.success_threshold:
                    time.sleep(2)
            if not self.ops.verify(state.active):
                raise ControlError("활성 edge 응답 확인 실패")
            state.slots[target] = Slot(identity.release, identity.image, True, active.bundle, identity.reference)
            state.phase, state.reason = "stable", "동일 릴리스 대기 서버 수동 복귀 완료"
            self.store.write(state)

    def finalize(self) -> None:
        from edge_release import finalize
        finalize(self)

    def rollback(self) -> None:
        with self.store.lock("deploy", False), self.store.lock("routing"):
            state = self.store.read()
            target = state.previous
            if state.pending or not target or target == state.active or not state.automatic_rollback:
                raise ControlError("승인된 이전 릴리스로 되돌릴 수 없습니다")
            if (state.slots[target].release != state.compatibility.get("previous_release") or
                    state.slots[target].reference != state.compatibility.get("previous_image")):
                raise ControlError("이전 서버가 이미 동기화되었습니다. 보존 bundle로 별도 재배포해야 합니다")
            state.hold = True
            self.switch(state, target, "degraded")
            state.reason = "수동 롤백 완료; 확인 후 resume 필요"
            self.store.write(state)

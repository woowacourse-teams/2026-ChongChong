"""영속 배포 상태와 프로세스 간 잠금. 외부 패키지 없이 Python 3.9 이상에서 실행한다."""
from contextlib import contextmanager
from dataclasses import asdict, dataclass, field
import fcntl
import json
import os
import math
from pathlib import Path
import tempfile
from typing import Dict, Iterator, Optional


class ControlError(RuntimeError):
    """운영자에게 원인을 전달하고 안전하게 작업을 중단한다."""


@dataclass(frozen=True)  # noqa: SLOTS_OK — 기존 배포 환경의 Python 3.9 호환성 유지.
class Identity:
    release: str
    image: str
    reference: str = ""


@dataclass  # noqa: MUTABLE_OK # noqa: SLOTS_OK — 잠금 안에서 상태 변경; Python 3.9 호환.
class Slot:
    """상태 전환 안에서만 변경하는 서버별 검증 결과."""
    release: str
    image: str
    eligible: bool
    bundle: str
    reference: str = ""


@dataclass  # noqa: MUTABLE_OK # noqa: SLOTS_OK — 잠금 안에서 상태 변경; Python 3.9 호환.
class State:
    """routing.lock 아래에서 읽고 변경하고 원자적으로 저장한다."""
    active: str
    slots: Dict[str, Slot]
    version: int = 1
    generation: int = 0
    phase: str = "stable"
    pending: Optional[Dict[str, str]] = None
    previous: str = ""
    desired: str = ""
    soak_since: float = 0
    hold: bool = False
    failures: Dict[str, int] = field(default_factory=lambda: {"a": 0, "b": 0})
    successes: Dict[str, int] = field(default_factory=lambda: {"a": 0, "b": 0})
    compatibility: Dict[str, str] = field(default_factory=dict)
    automatic_rollback: bool = False
    reason: str = ""
    operation: str = ""
    last_probe: float = 0
    rollback_bundle: str = ""
    restoring: bool = False


def atomic_write(path: Path, content: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True, mode=0o700)
    descriptor, name = tempfile.mkstemp(prefix=".next-", dir=str(path.parent))
    try:
        with os.fdopen(descriptor, "w") as output:
            output.write(content)
            output.flush()
            os.fsync(output.fileno())
        os.replace(name, path)
        with directory_fd(path.parent) as directory:
            os.fsync(directory)
    finally:
        if os.path.exists(name):
            os.unlink(name)


@contextmanager
def directory_fd(path: Path) -> Iterator[int]:
    descriptor = os.open(str(path), os.O_RDONLY)
    try:
        yield descriptor
    finally:
        os.close(descriptor)


class Store:
    def __init__(self, root: Path):
        self.directory = root / "edge-state"
        self.directory.mkdir(parents=True, exist_ok=True, mode=0o700)
        self.path = self.directory / "failover.json"

    @contextmanager
    def lock(self, name: str, blocking: bool = True) -> Iterator[None]:
        with (self.directory / (name + ".lock")).open("a") as handle:
            try:
                fcntl.flock(handle, fcntl.LOCK_EX | (0 if blocking else fcntl.LOCK_NB))
            except BlockingIOError as error:
                raise ControlError("다른 작업이 실행 중입니다: " + name) from error
            try:
                yield
            finally:
                fcntl.flock(handle, fcntl.LOCK_UN)

    def read(self) -> State:
        try:
            data = json.loads(self.path.read_text())
            data["slots"] = {key: Slot(**value) for key, value in data["slots"].items()}
            state = State(**data)
        except (OSError, ValueError, TypeError, KeyError, AttributeError) as error:
            raise ControlError("failover 상태를 읽을 수 없습니다. bootstrap 또는 수동 복구가 필요합니다") from error
        if (type(state.version) is not int or state.version != 1 or state.active not in ("a", "b") or
                set(state.slots) != {"a", "b"} or state.phase not in
                ("stable", "deploying", "soaking", "syncing", "degraded", "recovery")):
            raise ControlError("지원하지 않는 failover 상태입니다")
        if any(type(value) is not bool for value in (state.hold, state.automatic_rollback, state.restoring)):
            raise ControlError("상태 승인 값은 boolean이어야 합니다")
        if type(state.generation) is not int or state.generation < 0:
            raise ControlError("잘못된 상태 세대입니다")
        for slot in state.slots.values():
            if type(slot.eligible) is not bool or not all(isinstance(value, str) and value for value in
                                                        (slot.release, slot.image, slot.bundle)) or not isinstance(slot.reference, str):
                raise ControlError("잘못된 backend 상태입니다")
        for counters in (state.failures, state.successes):
            if not isinstance(counters, dict) or set(counters) != {"a", "b"} or any(type(value) is not int or value < 0 for value in counters.values()):
                raise ControlError("잘못된 probe 카운터입니다")
        if any(type(value) not in (int, float) or not math.isfinite(value) or value < 0 for value in (state.soak_since, state.last_probe)):
            raise ControlError("잘못된 관찰 시간입니다")
        if state.previous not in ("", "a", "b") or not all(isinstance(value, str) for value in (state.desired, state.operation, state.reason, state.rollback_bundle)):
            raise ControlError("잘못된 작업 상태입니다")
        if not isinstance(state.compatibility, dict) or any(not isinstance(key, str) or not isinstance(value, str) for key, value in state.compatibility.items()):
            raise ControlError("잘못된 호환성 상태입니다")
        if state.pending is not None and (not isinstance(state.pending, dict) or set(state.pending) != {"from", "to", "phase"} or
                state.pending["from"] not in ("a", "b") or state.pending["to"] not in ("a", "b") or
                state.pending["from"] == state.pending["to"] or state.pending["phase"] not in ("soaking", "degraded")):
            raise ControlError("잘못된 미완료 전환 상태입니다")
        return state

    def write(self, state: State) -> None:
        state.generation += 1
        atomic_write(self.path, json.dumps(asdict(state), sort_keys=True) + "\n")


def other(slot: str) -> str:
    return "b" if slot == "a" else "a"

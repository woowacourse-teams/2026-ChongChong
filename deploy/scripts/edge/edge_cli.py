# /// script
# requires-python = ">=3.9"
# dependencies = []
# ///
# 실행: bash deploy/scripts/edge/edge-control.sh status
"""외부 패키지 없는 운영 CLI. 상세 오류에 비밀값을 포함하지 않는다."""
from dataclasses import asdict
import json
import os
from pathlib import Path
import sys

from edge_control import Controller
from edge_io import Operations, fingerprint
from edge_state import ControlError


def main() -> int:
    os.umask(0o077)
    args = sys.argv[1:]
    if not args:
        raise ControlError("명령: bootstrap/status/hold/resume/monitor/finalize/deploy/rollback/restore/reconcile/rejoin/fingerprint")
    action = args[0]
    needs_artifact = action in ("bootstrap", "deploy", "fingerprint")
    if len(args) != (3 if action == "restore" else 2 if needs_artifact or action == "rejoin" else 1):
        raise ControlError("명령 인자 수가 잘못되었습니다")
    root = Path(os.environ.get("CHONGCHONG_APP_ROOT", "/opt/chongchong"))
    controller = Controller(root)
    if action == "fingerprint":
        print(fingerprint(controller.ops.snapshot(Path(args[1]).resolve())))
    elif action == "bootstrap":
        controller.bootstrap(Path(args[1]).resolve())
    elif action == "deploy":
        controller.deploy(Path(args[1]).resolve())
    elif action == "restore":
        from edge_release import deploy
        deploy(controller, Path(args[1]).resolve(), Path(args[2]).resolve())
    elif action == "status":
        print(json.dumps(asdict(controller.status()), ensure_ascii=False, indent=2))
    elif action in ("hold", "resume"):
        controller.hold(action == "hold")
    elif action == "monitor":
        controller.monitor()
    elif action == "finalize":
        controller.finalize()
    elif action == "rollback":
        controller.rollback()
    elif action == "reconcile":
        controller.reconcile()
    elif action == "rejoin":
        controller.rejoin(args[1])
    else:
        raise ControlError("알 수 없는 명령입니다")
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except (ControlError, OSError, ValueError, KeyError, TypeError) as error:
        # JSON/파일/외부 프로세스 원문에는 설정 내용이 포함될 수 있다.
        print(str(error) if isinstance(error, ControlError) else "상태/설정 처리 실패; 파일과 운영 상태를 확인하세요", file=sys.stderr)
        sys.exit(1)

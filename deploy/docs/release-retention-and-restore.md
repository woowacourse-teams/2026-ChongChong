# 릴리스 복구와 이미지 보존

관련 이슈: #645. 이 문서는 2026-11-06 로컬 구현 기준이다. 서버 설치·타이머 활성화·GitHub Secrets 등록·원격 삭제는 수행하지 않았다.

## 적용 범위

로컬 자동 정리와 이전 릴리스 복구는 `edge-state/failover.json`으로 초기화한 controller 모드에 적용한다.
기존 shell A/B 모드는 10분 안정화·대기 동기화가 없으므로 해당 모드에 자동 삭제를 추가하지 않는다.
적용 전 [controller 설치와 초기화](edge-failover.md)를 따라 실행 버전과 양쪽 실제 릴리스를 확인한다.
`edge-install.sh`는 새 `backend_retention.py`도 runtime에 포함한다.

## 합의한 정책과 구현

| 항목 | 동작 |
| --- | --- |
| 정상 배포 | 비활성 슬롯 배포·health 검증 → 트래픽 전환 → 10분 안정화 및 기존 연결 종료 → 동일 릴리스 대기 동기화 |
| DB | 성공 적용된 Flyway 변경 유지. 복구할 앱은 현재 DB와 호환되어야 함 |
| 로컬 | 현재 + 이전 성공 릴리스 2개를 보호하고 A/B 동기화·검증 성공 뒤 정리 |
| 원격 | 업로드 기준 최근 90일과 최소 최근 10개 이미지의 합집합 보존 |
| 원격 실행 | Actions가 하루 한 번 후보만 조회. 배포가 없는 때 운영자가 재확인 후 수동 삭제 |

## 로컬 이미지 정리

`edge_release.finalize`가 양쪽 동기화를 완료한 뒤 성공 기록과 정리를 요청한다.
edge의 deploy/routing 잠금을 유지하고 각 backend에서는 `releases/deploy.lock`을 사용한다.
직접 서버에서 실행하는 backend 배포도 같은 잠금을 사용하므로 이미지 교체와 정리가 겹치지 않는다.

`releases/successful-images.json`은 각 서버의 root 전용 최소 로컬 기록이다. S3나 별도 이력 서비스는 사용하지 않는다.
정상 stable 상태에서 다음 배포를 시작하기 전 기존 양쪽 릴리스를 기록하고, 동기화 성공 후 새 릴리스를 기록한다.
후보 배포·단순 pull은 성공 이력에 추가하지 않는다. 같은 설정 fingerprint와 digest는 하나의 릴리스로 계산하며
과거 릴리스로 복구하면 최근 성공 순서로 이동한다. 같은 이미지라도 설정이 다르면 별도 릴리스다.
처음 도입할 때 이미 사라진 과거 성공 이력을 추정하거나 복원하지 않는다.

정리 전 실제 current release의 설정 fingerprint·실행 이미지 ID·RepoDigest·컨테이너 실행 상태를 검증한다.
현재와 이전 성공 2개 외에도 모든 실행/정지 컨테이너 참조 이미지와 명시적 rollback bundle 이미지를 보호한다.
대상 repository의 이미지 중 보호되지 않은 것만 명시적으로 삭제하며 `--force`, 전체 `prune`, 볼륨 삭제는 사용하지 않는다.
다른 repository 태그를 공유하는 이미지와 소유 repository가 불분명한 dangling 이미지는 남긴다.
이미지 기록은 원자적으로 저장하고 삭제 실패는 동기화 성공을 취소하지 않는다. `status.reason`에 정리 실패가 남는다.
다음 성공 동기화에서 다시 정리하며 별도 무한 재시도 타이머는 추가하지 않는다.

**설정·Compose·edge bundle은 자동 삭제하지 않는다.** 이 작업은 이미지 정리이며,
복구 자료까지 나이만으로 지우지 않는다. 비밀 설정은 root 전용으로 보관하고 GitHub 로그·artifact에 올리지 않는다.
디렉터리 보존 기간과 실패 알림 외부 채널은 아직 별도 결정 대상이다.

## 동기화 이후 이전 릴리스 복구

일반 `deploy`는 현재 `config/backend.env`를 읽는다. 과거 설정 복구에는 반드시 새 `restore` 명령을 사용한다.

```bash
sudo bash /opt/chongchong/edge-runtime/current/edge-control.sh status
sudo bash /opt/chongchong/edge-runtime/current/edge-control.sh restore \
  /opt/chongchong/edge-state/bundles/release-OLD \
  /opt/chongchong/config/restore-approval.json
```

운영자는 실제 보존 bundle과 검토한 승인 파일을 지정한다. `release-OLD`는 예시이며 실제 경로로 바꾼다.
승인 파일은 **현재 활성 릴리스 → 복구할 이전 릴리스** 조합으로 새로 작성한다.
기존 bundle 내부의 과거 호환성 승인을 재사용하지 않는다.

```json
{
  "previous_release": "현재 활성 설정 fingerprint",
  "previous_image": "현재 활성 repository@sha256:digest",
  "next_release": "복구 bundle 설정 fingerprint",
  "next_image": "복구 bundle image-pin",
  "coexistence_evidence": "현재 DB에서 복구 대상 앱의 읽기·쓰기 및 기동 검증 근거",
  "allow_automatic_rollback": false
}
```

복구 bundle의 fingerprint는 일반 `fingerprint` 명령으로 계산하지 않는다(그 명령은 현재 env를 사용한다).
이전 성공 상태에서 기록한 fingerprint를 사용하거나 아래처럼 보관 파일 자체를 계산한다. 환경설정 내용은 출력하지 않는다.

```bash
sudo python3 - /opt/chongchong/edge-state/bundles/release-OLD <<'PY'
import sys
from pathlib import Path
sys.path.insert(0, '/opt/chongchong/edge-runtime/current')
from edge_io import fingerprint
print(fingerprint(Path(sys.argv[1])))
PY
```

복구는 stable/degraded이며 미완료 전환이 없고 현재 활성 서버의 실제 릴리스와 health·응답이 확인될 때 시작한다.
운영자가 명시한 restore는 hold 상태에서도 시작할 수 있다. 실제 시작 전 검증 실패는 기존 hold를 유지한다.
활성 서버까지 불명확하거나 다운된 재난 복구는 이 명령이 자동 해결하지 않으며 별도 운영자 조치가 필요하다.

보관된 `backend.env`, `image.env`, Compose, `image-pin`을 새 root 전용 bundle로 복사하고 승인과 대조한다.
원본은 변경하지 않는다. 비활성 서버에 먼저 배포하고 검증한 뒤 전환한다. 10분 안정화 후 기존 대기도 복구 버전으로 동기화한다.
복구 중에는 `restoring` 상태를 영속 저장한다. 새 배포와 정리는 배포 잠금과 상태 조건에 따라 제한된다.
복구 버전이 연속 3회 health 실패하면 hold로 중단한다. 이미 결함을 확인한 직전 버전으로 자동 복귀하지 않는다.
후보 준비 실패는 기존 활성 유지, 전환 실패·불확실한 상태는 수동 확인 대상으로 남긴다.
외부 알림 채널이 정해지지 않았으므로 현재 결과는 CLI 종료 코드, `status.reason`, systemd journal에서 확인한다.

## 원격 후보 조회 (삭제 없음)

`.github/workflows/remote-image-candidates.yml`은 매일 03:23 KST(18:23 UTC)와 수동 요청으로 실행한다.
예약은 기본 브랜치에 반영된 뒤에만 작동하고 실행 시각은 지연될 수 있다. 현재 파일 작성만 했으며 실행하지 않았다.

저장소 설정:

- Variables `DOCKERHUB_REPOSITORY`: `namespace/repository` (registry 접두사 없음)
- Variables `DOCKERHUB_PROTECTED_TAGS` (선택): 추가로 보호할 태그. 예: `dev-current,dev-rollback`. 현재 빌드는 커밋 SHA 태그만 만들므로 비워 두면 최근 10개·90일 규칙만 적용한다
- Secrets `DOCKERHUB_READ_USER`, `DOCKERHUB_READ_TOKEN`: 대상 repository를 조회할 수 있는 최소 읽기 권한 계정/OAT 식별자와 토큰

환경별 repository를 따로 쓰면 해당 repository의 보호 태그만 지정하고 조회 작업도 repository별로 구성한다.
현재 workflow는 설정한 repository 하나를 조회한다. 보호 태그를 지정했는데 하나라도 없으면 전체 후보 조회를 실패시킨다.
보호 태그 생성·갱신은 이 조회 작업이 수행하지 않는다. **운영자가 최신 current/rollback 태그를 확인한 후 사용한다.**

후보는 이 빌드의 40자리 소문자 커밋 SHA 태그만 대상으로 한다. 다른 이름의 수동 태그는 보호한다.
최소 10개는 repository 내 서로 다른 이미지 digest 기준이다. 같은 digest의 별칭은 개수를 늘리지 않는다.
90일 기준은 Docker Hub의 `tag_last_pushed`이며 재업로드하면 기간이 연장된다. 성공 배포 여부는 구분하지 않는다.
보호 태그·최근 10개·90일 이내 태그와 manifest 또는 하위 플랫폼 digest를 공유하면 함께 보존한다.
날짜/digest 누락, 중복 태그, pagination 실패·다른 호스트로 이동이 있으면 후보를 출력하지 않는다.

```bash
# API 응답의 태그 배열을 준비한 오프라인 검증. 네트워크·삭제 없음.
python3 deploy/scripts/edge/remote_image_candidates.py \
  --repository team/backend --protected-tags prod-current,prod-rollback \
  --input tags.json
```

Actions에는 삭제 권한이나 DELETE 호출이 없다. 실행 로그와 Summary에서 후보를 확인한다.
수동 삭제 직전 배포·안정화·동기화·복구가 진행 중이지 않은지, 보호 태그가 실제 A/B를 나타내는지 다시 확인한다.
삭제 중 새 배포를 시작하지 않는다. 과거 후보 목록을 그대로 삭제 목록으로 사용하지 않는다.

## 검증 범위

로컬 테스트는 실제 파일·상태·잠금과 배포 상태 전이를 사용하며 SSH/Docker 경계는 대역으로 격리한다.
Docker Hub 페이지 처리·기간/개수/공유 digest 보호는 고정 응답으로 검증한다. 실제 Docker Hub 인증·권한,
EC2의 Docker 이미지 삭제, 실제 구버전 앱과 현재 DB 호환성, Discord 등의 알림 전달은 별도 적용 검증 대상이다.

공식 API 근거: [태그 조회](https://docs.docker.com/reference/api/hub/latest/operations/ListRepositoryTags/),
[인증 토큰 교환](https://docs.docker.com/reference/api/hub/latest/operations/AuthCreateAccessToken/).

### 2026-11-06 로컬 실행 결과

- `python3 -m unittest discover -s deploy/tests -v`: 91개 통과, 45.371초.
- 전체 실행 후 추가한 실제 CLI의 잘못된 복구 경로 회귀 검증을 포함하여 `test_edge_cli.py` 3개 통과.
- 오프라인 태그 fixture를 실제 후보 조회 CLI에 전달하여 조회 전용 JSON과 보호 태그 제외 확인.
- Python 문법 검사, `edge-install.sh` Bash 문법 검사, Ruby YAML 파서로 workflow 문법·읽기 권한 확인, `git diff --check` 통과.
- 실제 서버 배포·이미지 삭제·Docker Hub API 호출·GitHub Actions 실행은 하지 않았다.

### 최신 디렉터리 구조 이식 검증 — 2026-11-15

[분리 작업본의 로컬 검증](../evidence/local-release-2026-11-15/README.md)을 참고한다. 서버 적용은 수행하지 않았다.

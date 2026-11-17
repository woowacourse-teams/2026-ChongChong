# 백엔드 장애 자동 전환

관련 작업: [#635](https://github.com/woowacourse-teams/2026-ChongChong/issues/635).
기존 A/B 배포 흐름은 [edge-deployment.md](edge-deployment.md), DB 확인 절차는
[database-release-compatibility.md](database-release-compatibility.md)를 참고한다.

이 기능은 단일 edge의 Nginx upstream을 정상 대기 백엔드로 교체한다. edge 자체,
공유 DB, 네트워크 전체 장애를 해결하지 않는다. 실제 서버 설치와 장애 실험은 로컬
검증과 별도 작업이며 이 문서 작성만으로 서버 설정이 변경되지 않는다.

로컬 이미지 보존·정리, 이전 릴리스 `restore`, 원격 후보 조회는
[릴리스 복구와 이미지 보존](release-retention-and-restore.md)을 따른다.

## 운영 정책

- 평상시에는 A/B의 이미지와 설정을 동일 릴리스로 유지한다.
- 활성 백엔드 장애를 연속 검사로 확인하고 검증된 정상 대기에만 전환한다.
- readiness와 일반 health를 함께 확인한다. 양쪽이 실패하면 전환하지 않는다.
- 장애 서버가 복구돼도 원래 서버로 자동 복귀하지 않는다.
- 승인된 구버전으로 자동 롤백한 경우 hold 상태로 남겨 자동 감시·동기화를 중지한다.
  운영자가 상태와 원인을 확인한 뒤 명시적으로 resume한다. 동일 릴리스 장애 전환은 이 hold 규칙의 대상이 아니다.
- 배포 대상은 컨테이너를 바꾸기 전에 전환 자격을 제거한다.
- 새 버전 전환 후 600초 동안 상태를 관찰한다. 건강 검사 실패나 관찰 공백이 있으면
  경과 시간만으로 안정화 완료라고 판단하지 않는다.
- 안정화와 이전 Nginx 연결 종료를 확인한 뒤 대기를 동일 릴리스로 배포한다.
- 대기 배포가 실패하면 활성 서버를 유지하고 대기는 제외 상태로 남긴다.
- 구버전 자동 전환은 정확한 구·신 릴리스 쌍의 호환성 근거가 있을 때만 허용한다.
- 수동 중지 상태는 프로세스 재시작 뒤에도 유지한다.

한 슬롯을 배포하는 동안에는 정상 대기가 없을 수 있다. 감시 주기와 timeout, 연속
실패 횟수, 라우팅 검증 시간이 복구 시간에 영향을 준다. 설정값만으로 복구 시간을
보장하지 않고 로컬 및 별도 승인된 dev 실험에서 측정한다.

## 배포와 장애 전환의 상태

| 상태 | 수행하는 일 | 주의할 점 |
| --- | --- | --- |
| 정상 | 같은 릴리스의 A/B를 검사 | 동일 태그만으로 동일 이미지를 추정하지 않는다. |
| 후보 배포 | 비활성 슬롯 제외, 설정 전송, 이미지 배포와 검증 | 활성 서버는 유지한다. |
| 전환 | 전환 의도 저장, 설정 검사·reload, 실제 응답 확인 | 명령 종료 코드만으로 완료 처리하지 않는다. |
| 안정화 | 이전 버전을 유지하고 새 활성 상태 관찰 | 구버전 자동 전환에는 별도 호환성 근거가 필요하다. |
| 대기 동기화 | 기존 연결 종료 확인 후 대기를 동일 릴리스로 변경 | 이전 버전의 즉시 라우팅 롤백 기간이 끝난다. |
| 복구 필요 | 저장 상태·실제 라우팅·릴리스를 대조 | 모호한 상태에서 새 배포나 임의 승격을 하지 않는다. |

배포 작업은 장기 배포 잠금을 사용하고 라우팅·상태 변경은 별도 짧은 잠금으로
직렬화한다. 잠금 밖에서 받은 검사 결과는 상태 세대가 바뀌었으면 폐기한다.
상시 감시 프로그램은 CodeDeploy가 덮어쓰는 artifact 경로와 분리하여 설치한다.

## 롤백과 요청 처리의 한계

대기까지 새 릴리스로 바꾼 뒤에는 라우팅만 이전 슬롯으로 돌려 구버전을 복원할 수
없다. 보관된 이전 이미지·설정·Compose로 별도 배포하고 현재 DB와의 호환성을 다시
확인해야 한다. DB를 자동으로 되돌리거나 이미 적용한 migration을 수정하지 않는다.

Nginx는 단일 upstream을 유지한다. 진행 중인 POST·파일 첨부·제출 요청을 다른
백엔드로 재전송하는 `non_idempotent` 옵션은 추가하지 않는다. 이미 DB에 반영한
요청의 응답이 유실되면 클라이언트는 결과를 모를 수 있다. 이 기능이 무손실이나
exactly-once를 제공한다고 표현하지 않는다.

HTTP 대기 상태에서도 앱 스케줄러와 알림 워커는 실행될 수 있다. 작업 claim과
장애 작업 회수, 구·신 워커의 데이터 호환성은 HTTP 전환과 별도로 확인한다.

## 설치와 초기화

다음은 향후 서버 적용 시 사용할 절차다. 로컬 구현 단계에서 실행하지 않는다.
Python 3.9 이상과 기존 Bash·SSH·curl·Docker·flock, systemd가 필요하다.

1. 기존 배포 방식으로 A/B의 이미지와 설정을 같은 릴리스로 맞추고 연결 종료를 확인한다.
2. edge의 `config/backend.env`와 배포 artifact가 실제 A/B의 current release와 일치하는지
   확인한다. 태그 이름만 같아서는 충분하지 않다.
3. `sudo bash /opt/chongchong/deploy/scripts/edge/edge-install.sh`로 실행 파일과 systemd unit을
   설치한다. 설치는 timer를 자동 활성화하지 않는다. 코드가 검증된 runtime 디렉터리로
   복사된 뒤 `edge-runtime/current`가 원자적으로 바뀐다.
4. `sudo bash /opt/chongchong/edge-runtime/current/edge-control.sh bootstrap /opt/chongchong/deploy`
   로 실제 A/B의 릴리스·이미지·health와 edge 응답을 확인하고 최초 상태를 만든다.
   다르거나 미완료 legacy 전환이 있으면 초기화가 거절된다.
5. `status`로 활성 슬롯과 양쪽 릴리스를 확인한 뒤 별도 승인된 적용 작업에서만
   `sudo systemctl enable --now chongchong-edge-monitor.timer chongchong-edge-finalize.timer`를 실행한다.

`edge-state/failover.json`이 없으면 기존 배포·롤백 경로를 유지한다. 이 파일이 있으면
기존 진입점이 설치된 controller로 위임한다. runtime이 누락되거나 상태가 손상되면
legacy 모드로 우회하지 않고 중단한다. 활성화 이후 상태 파일을 지워 비활성화하지 않는다.

기본 설정은 `EDGE_FAILURE_THRESHOLD=3`, `EDGE_SUCCESS_THRESHOLD=3`,
`EDGE_SOAK_SECONDS=600`이다. monitor timer는 이전 실행 종료 후 5초를 기다린다.
health 요청·SSH 검사·reload 시간은 별도로 소요되므로 정확히 5초마다 검사하는 것은 아니다.
finalize timer는 이전 실행 종료 후 15초를 기다리며 배포 hook과 별도로 동기화한다.

## 릴리스 승인 자료 준비

빌드가 push한 이미지의 불변 참조를 `deploy/image-pin`에 기록한다. 이 파일의 형식은
`repository@sha256:<digest>`다. controller는 후보 컨테이너를 시작하기 전에 설정과
이미지가 호환성 기록의 정확한 구·신 릴리스와 일치하는지 확인한다.

현재 활성 릴리스 정보는 `status`로 확인한다. 신규 설정 fingerprint는 다음 명령으로
확인한다. 이 명령은 `config/backend.env`와 artifact의 `image.env`, Compose 두 파일을
스냅샷으로 보관하고 내용의 SHA-256을 출력한다. 비밀값 원문은 출력하지 않는다.

```bash
sudo bash /opt/chongchong/edge-runtime/current/edge-control.sh fingerprint /path/to/reviewed-artifact
```

아래 형태의 `compatibility.json`을 검토한 artifact에 포함하거나,
edge의 root 전용 `config/compatibility/<next_release>.json`에 준비한다.
`image-pin`의 이미지 digest와 설정 fingerprint는 서로 다른 값이다.

```json
{
  "previous_release": "현재 활성 설정 fingerprint",
  "previous_image": "현재 활성 repository@sha256:digest",
  "next_release": "검토한 신규 설정 fingerprint",
  "next_image": "신규 image-pin의 값",
  "coexistence_evidence": "이 릴리스 쌍의 DB/API/워커 공존 검증 결과 경로 또는 링크",
  "allow_automatic_rollback": false,
  "rollback_evidence": ""
}
```

구버전 복귀까지 검증했다면 `allow_automatic_rollback`을 `true`로 바꾸고
`rollback_evidence`에 그 결과를 연결한다. 문자열 존재 검사는 검증 결과의 진실성을
증명하지 않는다. 운영자가 실제 결과를 확인해야 하며 확인하지 않은 근거를 기입하지 않는다.
환경설정·이미지·활성 릴리스가 바뀌면 기존 기록을 재사용할 수 없다.

CodeBuild가 이 호환성 근거를 자동으로 만들어 주지는 않는다. opt-in 활성화 전,
빌드 artifact 검토와 승인 자료 전달 단계를 운영 흐름에 준비해야 한다. 승인 자료가
없으면 배포가 안전하게 거절된다. 상태 저장 디렉터리와 보관 bundle은 비밀값을 포함하므로
root 전용으로 유지하고 Git·로그·GitHub 이슈에 첨부하지 않는다.

설정 식별은 `current-backend-release`의 파일을 기준으로 하고 실행 중인 컨테이너의
이미지 ID·digest를 함께 확인한다. 운영자가 이 절차 밖에서 컨테이너 환경변수나
마운트를 수동 변경한 경우까지 현재 파일과 실제 실행 설정이 같다고 증명하지는 않는다.
수동 변경이 있었다면 자동 전환을 중지하고 승인된 릴리스로 다시 맞춘 뒤 검증한다.

## 운영 명령과 복구

설치한 `edge-control.sh`의 하위 명령은 다음과 같다.

| 명령 | 의미 |
| --- | --- |
| `status` | 활성 슬롯, 단계, 대기 적격성, 진행 중 전환과 이유 확인 |
| `hold` / `resume` | 자동 동작 중지 / 재개. 미완료 전환은 재개 전에 복구 필요 |
| `monitor` | 건강 검사와 필요 시 장애 전환을 한 번 실행 |
| `deploy ARTIFACT` | 승인 자료를 확인하고 비활성 슬롯 배포·전환 |
| `finalize` | 안정화·drain 조건 충족 시 대기 동기화 |
| `restore BUNDLE APPROVAL` | 보존 설정과 현재 DB 호환성 자료로 이전 릴리스를 비활성 슬롯에 복구 |
| `rollback` | 승인된 이전 릴리스가 남아 있을 때 라우팅 복귀 후 hold |
| `reconcile` | 저장 상태와 실제 라우팅·릴리스를 확인하고 hold 상태로 복구 |
| `rejoin a` / `rejoin b` | 활성과 같은 릴리스로 복구된 비활성 서버를 재검증해 대기 자격만 복원 |

`hold`는 이미 시작한 원격 컨테이너 작업을 되감지 않는다. 후속 전환·적격성 복귀를
중단시키고 상태 확인을 요구한다. 프로세스가 중단되어 `deploying`·`syncing` 또는 pending
상태가 남으면 작업 종료와 백엔드 상태를 확인하고 `reconcile`, `status`, `resume` 순서로
판단한다. 자동으로 컨테이너를 재배포하거나 불명확한 슬롯을 승격하지 않는다.

`journalctl -u chongchong-edge-monitor.service -u chongchong-edge-finalize.service`에서
실행 실패를 확인한다. `status`의 `reason`과 함께 확인하며 외부 알림 채널은 별도 연동이다.

장애 후 제외된 서버는 단순 health 복구만으로 적격 상태가 되지 않는다. `rejoin SLOT`은
정상·저하 상태에서 비활성 서버의 릴리스가 활성과 같고 연속 건강 검사를 통과했을 때만
대기 자격을 복원하며 트래픽을 돌리지 않는다. 버전이 다르면 승인된 배포 절차로 대기를
다시 준비한다. 양쪽 동기화 이후 구버전이 필요하면
보존 bundle의 이미지·Compose와 이전 `backend.env`를 함께 복원하는 `restore BUNDLE APPROVAL`을 사용한다.
`deploy ARTIFACT`는 항상 **현재 `config/backend.env`**를 읽으므로 보존 bundle 경로만
넘겨서는 이전 설정이 복원되지 않는다. 원본 설정 복원과 현재 DB 호환성 확인이 선행되어야 한다.

보존 bundle은 자동 삭제하지 않는다. 롤백에 필요한 릴리스와 현재 상태가 참조하는 자료를
확인한 뒤 운영자가 별도 보관·정리 정책을 적용한다. backend의 배포 실행 파일도 원격
작업이 남아 있을 때 덮어쓰지 않도록 실행마다 고유 경로를 사용하므로, 진행 중 작업이
없는지 확인한 뒤 이전 실행 파일을 정리한다.

## 실제 서버 실험 전 합의할 항목

1. dev 대상 edge/A/B와 현재 활성 슬롯, 설치할 코드 버전을 확인한다.
2. 현재 routing, 실행 runtime, 서비스 설정, 이미지·앱 설정을 보존한다.
3. 정지할 대상·장애 방식·관찰 시간·중단 기준과 정상 상태 복구 방법을 정한다.
4. 기존 배포 및 다른 메모리·GC 실험과 시간이 겹치지 않게 한다.
5. 자동 전환을 중지할 수 있는 접속 경로와 수동 전환 절차를 준비한다.
6. 연속 요청 실패 수, 첫 실패부터 검증된 전환까지 시간, POST 처리 횟수,
   기존 연결 완료 여부, 양쪽 릴리스와 대기 자격을 기록한다.

prod 적용과 edge 이중화는 이 로컬 구현 작업의 범위에 포함하지 않는다.

## 로컬 검증 기록 — 2026-11-04

| 검증 | 결과 |
| --- | --- |
| `python3 -m unittest discover -s deploy/tests -v` | Python 3.9에서 67개 통과, 39.506초 |
| 배포·smoke 스크립트 `bash -n` | 통과 |
| `python3 -m py_compile deploy/scripts/edge/edge_*.py` | Python 3.9 문법 검사 통과 |
| edge Compose `config --quiet` | 통과 |
| backend Compose와 임시 pinned overlay의 `config --format json` | 최종 image가 지정 digest와 일치 |
| buildspec YAML 파싱, `git diff --check` | 통과 |

신규 검증은 실제 별도 프로세스의 `fcntl` 잠금·강제 종료·영속 상태와 실제 로컬 HTTP
서버·curl을 사용한다. CLI는 별도 프로세스로 확인했다. SSH·Docker 배포 동작은 테스트
대역으로 검증했으며, 그 결과를 실제 EC2 배포 성공으로 해석하지 않는다.

최초 검증에서 연결할 수 없었던 Docker 엔진은 사용자 폴더
`~/Applications/Docker.app`의 기존 Docker Desktop을 시작하여 준비했다. 추가 Docker
제품 설치 없이 `desktop-linux` context를 명시해 로컬에서 다음 검증을 완료했다.

| Docker 추가 검증 | 결과 |
| --- | --- |
| `DOCKER_CONTEXT=desktop-linux bash deploy/tests/edge-nginx-smoke.sh` | 통과. A → B → A 전환 중 150개 요청 실패 0건, 전환 전 시작한 긴 A 요청 완료 |
| 같은 edge smoke의 오류·POST 시나리오 | 잘못된 설정 거부·복원 후 재시작 성공. 처리 후 응답이 유실된 POST는 A에서 1회 처리되고 B에서는 재실행되지 않음 |
| `DOCKER_CONTEXT=desktop-linux bash deploy/tests/nginx-smoke.sh` | 통과. 설정 문법·worker 제한·JSON 로그·런타임 변수·컨테이너 재생성 검증 |
| Linux/ARM64 컨테이너에서 전체 unittest | Python 3.13에서 67개 통과, 38.992초. 실제 Linux 프로세스 잠금·강제 종료·상태 복구 포함 |

환경은 Docker Engine `29.6.1`, Nginx `1.28.3` (`nginx:1.28-alpine`)이다.
Nginx 이미지 digest는
`nginx@sha256:a8b39bd9cf0f83869a2162827a0caf6137ddf759d50a171451b335cecc87d236`이다.
Linux 테스트는 `deploy/`를 읽기 전용 마운트한 `python:3.13-alpine` 컨테이너에서
Bash·curl·coreutils·util-linux를 설치한 뒤 실행했다. 이 컨테이너는 종료 시 제거했다.

로그: [edge Nginx](../evidence/local-nginx-2026-11-04/edge-nginx-smoke.log),
[기존 Nginx](../evidence/local-nginx-2026-11-04/nginx-smoke.log),
[Linux 전체 테스트](../evidence/local-nginx-2026-11-04/linux-tests.log).
edge 로그의 `unknown directive "invalid"`는 잘못된 설정을 의도적으로 넣는 복원 테스트의
예상 출력이며 최종 종료 코드는 0이다. 테스트가 만든 컨테이너·네트워크는 정리됐다.
Docker Desktop은 다음 로컬 검증에 사용할 수 있도록 실행 상태로 두었다.

Nginx 검증은 테스트 backend와 실제 라우팅 교체를 사용한다. 전체 controller의 원격
SSH 배포는 여전히 테스트 대역으로 검증했다. systemd 설치·timer 실행, 실제 구·신 앱
이미지와 PostgreSQL의 공존·복귀 호환성, dev/prod 장애 전환 및 CI 실행 결과는 아직
확인하지 않았다. 실제 서버 접속이나 변경은 수행하지 않았다.

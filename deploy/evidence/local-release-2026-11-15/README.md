# 배포 변경 분리 후 로컬 검증 — 2026-11-15

기준: origin/dev `416be992` 위에 #635·#645 작업을 별도 worktree로 이식했다. 최신 config/scripts/docs 구조를 유지하고 ADR 번호 충돌을 0060으로 해결했다. 실제 서버·DB·Docker Hub 설정은 변경하지 않았다.

| 검증 | 결과 |
| --- | --- |
| `python3 -m unittest discover -s deploy/tests -v` (macOS Python 3.9) | 95개 통과, 스킵 0 |
| 같은 명령 (Linux ARM64 python:3.13-alpine, bash/curl/coreutils/util-linux 설치) | 95개 통과, 스킵 0 |
| `bash deploy/tests/edge-nginx-smoke.sh` | 150건 실패 0, A→B→A, 긴 요청 완료, 응답 유실 POST 재실행 없음 |
| `bash deploy/tests/nginx-smoke.sh` | 문법·worker 제한·JSON 로그·런타임 변수·재생성 통과 |
| `bash deploy/tests/host-monitoring-test.sh` | 8개 시나리오 통과 |
| 기본/dev/backend/edge(metrics) Compose config | 모두 통과 |
| Bash 문법, Python compileall, YAML 파싱, 브랜치 정책, diff 공백 검사 | 통과 |
| 격리 Linux 컨테이너에서 edge-install.sh 실행 | runtime·backend runner·timer 복사 및 Python 컴파일 통과. systemctl은 daemon-reload만 허용하는 대역 |

일반 배포가 config/의 Compose와 현재 env를 사용하는지, restore가 평평하게 보관한 과거 Compose/env를 사용하는지 회귀 테스트를 추가했다.

로그: [macOS](macos-tests.log), [Linux](linux-tests.log), [edge Nginx](edge-nginx-smoke.log), [Nginx](nginx-smoke.log), [runtime 설치](runtime-install.log).

## 미검증·미실행

- 실제 EC2 설치, systemd 활성화·장애 전환·이미지 삭제 및 구/신 앱 DB 호환성
- Docker Hub 실제 인증 및 예약 Actions 실행
- CodePipeline 실제 트리거 설정 조회. 저장소 문서의 환경 브랜치(dev/prod) push 정책과 Actions 정의를 확인했다.
- 기존 릴리스 디렉터리·bundle·원격 runner 자동 정리는 구현 범위 밖이다. 로컬 정리는 앱 이미지에 한정한다.

테스트 대역의 성공을 실제 서버 검증으로 해석하지 않는다. 스크립트·검증은 에이전트가 수행했다.

저장한 로그는 행 끝 공백을 제거했으며 macOS 테스트의 worktree 절대 경로를 `<worktree>`로 치환했다. Nginx 기동 대기 중 첫 curl 실패는 준비 확인 재시도이며, 본 전환 측정 150건의 실패 수와 구분한다.

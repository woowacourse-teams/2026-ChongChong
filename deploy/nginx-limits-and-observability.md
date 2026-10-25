# Nginx 연결 한도를 정한 실험과 기존 단일 서버 적용 기록

이 문서는 2026-10-16 실험과 **legacy 배포 경로**의 변경 기록이다.
연결 한도를 정한 근거는 참고할 수 있지만, 아래 `runtime.sh`·`.env`·dev override 명령은
현재 dev edge/A/B 운영 절차가 아니다. 현재 운영은 [배포 안내](README.md)와
[모니터링 확인](monitoring.md)을 따른다. 아래 검증 결과와 미확인 항목은 기록 당시 기준이다.

## 적용값과 로컬 근거

권장 시작값은 `worker_connections 4096`, `worker_rlimit_nofile 8192`, Compose `ulimits.nofile` soft/hard 각각 `8192`다. 워커 수는 기존처럼 `auto`를 유지한다. 이 값은 처리 속도를 높이는 설정이 아니라, 처리 중인 요청에 필요한 연결과 소켓을 확보하기 위한 한도다.

2026-10-16 KST에 Docker Desktop의 Linux 컨테이너로 비교했다. `nginx:1.28-alpine`의 실제 버전은 1.28.3, 이미지 digest는 `sha256:a8b39bd9cf0f83869a2162827a0caf6137ddf759d50a171451b335cecc87d236`이다. Nginx CPU 제한 2개, 메모리 제한 128MiB에서 별도 비동기 HTTP 서버가 3초 뒤 201을 반환하도록 했다. 1초 동안 1,050건을 시작해 처리 중인 연결이 겹치게 했다. 조건마다 두 번 반복했고 아래 결과는 두 반복에서 같았다.

| 워커 | 연결 한도 | FD soft/hard | 회당 성공 | 회당 실패 |
| ---: | ---: | ---: | ---: | --- |
| 2 | 1,024 | 1,024 | 1,012 | HTTP 500 38건 |
| 2 | 4,096 | 1,024 | 1,012 | HTTP 500 38건 |
| 2 | 2,048 | 8,192 | 1,050 | 없음 |
| 2 | 4,096 | 8,192 | 1,050 | 없음 |
| 1 | 1,024 | 1,024 | 987 | 응답 전 연결 종료 63건 |
| 1 | 4,096 | 1,024 | 1,014 | HTTP 500 36건 |
| 1 | 2,048 | 8,192 | 1,023 | 응답 전 연결 종료 27건 |
| 1 | 4,096 | 8,192 | 1,050 | 없음 |

전체 16회·16,800건을 보냈다. 각 회차의 마지막 요청 시작은 약 999~1,000ms였다. 낮은 FD 조건에서 `socket() failed (24: No file descriptors available)`를 재현했다. 연결 수만 늘리는 것으로는 해결되지 않았다. 2,048은 워커 2개에서는 충분했지만, 워커 하나로 집중시키면 부족했다. 4,096/8,192는 워커 1·2개 모두 두 번씩 1,050건 전체가 성공했고 연결 부족·FD 오류가 없었다. 각 실행에서 `/proc`의 실제 Nginx 프로세스 한도도 저장했다.

프록시는 클라이언트와 upstream 연결을 모두 사용한다. 요청 1,050건이 한 워커에 겹치면 단순 계산으로도 약 2,100개의 연결 슬롯이 필요하다. 4,096은 이 조건에 여유를 둔 값이고, 8,192는 소켓 외 파일·로그 등을 포함한 FD 여유를 둔 운영 시작값이다. 8,192가 최소 필요값임을 증명한 실험은 아니다.

이 시험은 TLS·CloudFront·실제 Java/DB·dev CPU 크레딧을 재현하지 않았다. 연결 종료를 실제 서비스의 CloudFront 502와 동일시하지 않는다. dev의 500 5건과 미확정 502 4건은 재시험에서 따로 추적해야 한다. 응답 p95 목표 달성도 별도다.

원본: `/Users/limhyejeong/Study/test/experiments/523-nginx-limits-20261016/`. `verified-results.json`은 Alpine의 FD 오류 문구를 반영해 로그에서 재집계한 결과다. 각 조건 폴더에 요청별 결과, 접근·오류 로그, 실제 한도가 있다. `experiment.py`, `upstream.py`, `summarize.py`를 함께 보관했다.

공식 근거: [Nginx worker_connections](https://nginx.org/en/docs/ngx_core_module.html#worker_connections), [worker_rlimit_nofile](https://nginx.org/en/docs/ngx_core_module.html#worker_rlimit_nofile).

## 저장소 설정과 배포 순서

`CLOUD_WATCH_APP_ENV=dev`일 때 `runtime.sh`의 `compose`가 `docker-compose.dev.yml`을 추가한다. 이 파일은 Nginx 메인 설정과 FD 한도를 적용한다. prod는 이 override를 사용하지 않는다. 라우팅은 두 환경 모두 `default.conf.template` 하나로 관리한다. API 로그 형식은 기본 Compose에서 `NGINX_API_LOG_FORMAT=main`, dev override에서 `loadtest`로 지정한다. JSON 로그 형식은 dev 메인 설정에서만 정의한다.

`NGINX_ENVSUBST_FILTER=^(SERVER_NAME|NGINX_API_LOG_FORMAT)$`로 치환 대상을 제한한다. Compose 파일에서는 끝의 `$`를 `$$`로 이스케이프한다. dev API 로그에는 요청 ID 두 종류, CloudFront ID, 연결 번호, URI, 상태, 전체·upstream 시간을 기록한다. 시간 단위는 초다. 인증 헤더·쿠키·본문·쿼리 문자열은 기록하지 않는다. dev API 로그가 기존 combined 형식에서 JSON으로 바뀌므로 외부 파서가 있다면 JSON 파서를 사용해야 한다. prod API 로그는 기존 main 형식을 유지한다.

배포는 다음 순서로 진행한다.

1. BeforeInstall에서 기존 배포 디렉터리를 `deploy-backups/before-install.*/deploy`에 보관한다.
2. Compose 검증과 이미지 pull을 수행한다.
3. 임시 Nginx 컨테이너에서 템플릿을 생성하고 `nginx -t`를 실행한다. 이 임시 컨테이너에서만 `backend`를 loopback으로 해석하게 해 최초 배포에도 문법 검사가 가능하게 한다. 실제 upstream 연결 검사는 이후 애플리케이션 확인 단계가 맡는다.
4. backend·nginx만 재생성하고 기동 및 실제 워커 한도를 확인한다. 실패하면 저장한 이전 배포 Compose와 이전 백엔드 이미지로 복구를 시도한다. 백업이 없으면 자동 복구 성공을 주장하지 않고 실패한다.
5. dev에서는 metrics 프로필 여부와 무관하게 수집기만 별도로 pull·재생성한다. prod는 명시적 metrics 프로필을 유지한다. 수집기 실패는 배포 실패로 보고하지만 정상 기동한 애플리케이션을 다시 재시작하지 않는다.
6. 수집기의 실행·재시작·OOM 여부와 수집 환경변수 일치를 검사한다. 기본 수집 주기는 60초, timeout은 10초, 메모리 제한은 기존 192MiB다.

수집기 재생성 중 지표 공백과 누적값 기준점 초기화가 생길 수 있다. 수집기가 켜졌다는 것만으로 CloudWatch 전송 성공을 보장하지 않는다. 고빈도 수집 설정은 CPU·메모리 부담을 늘릴 수 있으므로 부하 시험 후 기본값으로 돌린다.

## dev 반영 전 보존할 것

배포 디렉터리 백업에는 실행 컨테이너 로그와 `/tmp`의 JFR이 포함되지 않는다. backend 재생성 전에 필요한 JFR을 호스트로 `docker cp`하고, Nginx·백엔드 진단 로그와 k6 원본을 보존한다. 다른 사람이 부하 시험 중인지 확인한다. 이번 작업에서는 dev에 배포하거나 기존 컨테이너를 재생성하지 않았다.

## 배포 후 검증

```bash
sudo bash -c 'source /opt/chongchong/deploy/scripts/runtime.sh; load_deployment_environment; verify_nginx_limits; verify_metrics_collector'

CHECK_ID="nginx-check-$(date +%s)"
curl -sS -o /dev/null -w 'HTTP %{http_code}\n' \
  -H "X-Request-Id: $CHECK_ID" https://dev.chongchong.kr/api/studies
sudo docker logs --since 2m chongchong-nginx 2>&1 | grep -F "$CHECK_ID"

sudo docker logs --since 3m chongchong-metrics-collector
sudo docker stats --no-stream
```

미인증 요청의 401은 로그 검증용으로 정상이다. 요청 ID·최종 상태·upstream 상태 및 시간이 JSON으로 남는지 확인한다. 최소 두 수집 주기 이후 CloudWatch에서 새 데이터가 들어오는지 확인하고, 배포 대상 EC2 인스턴스 ID·이미지·시각과 해당 호스트 수집기 로그를 함께 기록한다. `environment=dev`만으로 어느 EC2의 데이터인지 판정하지 않는다. 수집기 로그의 인증·OTLP 오류와 `Partial success response`도 확인한다.

새 미제출 데이터·유효 토큰으로 1초 1,050건을 다시 전송한다. k6 실제 전송 수, 시작 분포, dropped iterations, 201·500·502, p95·최대 시간과 Nginx 오류를 대조한다. CPU·Hikari 대기·수집 누락도 함께 기록한다. 재생성으로 한도가 적용된 것과 실제 서비스 오류가 해결된 것을 구분한다.

## 복구 범위

애플리케이션 자동 복구는 이전 배포 디렉터리의 설정을 마운트한다. 복구에 사용 중인 백업은 삭제하지 않는다. 수집기 설정 실패는 이전 `otel-collector.yaml`을 복구하고 metrics 서비스만 다시 생성한다. 이전 배포 산출물로 재배포하면 Nginx 설정과 수집기 자동 적용 코드도 함께 되돌릴 수 있다. 이미 자동 복구를 수행했다면 다음 배포 전에 실행 중인 컨테이너의 마운트가 어느 디렉터리를 가리키는지 확인한다. 백업의 자동 삭제 정책은 추가하지 않았다.

`ulimits`는 컨테이너 생성 옵션이므로 Nginx reload만으로 적용하지 못한다. 수동 Compose 실행도 dev override를 포함해야 한다.

```bash
sudo docker compose --env-file /opt/chongchong/.env \
  --env-file /opt/chongchong/deploy/image.env \
  -f /opt/chongchong/deploy/docker-compose.yml \
  -f /opt/chongchong/deploy/docker-compose.dev.yml \
  up -d --no-deps --force-recreate nginx
```

## 수행한 검증과 남은 확인

- 배포 분기·실패·복구 경로와 수집기 환경변수 검사: Python unittest 12개 통과. Docker 호출을 대체한 제어 흐름 테스트다.
- 실제 Nginx Docker 컨테이너: `nginx -t`, 워커 FD 8,192, API JSON 로그·upstream 404, 민감 헤더·쿼리 제외, 템플릿 변수 보존, 두 번의 컨테이너 생성 검증 통과.
- 기본 Compose 및 dev override 구성 검증, 셸 문법 검사 통과.
- `.github/workflows/deploy-ci.yml`에 위 검증을 추가했다. GitHub 원격 CI는 아직 실행하지 않았다.
- 실제 ADOT 기동·CloudWatch 전송, EC2 CodeDeploy 복구와 dev 1초 재부하는 아직 확인하지 않았다. 이 확인 전에는 #513·#523을 완료 처리하지 않는다.

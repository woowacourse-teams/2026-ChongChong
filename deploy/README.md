# Backend deployment

`dev` 또는 `prod`에 병합된 백엔드 변경을 환경별 CodePipeline이 CodeBuild와 CodeDeploy를 거쳐 대상 단일 EC2에
배포하도록 구성한다. 이 문서는 저장소에 포함할 수 없는 AWS Resource와 EC2 최초 설정을 연결하는 절차다.

## 1. 배포 전 확인

- 현재 `buildspec.yml`은 `linux/arm64` Docker 이미지를 생성하므로 배포 대상 EC2도 ARM64 환경이어야 한다.
- CodeDeploy Agent는 Ubuntu 26.04와 Ruby 없는 실행을 지원하는 v2를 설치한다.
- EC2 outbound HTTPS 443 통신을 허용한다.
- EC2 inbound 80과 443을 인터넷에 허용하고 22는 기존 관리 IP로만 제한한다.
- RDS 5432 inbound source가 EC2의 Security Group인지 확인한다.
- EC2에 CodeDeploy용 Instance Profile을 연결한다.
- Docker Hub에 백엔드 이미지 Repository와 push/pull Token을 준비한다.
- EC2 공개 IP가 바뀌면 `SERVER_NAME`과 인증서도 바뀌므로 개발 기간 중 IP 변경 여부를 관리한다.

## 2. EC2를 한 번 준비한다

허용된 위치에서 저장소의 `deploy` 디렉터리를 EC2로 복사한다. `deploy/.env.example`을 참고하여
`/opt/chongchong/.env`를 생성하고 실제 값을 입력한다. 이 파일은 Git에 커밋하지 않는다.

```bash
sudo install -d -m 0755 /opt/chongchong
sudo install -m 0600 deploy/.env.example /opt/chongchong/.env
sudoedit /opt/chongchong/.env
sudo deploy/scripts/bootstrap-ec2.sh
```

bootstrap은 Docker Engine과 Compose Plugin, Host Certbot, CodeDeploy Agent를 설치한다. 최초 인증서는 80번 포트의
임시 Nginx 컨테이너와 Certbot webroot 방식으로 발급한다. 이후 systemd timer가 매일 갱신 필요 여부를 확인하고 실제로
갱신한 경우에만 Nginx를 reload한다.

`/opt/chongchong/.env`에는 다음 범주의 값이 필요하다.

- AWS Region, `nip.io` 서버 이름과 Certbot 이메일
- Docker Hub 사용자명과 최소 pull 권한 Token
- PostgreSQL JDBC URL, 사용자명과 비밀번호
- Study Invite 및 인증 JWT 설정
- 프론트엔드 기준 URL과 선택적인 CORS 허용 Origin 목록
- Kakao REST API 키, Client Secret과 환경별 Redirect URI
- Web Push VAPID 키와 푸시 클릭 토큰 서명 키

`WEB_PUSH_NOTIFICATION_CLICK_TOKEN_SECRET`에는 `openssl rand -base64 32`로 생성한 값을 설정한다. 같은 환경의 모든 백엔드 인스턴스는 같은 값을 사용하고, dev와 prod는 서로 다른 값을 사용해도 된다. 실제 값은 `/opt/chongchong/.env`에만 저장한다.

배포 스크립트가 이 파일을 읽으므로 각 줄은 Bash에서 읽을 수 있는 `KEY=VALUE` 형식으로 작성한다. 공백이나 `$`,
따옴표처럼 Shell에서 의미가 있는 문자가 포함된 값은 작은따옴표로 감싼다.

`FRONTEND_ALLOWED_ORIGINS`는 쉼표로 구분한 정확한 Origin 목록이며, 생략하면 `FRONTEND_BASE_URL`을 사용한다.
로컬 프론트엔드도 허용할 때만 `https://chongchong.app,http://localhost:3005`처럼 추가하고 wildcard는 사용하지 않는다.

Kakao Authorization Code 로그인에는 다음 설정을 사용한다.

| 변수 | 필수 여부 | 용도와 기본값 |
| --- | --- | --- |
| `AUTH_KAKAO_REST_API_KEY` | 필수 | Kakao Developers 애플리케이션의 REST API 키 |
| `AUTH_KAKAO_CLIENT_SECRET` | 필수 | Kakao Developers에서 활성화한 Client Secret |
| `AUTH_KAKAO_REDIRECT_URI` | 필수 | 환경별 웹 Callback URI |

`AUTH_KAKAO_REDIRECT_URI`는 Kakao Developers에 등록한 URI, 프론트엔드가 Authorization Code를 받는 URI와
백엔드가 Token 교환에 사용하는 URI가 완전히 같아야 한다. 로컬은 `http://localhost:3005/auth/kakao/callback`,
배포 환경은 같은 `/auth/kakao/callback` 경로를 사용하는 실제 프론트엔드 HTTPS 주소로 설정한다. REST API 키와
Client Secret의 실제 값은 `/opt/chongchong/.env`에만 저장하고 저장소에는 커밋하지 않는다.

### dev 알림 부하 테스트 sender 설정

Compose는 `/opt/chongchong/.env`의 `SPRING_PROFILES_ACTIVE`, `WEB_PUSH_SENDER_MODE`,
`WEB_PUSH_SENDER_SIMULATED_DELAY_MS`를 backend 컨테이너에 전달한다. 기본값은 활성 프로필 미지정,
`real`, `500ms`이므로 prod의 기존 `.env`에 새 변수를 추가하지 않아도 실제 전송을 유지한다.

다른 사용자 작업이 섞이지 않은 dev 테스트 DB와 큐를 확인한 뒤 dev EC2의 `/opt/chongchong/.env`에 설정한다.

```dotenv
CLOUD_WATCH_APP_ENV=dev
SPRING_PROFILES_ACTIVE=load-test
WEB_PUSH_SENDER_MODE=simulated
WEB_PUSH_SENDER_SIMULATED_DELAY_MS=500
```

기존 활성 프로필이 필요하면 유지하고 `load-test`를 추가한다. `prod` 프로필은 포함하지 않는다.
시뮬레이터는 `app.environment=dev`, `load-test` 프로필과 `simulated` 모드를 함께 요구한다.
기존 DB·인증·VAPID 설정은 유지한다. `.env` 변경 후 재배포하면 배포 스크립트가 backend를 재생성한다.
단순 `docker restart`로는 변경한 환경 변수가 적용되지 않는다. 별도 Compose override는 필요하지 않다.

테스트를 시작하기 전에 backend 컨테이너의 위 환경 변수와 시뮬레이터 시작 로그를 확인한다.
종료 시 테스트 Delivery와 합성 구독을 정리한 뒤 `load-test`를 제거하고 모드를 `real`로 복원해 재배포한다.
데이터 준비와 20건 예행시험은 [알림 부하 테스트 sender](../backend/docs/load-test-sender.md)를 따른다.

### 알림 발송 및 종료 설정

다음 값은 `/opt/chongchong/.env`에서 설정한다. 기존 `.env`에 없으면 표의 기본값을 사용한다.

| 변수 | 기본값 | 용도 |
| --- | --- | --- |
| `WEB_PUSH_DELIVERY_CONCURRENCY` | `1` | 최대 동시 발송 수 |
| `WEB_PUSH_DELIVERY_SEND_TIMEOUT` | `5s` | Web Push 응답 대기 제한 시간 |
| `WEB_PUSH_DELIVERY_SHUTDOWN_TIMEOUT` | `8s` | 종료 시 발송 작업 완료를 기다리는 시간 |
| `BACKEND_STOP_TIMEOUT_SECONDS` | `60` | 컨테이너 강제 종료 전 유예 시간(초) |

종료 대기 시간은 발송 제한 시간보다 1초 이상 길어야 한다. 애플리케이션은 발송 작업이 완료되거나 종료 대기
시간이 지날 때까지 JPA와 데이터소스를 유지한다. 컨테이너 유예 시간은 이 대기와 Spring의 다른 종료 단계를
포함한 전체 종료 시간보다 길게 설정한다.

`BACKEND_STOP_TIMEOUT_SECONDS`는 backend의 `stop_grace_period`와 재배포·롤백 시 Compose `up --timeout`에
동일하게 적용된다. 새 종료 정책을 처음 배포할 때에도 기존 컨테이너를 이 시간만큼 기다린다.

## 3. CodeBuild Project를 구성한다

| 설정 | 값 |
| --- | --- |
| Source | CodePipeline |
| Artifacts | CodePipeline |
| Environment | Managed image, x86_64, ARM64 cross-platform build 지원 |
| Image | `aws/codebuild/amazonlinux-x86_64-standard:6.0` |
| Compute | `BUILD_GENERAL1_SMALL` |
| Privileged mode | 활성화 |
| Buildspec | `buildspec.yml` |

현재 x86_64 CodeBuild Project는 `docker build --platform linux/arm64`를 실행한다. Project를 다시 만들 때는 선택한
Managed Image의 Docker Builder가 binfmt/QEMU를 통한 ARM64 cross-platform build를 지원하는지 먼저 검증한다. 이
조건을 충족하지 못하면 native ARM64 CodeBuild 환경을 사용해야 한다.

CodeBuild 환경에는 다음 변수를 설정한다.

| 변수 | 용도 | 저장 방식 |
| --- | --- | --- |
| `DOCKERHUB_USERNAME` | Docker Hub push 사용자 | Parameter Store `/chongchong/dev/dockerhub/username` |
| `DOCKERHUB_REPOSITORY` | 예: `docker.io/team/chongchong-backend` | 일반 변수 가능 |
| `DOCKERHUB_TOKEN` | Docker Hub push Token | Parameter Store `/chongchong/dev/dockerhub/token` |

CodeBuild Service Role에는 username과 Token 두 파라미터에 대한 `ssm:GetParameters` 권한만 부여한다.
`DOCKERHUB_TOKEN`을
plaintext 환경 변수나 저장소 파일에 넣지 않는다.

CodeBuild는 Corretto 25로 `bootJar`를 실행하고 ARM64 이미지를 Docker Hub에 commit SHA 태그로만 push한다.
CodeDeploy artifact에는 애플리케이션 소스나 JAR 대신 `appspec.yml`, Compose, Nginx, 스크립트와 확정된 `image.env`만
포함한다.

`dev`와 `prod` Pipeline은 같은 Docker Hub Repository와 push 자격 증명을 사용해도 되는 동일한 신뢰 경계에 있을
때만 CodeBuild Project와 `buildspec.yml`을 공유한다. 환경별 write 권한 격리가 필요하면 CodeBuild Project, Service
Role, Parameter Store 경로와 이미지 Repository를 분리한다. 현재 Parameter Store의 `/chongchong/dev/dockerhub/*`
경로를 공용으로 사용하는 것은 전자의 조건을 전제로 한다.

각 실행의 `CODEBUILD_RESOLVED_SOURCE_VERSION`을 commit SHA별 이미지 태그와 `image.env`에 기록하므로 정상적인
Pipeline 실행끼리는 `dev`와 `prod` 이미지 태그가 충돌하지 않는다. Docker Hub 태그 자체는 덮어쓸 수 있으므로 이
구성은 Registry 수준의 불변성을 보장하지 않는다. 더 강한 이미지 무결성이 필요하면 push 결과의 manifest digest를
artifact에 기록하고 digest로 배포해야 한다. `dev`나 `prod` 같은 가변 태그는 생성하지 않는다.

## 4. CodeDeploy를 구성한다

1. EC2/On-Premises compute platform의 Application을 생성한다. `dev`와 `prod`가 같은 Application을 공유할 수 있다.
2. 환경마다 EC2 tag로 대상 서버 한 대를 선택하는 Deployment Group을 생성한다. 개발 서버와 운영 서버가 다르면
   Deployment Group도 분리한다.
3. Deployment Type은 `In-place`, Configuration은 `CodeDeployDefault.AllAtOnce`를 사용한다.
4. 배포 실패 시 자동 rollback을 활성화한다.
5. CodeDeploy Service Role과 EC2 Instance Profile을 연결한다.

EC2 Instance Profile은 Pipeline artifact S3 읽기에 필요한 최소 권한을 가져야 한다. 현재처럼 공개 CodeDeploy
Endpoint를 사용하는 Agent에는 별도의 `codedeploy-commands-secure` 권한이 필요하지 않으며, 이 권한은 CodeDeploy용
VPC Endpoint와 IAM 인증을 사용할 때만 추가한다. CodeDeploy Service Role은 대상 EC2 tag와 배포 상태를 관리하는 데
필요한 권한을 가져야 한다. Pipeline Service Role도 해당 환경의 Deployment Group과 artifact 경로만 조작하도록
권한을 제한한다.

## 5. CodePipeline V2를 구성한다

`dev`와 `prod` 환경마다 Pipeline을 하나씩 두고 `Source -> Build -> Deploy` 순서로 구성한다.

### Source

- Provider: GitHub via CodeStarSourceConnection
- Repository: `woowacourse-teams/2026-ChongChong`
- Output artifact format: CodePipeline default
- Trigger event: Push
- Include branch: Pipeline 환경과 같은 `dev` 또는 `prod`
- Include file paths:
  - `backend/**`
  - `deploy/**`
  - `buildspec.yml`
  - `appspec.yml`

### Build

- Provider: CodeBuild
- Input: Source artifact
- Output: Build artifact
- Project: 3단계에서 만든 공용 x86_64 CodeBuild Project

### Deploy

- Provider: CodeDeploy
- Input: Build artifact
- Application: 4단계에서 만든 공용 Application
- Deployment Group: Pipeline 환경에 대응하는 Deployment Group

프론트엔드 파일만 바뀐 `dev` 또는 `prod` push에서는 Pipeline을 실행하지 않는다. 위 배포 경로 중 하나가 바뀌면
구현 단순성을 위해 JAR과 Docker 이미지를 모두 새로 만든다.

## 6. 최초 배포를 확인한다

Pipeline 성공 후 다음을 확인한다.

```bash
sudo systemctl status codedeploy-agent
sudo docker compose \
  --env-file /opt/chongchong/.env \
  --env-file /opt/chongchong/deploy/image.env \
  --file /opt/chongchong/deploy/docker-compose.yml \
  ps
sudo bash -c '
  set -Eeuo pipefail
  set -a
  source /opt/chongchong/.env
  set +a
  status_code="$(curl --silent --show-error --output /dev/null --write-out "%{http_code}" \
    --resolve "${SERVER_NAME}:443:127.0.0.1" \
    "https://${SERVER_NAME}/actuator/health")"
  [[ "${status_code}" =~ ^2[0-9]{2}$ ]]
'
```

배포 스크립트는 backend와 Nginx 컨테이너가 실행되고 Nginx에서 backend 8080 포트로 연결할 수 있는지 확인한 뒤,
Spring Boot health endpoint가 HTTPS 2xx로 응답하는지 검증한다.
새 컨테이너 시작에 실패하면 직전 backend 이미지로 복구를 시도한다. 단일 EC2를 사용하므로 배포 중 짧은 중단은
허용하며, 운영 전 무중단 전환은 별도 결정으로 다룬다.

## 7. CloudWatch HTTP·Hikari·JVM 메트릭 수집기를 환경별로 활성화한다

[ADR-0044](../backend/docs/adr/0044-collect-http-metrics-with-adot-and-cloudwatch.md)에 따라 각 EC2의 ADOT
수집기는 사설 Docker 네트워크에서 백엔드의 `/actuator/prometheus`를 읽고 CloudWatch의 OTLP 메트릭
엔드포인트로 전송한다. AMP workspace나 추가 메트릭 서버는 만들지 않는다. `CLOUD_WATCH_AWS_REGION`과
`CLOUD_WATCH_APP_ENV`는 기존 로그 설정값을 사용한다. dev에서는 `dev`, prod에서는 `prod`를 설정한다.
컨테이너에 정적 AWS 키를 넣지 않고 EC2 Instance Profile의 임시 자격 증명을 사용한다. 메트릭용 inbound
포트는 열지 않으며 CloudWatch `monitoring` 엔드포인트로 HTTPS 443 outbound가 가능해야 한다.

EC2 역할에는 `cloudwatch:PutMetricData` 권한이 필요하다. dev의 `ec2-project` 역할에서는 CLI의 기본
`PutMetricData` 호출이 성공했다. 이는 수집기의 OTLP 전송까지 확인한 것은 아니다. prod에서도 실제 역할과
권한을 확인한다. 수집기에서 자격 증명을 읽지 못하면 EC2 IMDSv2 접근과 응답 hop limit을 확인한다. hop limit
변경은 같은 호스트의 다른 컨테이너에도 역할 접근을 허용할 수 있으므로 적용 범위를 검토한다.

수집기 활성화 전 `free -h`, `vmstat 1 5`, `df -h /`, `sudo docker stats --no-stream`으로 기준값을 확인한다.
2026-09-29 측정 기준 prod `t4g.small`의 가용 메모리는 766MiB, dev `t4g.micro`는 209MiB였다. prod 루트
파일시스템은 이후 확장했으므로 현재 여유 용량을 다시 확인한다. dev 백엔드는 약 305MiB를 사용했고 스왑
337MiB가 사용 중이었다. 수집기의 192MB 제한은 최대치일 뿐 실제 사용량이나 안전한 여유를 보장하지 않는다.

`CLOUD_WATCH_APP_ENV=dev`이면 배포 스크립트가 애플리케이션 기동 확인 후 수집기를 별도로 pull·재생성한다.
dev에서는 `COMPOSE_PROFILES=metrics`가 없어도 반영된다. prod는 기존처럼 명시적 프로필 활성화가 필요하다.
수동으로 수집기만 적용할 때는 아래 명령을 사용한다. 변경 범위와 복구·검증은
[Nginx 연결 한도와 수집기 배포](nginx-limits-and-observability.md)를 따른다.

```bash
sudo docker compose \
  --env-file /opt/chongchong/.env \
  --env-file /opt/chongchong/deploy/image.env \
  --file /opt/chongchong/deploy/docker-compose.yml \
  --profile metrics up --detach --no-deps metrics-collector
sudo docker logs --tail 100 chongchong-metrics-collector
free -h
vmstat 1 30
sudo docker stats --no-stream
sudo docker inspect --format '{{.State.OOMKilled}}' chongchong-metrics-collector
```

Prometheus 출력의 누적 히스토그램에는 OTLP 시작 시각이 없어 수집기가 `cumulativetodelta`로 수집 간 증가분을
만든다. 첫 관측값은 기준값으로만 사용하므로 최소 두 번의 수집(기본 주기 60초) 이후 전송 결과를 확인한다. 수집기 로그에서
`Partial success response`와 자격 증명·네트워크·OTLP 전송 오류가 없는지 확인한다. CloudWatch Query Studio에서 실제
HTTP 메트릭 이름과 `environment` 속성을 확인하고, API 요청 전후 요청 수가 증가하는지 조회한다. 통제된 dev
환경에서만 5xx 요청과 p95 계산을 검증한다. Prometheus 출력이 OTLP로 변환되므로 실제 이름과 쿼리를 확인한
뒤 바니에게 전달한다. Query Studio 읽기 권한은 EC2의 쓰기 권한과 별개다. 공개 HTTPS에서
`/actuator/prometheus`가 프록시되지 않는지도 확인한다.

`vmstat`의 첫 줄은 부팅 이후 평균이므로 그 뒤의 `si`·`so`를 본다. 지속적인 스왑 입출력, OOM, 백엔드
응답 악화 또는 전송 실패가 보이면 아래 명령으로 수집기만 중지한다.

```bash
sudo docker compose \
  --env-file /opt/chongchong/.env \
  --env-file /opt/chongchong/deploy/image.env \
  --file /opt/chongchong/deploy/docker-compose.yml \
  --profile metrics stop metrics-collector
```

dev는 이후 배포에서 수집기를 재생성하므로 수동 중지는 다음 배포까지의 조치다. 지속적으로 중단해야 한다면
수집기 자동 배포 변경을 되돌린 배포본을 사용한다. prod는 별도로 권한·자원·실제 메트릭을 검증한 뒤
`COMPOSE_PROFILES=metrics`를 설정한다. 수집 중단 구간에는 데이터 공백이 생길 수 있지만
백엔드의 요청 처리와 CloudWatch Logs 수집은 계속 동작한다. CloudWatch OpenTelemetry 메트릭은 수집
데이터량에 따라 과금되므로 실제 수집량과 비용을 확인한다.

### 제출 집중 부하에서 Hikari·JVM 지표 확인

기존 HTTP·알림 지표와 함께 아래 계열을 허용한다. 백엔드가 이미 노출하는 지표를 전달하며, 허용 목록 추가가 새 계측이나 히스토그램 버킷을 만들지는 않는다.

| 지표 계열 | 확인 목적 |
| --- | --- |
| `hikaricp_*` | 연결 풀 active·idle·max·pending, 획득·사용·생성 시간, 획득 timeout |
| `jvm_memory_(used/committed/max)_bytes` | JVM 영역별 메모리 사용량·한도 |
| `jvm_gc_*` | GC 정지 시간과 할당·회수 관련 지표 |
| `jvm_threads_*` | JVM 스레드 수·상태 |
| `process_cpu_usage`, `system_cpu_usage` | JVM 프로세스와 실행 환경 CPU 사용률 |
| `process_uptime_seconds` | 재시작에 따른 지표 초기화 구분 |

Hikari pending은 수집 순간의 대기 수다. 60초 수집으로는 3초 부하를 놓칠 수 있으므로 dev 진단 실험에서만 `/opt/chongchong/.env`에 다음 값을 설정할 수 있다.

```dotenv
METRICS_SCRAPE_INTERVAL=1s
METRICS_SCRAPE_TIMEOUT=1s
```

timeout은 interval보다 길면 안 된다. 기본값은 60s·10s이며 prod도 기본값을 유지한다. 1초 설정은 수집 횟수를 60배로 늘리므로 수집기 CPU·메모리와 전송 오류를 확인하고 실험 후 기본값으로 복원한다. 1초 수집도 모든 순간 대기를 포착하지는 못한다.

YAML과 Compose 파일을 배포 위치에 함께 반영한 뒤 수집기만 재생성한다. 환경변수 추가는 단순 restart로 반영되지 않는다. 애플리케이션 재빌드나 연결 풀 크기 변경은 필요 없다.

```bash
sudo docker compose \
  --env-file /opt/chongchong/.env \
  --env-file /opt/chongchong/deploy/image.env \
  --file /opt/chongchong/deploy/docker-compose.yml \
  --profile metrics up --detach --no-deps --force-recreate metrics-collector
sudo docker logs --tail 100 chongchong-metrics-collector
sudo docker exec chongchong-nginx \
  wget -qO- -T 10 http://backend:8080/actuator/prometheus \
  | grep -E '^(hikaricp_|jvm_memory_|jvm_gc_|jvm_threads_|process_cpu_usage|system_cpu_usage|process_uptime_seconds)'
```

위 출력은 백엔드 노출 여부만 확인한다. 실제 CloudWatch 전송은 Query Studio에서 변환된 지표 이름, dev 환경·서비스·인스턴스·pool 라벨을 확인한다. 조회 step을 줄여도 원래 수집 간격보다 세밀한 자료가 생기지는 않는다. batch 전송 주기 10초는 그대로이며 수집 시각과 화면에 도착하는 시각을 구분한다.

`acquire`와 `usage`의 count·sum 증가분으로 구간 평균을 비교한다. 버킷이 없으면 p95를 계산할 수 없고 max는 전체 실행의 최대값을 보장하지 않는다. pending 상승과 active가 max에 도달하는 시점, 획득 시간 증가가 함께 관측되는지 확인한다. 이것만으로 근본 원인을 확정하지 말고 SQL·잠금·트랜잭션 점유 시간과 연결한다.

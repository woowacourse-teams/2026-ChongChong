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

## 7. Prometheus 메트릭 수집기를 환경별로 활성화한다

[ADR-0043](../backend/docs/adr/0043-collect-http-metrics-with-adot-and-amp.md)에 따라 `dev`와 `prod`의
AWS 계정에 AMP workspace를 각각 만들고 보존 기간을 30일로 설정한다. 이 설정은 비용을 발생시키므로 먼저 해당
계정의 결제 정보와 리전별 단가를 확인한다. 환경별 EC2 Instance Profile에는 **자기 workspace** ARN으로 제한한
`aps:RemoteWrite` 권한을 추가한다. ADOT 컨테이너가 Instance Profile을 읽을 수 있도록 IMDSv2 접근과 응답
hop limit을 확인한다. 컨테이너에 정적 AWS 키를 넣지 않는다. EC2에서 AMP endpoint로 HTTPS 443 outbound가
가능해야 한다. Docker 네트워크와 Security Group에 메트릭용 inbound 포트를 추가할 필요는 없다.

Instance Profile에 추가할 IAM 정책의 형태는 다음과 같다. 계정 ID와 workspace ID를 각 환경의 실제 값으로
바꾸고, 기존 CodeDeploy·CloudWatch Logs 권한은 유지한다.

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": "aps:RemoteWrite",
      "Resource": "arn:aws:aps:ap-northeast-2:123456789012:workspace/ws-01234567-89ab-cdef-0123-456789abcdef"
    }
  ]
}
```

수집기 활성화 전 해당 EC2에서 `free -h`, `vmstat 1 5`, `df -h /`, `sudo docker stats --no-stream` 결과를
확인한다. 2026-09-29 측정 기준 prod `t4g.small`의 가용 메모리는 766MiB, dev `t4g.micro`는 209MiB였다.
prod의 루트 파일시스템은 이후 확장했으므로 새 여유 공간을 다시 확인한다. dev 백엔드는 약 305MiB를 사용했고
스왑 337MiB가 사용 중이었다. 수집기의 192MB 제한은 최대치일 뿐 실제 사용량이나 안전한 여유를 보장하지 않는다.
따라서 dev는 먼저 단기 시험을 하고, 백엔드 응답이나 스왑 입출력이 악화되면 수집기를 내린 뒤 인스턴스 증설
또는 수집 위치 변경을 결정한다.

준비가 끝나면 `/opt/chongchong/.env`에 다음 값을 환경별로 추가한다. URL은 해당 workspace의 실제
`remote_write` endpoint를 사용한다. dev 단기 시험에서는 `COMPOSE_PROFILES`를 아직 설정하지 않고 아래의
`--profile metrics` 명령으로 수집기만 실행한다. 안정성을 확인한 뒤 `COMPOSE_PROFILES=metrics`를 추가하면
이후 배포에서도 수집기가 함께 실행된다.

```dotenv
AMP_AWS_REGION=ap-northeast-2
AMP_REMOTE_WRITE_URL=https://aps-workspaces.ap-northeast-2.amazonaws.com/workspaces/ws-01234567-89ab-cdef-0123-456789abcdef/api/v1/remote_write
CLOUD_WATCH_APP_ENV=dev
```

`prod`에서는 마지막 값을 `prod`로 설정한다. `AMP_AWS_REGION`은 workspace의 리전과 일치해야 한다.
작업 순서는 dev workspace·권한 준비 → dev 단기 시험·자원 확인 → dev 상시 활성화 → prod workspace·권한
준비 → prod 활성화·검증이다. dev가 불안정하면 인스턴스 증설 또는 수집 위치 변경 후 재검증한다.

dev에서 단기 시험할 때는 백엔드가 이미 실행 중인 상태에서 다음 명령으로 수집기만 시작한다.

```bash
sudo docker compose \
  --env-file /opt/chongchong/.env \
  --env-file /opt/chongchong/deploy/image.env \
  --file /opt/chongchong/deploy/docker-compose.yml \
  --profile metrics up --detach --no-deps metrics-collector
free -h
vmstat 1 30
sudo docker stats --no-stream
sudo docker inspect --format '{{.State.OOMKilled}}' chongchong-metrics-collector
```

`vmstat`의 첫 줄은 부팅 이후 평균이므로 그 뒤의 `si`·`so`를 본다. 지속적인 스왑 입출력, OOM 또는 백엔드
응답 악화가 보이면 수집기를 중지하고 상시 profile을 설정하지 않는다. 단기 시험을 끝내거나 중지할 때는
`sudo docker rm --force chongchong-metrics-collector`로 수집기 컨테이너만 제거한다.
각 서버에서 배포 후 `docker compose ... ps metrics-collector`와 컨테이너 로그를 확인하고, AMP에서
`up{job="chongchong-backend",environment="dev"}` 또는 `prod`가 1인지 확인한다. 실제 API를 여러 번 호출한 뒤
`http_server_requests_seconds_count{job="chongchong-backend",environment="dev"}`가 증가하는지 확인한다.
메트릭 이름과 라벨은 실제 AMP 조회 결과로 확정한다. 공개 HTTPS에서 `/actuator/prometheus`가 프록시되지
않는지도 확인한다.

```bash
sudo docker compose \
  --env-file /opt/chongchong/.env \
  --env-file /opt/chongchong/deploy/image.env \
  --file /opt/chongchong/deploy/docker-compose.yml \
  ps metrics-collector
sudo docker logs --tail 100 chongchong-metrics-collector
```

수집기가 백엔드 자원을 압박하거나 전송 실패가 지속되면 `.env`의 `COMPOSE_PROFILES=metrics`를 제거하고
일반 배포 명령의 `--remove-orphans`로 수집기만 내린다. 이때 백엔드의 메트릭 노출과 CloudWatch 로그 수집은
계속 동작한다. ADOT은 로컬 시계열을 영구 보존하지 않으므로 전송 중단 구간에는 데이터 공백이 생길 수 있다.

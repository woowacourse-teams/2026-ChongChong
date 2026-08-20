# Backend deployment

`dev`에 병합된 백엔드 운영 변경을 CodePipeline, CodeBuild와 CodeDeploy로 단일 EC2에 배포한다. 이 문서는 저장소에
포함할 수 없는 AWS Resource와 EC2 최초 설정을 연결하는 절차다.

## 1. 배포 전 확인

- EC2는 CodeDeploy Agent가 공식 지원하는 Ubuntu 22.04 LTS인지 확인한다.
- EC2 Architecture는 `arm64`, Instance Type은 `t4g.micro`를 사용한다.
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
- 프론트엔드 기준 URL

배포 스크립트가 이 파일을 읽으므로 각 줄은 Bash에서 읽을 수 있는 `KEY=VALUE` 형식으로 작성한다. 공백이나 `$`,
따옴표처럼 Shell에서 의미가 있는 문자가 포함된 값은 작은따옴표로 감싼다.

## 3. CodeBuild Project를 구성한다

| 설정 | 값 |
| --- | --- |
| Source | CodePipeline |
| Artifacts | CodePipeline |
| Environment | Managed image, ARM |
| Image | `aws/codebuild/amazonlinux-aarch64-standard:3.0` |
| Compute | `BUILD_GENERAL1_SMALL` |
| Privileged mode | 활성화 |
| Buildspec | `buildspec.yml` |

CodeBuild 환경에는 다음 변수를 설정한다.

| 변수 | 용도 | 저장 방식 |
| --- | --- | --- |
| `DOCKERHUB_USERNAME` | Docker Hub push 사용자 | 일반 변수 가능 |
| `DOCKERHUB_REPOSITORY` | 예: `docker.io/team/chongchong-backend` | 일반 변수 가능 |
| `DOCKERHUB_TOKEN` | Docker Hub push Token | Secrets Manager 또는 Parameter Store 참조 |

`DOCKERHUB_TOKEN`을 plaintext 환경 변수나 저장소 파일에 넣지 않는다. 현재 권한 범위에서 비밀 저장소를 사용할 수
없다면 인프라 담당자가 안전한 주입 수단을 제공하기 전에는 파이프라인을 활성화하지 않는다.

CodeBuild는 Corretto 25로 `bootJar`를 실행하고 ARM64 이미지를 Docker Hub에 commit SHA와 `dev` 태그로 push한다.
CodeDeploy artifact에는 애플리케이션 소스나 JAR 대신 `appspec.yml`, Compose, Nginx, 스크립트와 확정된 `image.env`만
포함한다.

## 4. CodeDeploy를 구성한다

1. EC2/On-Premises compute platform의 Application을 생성한다.
2. EC2 tag로 개발 서버 한 대를 선택하는 Deployment Group을 생성한다.
3. Deployment Type은 `In-place`, Configuration은 `CodeDeployDefault.AllAtOnce`를 사용한다.
4. 배포 실패 시 자동 rollback을 활성화한다.
5. CodeDeploy Service Role과 EC2 Instance Profile을 연결한다.

EC2 Instance Profile은 CodeDeploy 명령 수신과 Pipeline artifact S3 읽기에 필요한 최소 권한을 가져야 한다.
CodeDeploy Service Role은 대상 EC2 tag와 배포 상태를 관리하는 데 필요한 권한을 가져야 한다.

## 5. CodePipeline V2를 구성한다

단계는 `Source -> Build -> Deploy` 순서로 구성한다.

### Source

- Provider: GitHub via CodeStarSourceConnection
- Repository: `woowacourse-teams/2026-ChongChong`
- Output artifact format: CodePipeline default
- Trigger event: Push
- Include branch: `dev`
- Include file paths:
  - `backend/**`
  - `deploy/**`
  - `buildspec.yml`
  - `appspec.yml`

### Build

- Provider: CodeBuild
- Input: Source artifact
- Output: Build artifact
- Project: 3단계에서 만든 ARM CodeBuild Project

### Deploy

- Provider: CodeDeploy
- Input: Build artifact
- Application과 Deployment Group: 4단계에서 만든 Resource

프론트엔드 파일만 바뀐 `dev` push에서는 Pipeline을 실행하지 않는다. 위 배포 경로 중 하나가 바뀌면 구현 단순성을
위해 JAR과 Docker 이미지를 모두 새로 만든다.

## 6. 최초 배포를 확인한다

Pipeline 성공 후 다음을 확인한다.

```bash
sudo systemctl status codedeploy-agent
sudo docker compose \
  --env-file /opt/chongchong/.env \
  --env-file /opt/chongchong/deploy/image.env \
  --file /opt/chongchong/deploy/docker-compose.yml \
  ps
curl --resolve "${SERVER_NAME}:443:127.0.0.1" "https://${SERVER_NAME}/"
```

배포 스크립트는 backend와 Nginx 컨테이너가 실행되고 Nginx에서 backend 8080 포트로 연결할 수 있는지 확인한다.
새 컨테이너 시작에 실패하면 직전 backend 이미지로 복구를 시도한다. 단일 EC2를 사용하므로 배포 중 짧은 중단은
허용하며, 운영 전 무중단 전환은 별도 결정으로 다룬다.

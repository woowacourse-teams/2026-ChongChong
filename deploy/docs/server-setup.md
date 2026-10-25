# edge와 backend A/B 준비하기

EC2를 만든 뒤 서버 안에 배포 환경을 설치하는 순서다. 운영자는 SSM으로 접속하고, 배포할 때는 edge가 SSH로 A/B에 접속한다.

| 서버 | 설치할 것 |
| --- | --- |
| edge | Docker, CodeDeploy Agent, Nginx, ADOT, 인증서, CloudWatch Agent |
| backend A/B | Docker, 앱 컨테이너, CloudWatch Agent |

새 서버는 1번부터 진행한다. 이미 운영 중인 서버는 필요한 단계만 적용한다. 기존 prod를 B로 사용할 경우에는 B의 앱을 유지한 채 새 edge와 A를 준비한다.

> [!NOTE]
> 아래 순서는 **기존 VPC·RDS에 Ubuntu ARM64 EC2를 추가한 상태**에서 시작한다.
> AWS 콘솔 오른쪽 위 리전은 **아시아 태평양(서울), ap-northeast-2**로 맞춘다.
> 각 단계의 확인 결과가 나오면 다음 단계로 이동한다. 명령은 코드 블록 하나씩 실행한다.

## 1. 서버 주소와 AWS 설정을 확인한다

EC2 콘솔에서 edge·A·B의 **InstanceId와 사설 IP**, edge의 **공인 IP**, **서비스 도메인과 현재 연결 대상**을 기록한다.

### 1-1. 콘솔에서 서버를 구분한다

1. AWS 상단 검색창에서 **EC2**를 열고 왼쪽 **인스턴스**를 선택한다.
2. 서버를 하나씩 선택해 **세부 정보**의 인스턴스 ID와 프라이빗 IPv4 주소를 적는다.
3. **보안** 탭에서 보안 그룹 ID와 IAM 역할을 적는다.
4. **네트워킹** 탭에서 VPC ID·서브넷 ID·퍼블릭 IPv4 주소를 확인한다.

| 역할 | 인스턴스 ID | 사설 IP | 보안 그룹 ID | IAM 역할 |
| --- | --- | --- | --- | --- |
| edge | 직접 입력 | 직접 입력 | 직접 입력 | 직접 입력 |
| backend A | 직접 입력 | 직접 입력 | 직접 입력 | 직접 입력 |
| backend B | 직접 입력 | 직접 입력 | 직접 입력 | 직접 입력 |

이 표의 A/B 사설 IP는 나중에 edge.env에 입력한다. 같은 VPC 안의 서버끼리는 이 주소로 통신한다.

### 1-2. 보안 그룹을 설정한다

**EC2 → 인스턴스 → 보안 → 보안 그룹 링크 → 인바운드 규칙 편집**을 연다.

보안 그룹에서 아래 통신을 허용한다.

| 대상 | 포트 | 출발지 |
| --- | --- | --- |
| edge | TCP 80, 443 | 인터넷 또는 현재 서비스의 원본 접근 정책 |
| backend A/B | TCP 22, 8080 | edge의 보안 그룹 |
| RDS | TCP 5432 | backend A/B의 보안 그룹 |

22번은 SSH 유형, 8080번은 사용자 지정 TCP로 추가한다. A/B 규칙의 소스에는 위에서 기록한 **edge 보안 그룹 ID(sg-...)**를 검색해 선택한다. RDS의 5432 규칙도 같은 방식으로 A/B 보안 그룹을 선택한다.

RDS 보안 그룹은 **RDS → 데이터베이스 → 대상 DB → 연결 및 보안 → VPC 보안 그룹**에서 연다. 저장 후 인바운드 목록의 포트와 소스를 표와 대조한다.

패키지·이미지 다운로드와 AWS 접속을 위해 HTTPS outbound 경로도 필요하다. 사설 서브넷은 NAT 등의 외부 통신 경로를 확인한다.

**VPC → 서브넷 → 서버의 서브넷 → 라우팅 테이블**에서 확인한다. 공용 서브넷은 인터넷 게이트웨이(igw-...)로 향하는 기본 경로와 서버 공인 IP가 필요하고, 사설 서브넷은 NAT 등의 경로를 사용한다. 다운로드에서 timeout이 나면 이 경로와 아웃바운드 규칙을 확인한다.

### 1-3. EC2에 IAM 역할을 연결한다

EC2에 연결한 IAM 역할에는 SSM 접속과 CloudWatch 메트릭 전송 권한이 필요하다. A/B에는 앱의 S3·CloudWatch Logs 권한, edge에는 CodeDeploy artifact를 읽는 S3 권한도 필요하다. 기존 환경을 확장한다면 역할이 같은 서버의 IAM 설정을 기준으로 준비한다.

기존 역할은 **EC2 → 인스턴스 선택 → 작업 → 보안 → IAM 역할 수정**에서 연결한다.

새 역할이 필요하면 **IAM → 역할 → 역할 생성 → AWS 서비스 → EC2**를 선택한다. 공통 권한으로 `AmazonSSMManagedInstanceCore`와 `CloudWatchAgentServerPolicy`를 연결하고 역할을 만든 뒤 EC2에 연결한다. 각각 SSM 연결과 CloudWatch Agent 전송에 쓰는 AWS 관리형 정책이다. [SSM 권한 안내](https://docs.aws.amazon.com/systems-manager/latest/userguide/session-manager-getting-started-instance-profile.html), [CloudWatch 역할 안내](https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/prerequisites.html).

| 추가 권한 | 설정을 찾는 곳 |
| --- | --- |
| A/B의 앱 S3 접근 | 기존 백엔드 EC2의 IAM 역할 정책. backend.env의 AWS_S3_BUCKET·AWS_S3_UPLOAD_PREFIX와 대조 |
| edge의 artifact 읽기 | CodePipeline 설정의 artifact S3 버킷. edge 역할에 해당 artifact의 s3:GetObject·s3:GetObjectVersion 등 읽기 권한 연결 |
| KMS로 암호화한 artifact 해독 | S3 객체의 암호화 키. edge 역할과 키 정책에 kms:Decrypt 허용 |

앱 버킷과 artifact 버킷은 용도가 다르다. 환경별 버킷명과 키는 위 화면에서 확인한 값을 사용한다.

### 1-4. 앱 로그 그룹을 만든다

CloudWatch → 로그 그룹에서 **/chongchong/dev/backend** 또는 **/chongchong/prod/backend**를 만든다. 앱의 awslogs 설정은 이 그룹을 자동 생성하지 않는다.

이미 같은 이름이 있으면 그대로 사용한다. 목록에서 이름과 리전이 맞는지 확인한다. 앱이 시작되면 이 그룹 안에 backend-a·backend-b 로그 스트림이 생긴다.

## 2. 세 서버에 배포 파일을 가져온다

**실행 위치: 각 서버의 SSM 터미널.**

### 2-1. SSM 터미널을 연다

1. **EC2 → 인스턴스**에서 접속할 서버를 선택한다.
2. **연결 → Session Manager → 연결**을 누른다.
3. 터미널이 열리면 `hostname`을 실행해 선택한 서버가 맞는지 확인한다.

연결 버튼이 비활성화되면 인스턴스의 IAM 역할, SSM Agent 상태, AWS로 나가는 HTTPS 경로를 확인한다. AccessDenied가 나오면 콘솔 사용자의 세션 시작 권한을 확인한다. [AWS 접속 안내](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/connect-with-systems-manager-session-manager.html).

### 2-2. 서버 안에서 명령을 실행한다

이 문서의 서버 명령은 root 셸에서 실행한다. SSM 세션을 새로 열면 먼저 실행한다.

~~~bash
sudo -i
whoami
~~~

**확인 결과:** `root`가 나와야 한다. 이후 명령은 이 터미널에서 이어서 실행한다.

> [!TIP]
> `read` 명령이 값을 물으면 입력하고 Enter를 누른다.
> 편집기가 nano라면 Ctrl+O → Enter로 저장하고 Ctrl+X로 닫는다. 편집기를 지정하려면 `EDITOR=nano sudoedit 파일경로`를 사용한다.

OS와 아키텍처를 확인한다.

~~~bash
. /etc/os-release
printf '%s %s\n' "$ID" "$VERSION_ID"
dpkg --print-architecture
~~~

Ubuntu와 arm64가 나오면 다음으로 진행한다. 현재 빌드는 ARM64 이미지를 만든다.

### 2-3. 배포 파일을 복사한다

처음 파일을 가져올 때 실행한다. 브랜치는 적용할 코드가 들어 있는 브랜치를 입력한다. PR 검증 중에는 작업 브랜치, 병합 후에는 환경 브랜치를 사용한다.

~~~bash
apt-get update
apt-get install -y git ca-certificates curl wget python3 tar util-linux coreutils openssh-client
read -r -p '배포 파일을 가져올 브랜치: ' DEPLOY_REF
git clone --depth 1 --branch "$DEPLOY_REF" \
  https://github.com/woowacourse-teams/2026-ChongChong.git /root/chongchong-source
install -d /opt/chongchong/deploy
cp -a /root/chongchong-source/deploy/. /opt/chongchong/deploy/
ls /opt/chongchong/deploy/scripts
~~~

backend, edge, legacy, setup 디렉터리가 보여야 한다. 이후 앱 배포에서는 CodeDeploy가 edge의 배포 파일을 갱신한다.

`destination path ... already exists`가 나오면 이전에 clone한 디렉터리가 있는 상태다. 그 디렉터리의 브랜치와 commit을 먼저 확인한다.

~~~bash
git -C /root/chongchong-source branch --show-current
git -C /root/chongchong-source log -1 --oneline
~~~

## 3. Docker를 설치한다

**실행 위치: Docker가 없는 새 edge·A·B.** 기존 서버에서 아래 두 명령이 실행되면 설치를 건너뛴다.

~~~bash
docker version
docker compose version
~~~

새 서버에서는 다음 순서로 설치한다.

~~~bash
install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
chmod a+r /etc/apt/keyrings/docker.asc
. /etc/os-release
cat > /etc/apt/sources.list.d/docker.sources <<EOF
Types: deb
URIs: https://download.docker.com/linux/ubuntu
Suites: $VERSION_CODENAME
Components: stable
Architectures: $(dpkg --print-architecture)
Signed-By: /etc/apt/keyrings/docker.asc
EOF
apt-get update
apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
systemctl enable --now docker
docker version
docker compose version
~~~

Docker의 Client·Server 버전과 Compose 버전이 출력되면 완료다. 설치 방식은 [Docker Ubuntu 안내](https://docs.docker.com/engine/install/ubuntu/)를 따른다.

## 4. 호스트 메모리 수집을 설정한다

**실행 위치: edge·A·B 각각.**

~~~bash
bash /opt/chongchong/deploy/scripts/setup/setup-host-memory.sh
systemctl is-active amazon-cloudwatch-agent
~~~

active가 나오면 CloudWatch → 지표 → **ChongChong/Host**에서 이 서버의 InstanceId를 선택한다. mem_available, mem_available_percent, mem_total의 최근 값이 들어오는지 확인한다. 수집 간격은 10초다.

설치나 전송이 실패하면 다음 로그를 확인한다.

~~~bash
journalctl -u amazon-cloudwatch-agent --since '10 minutes ago' --no-pager
tail -n 50 /opt/aws/amazon-cloudwatch-agent/logs/amazon-cloudwatch-agent.log
~~~

### 기존 설정이 있어 중단된 경우

B처럼 수동으로 설치했던 서버는 관리 기록이 없어서 중단될 수 있다. 저장소 설정과 현재 설정을 비교한다.

~~~bash
diff -u /opt/chongchong/deploy/config/cloudwatch-agent-memory.json \
  /opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.d/file_cloudwatch-agent-memory.json
~~~

차이가 없고, 에이전트가 실행 중이며 CloudWatch에도 지표가 도착한다면 현재 파일을 관리 기록으로 등록한다.

~~~bash
install -d -m 700 /opt/chongchong/observability-resources
install -m 600 \
  /opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.d/file_cloudwatch-agent-memory.json \
  /opt/chongchong/observability-resources/memory-config.verified
bash /opt/chongchong/deploy/scripts/setup/setup-host-memory.sh
~~~

차이가 있으면 내용을 먼저 확인한다. 다른 지표가 포함돼 있다면 그 설정을 보존한 채 병합해야 한다. 적용 도중 실패해 현재 파일과 관리 기록이 달라진 경우도 이 비교부터 시작한다.

## 5. edge에서 A/B로 SSH 연결을 설정한다

### 5-1. edge에서 배포 키를 만든다

**실행 위치: edge.** 기존 배포 키가 있으면 그 키를 사용한다.

~~~bash
install -d -m 700 /root/.ssh
ssh-keygen -t ed25519 -N '' -f /root/.ssh/chongchong-deploy -C chongchong-deploy
cat /root/.ssh/chongchong-deploy.pub
~~~

출력된 공개키 한 줄을 복사한다.

**확인 결과 예시:** `ssh-ed25519 AAAA... chongchong-deploy`. 다음 단계에는 이 한 줄을 붙여 넣는다.

### 5-2. A/B에 공개키를 등록한다

**실행 위치: A와 B 각각.** 배포 계정은 ubuntu를 사용한다.

~~~bash
install -d -o ubuntu -g ubuntu -m 700 /home/ubuntu/.ssh
touch /home/ubuntu/.ssh/authorized_keys
chown ubuntu:ubuntu /home/ubuntu/.ssh/authorized_keys
chmod 600 /home/ubuntu/.ssh/authorized_keys
read -r -p 'edge 공개키 한 줄: ' DEPLOY_PUBLIC_KEY
printf '%s\n' "$DEPLOY_PUBLIC_KEY" >> /home/ubuntu/.ssh/authorized_keys
sudo -u ubuntu sudo -n true
ssh-keygen -lf /etc/ssh/ssh_host_ed25519_key.pub
~~~

sudo 명령이 아무 출력 없이 끝나면 비밀번호 없이 배포 명령을 실행할 수 있다. 실패하면 ubuntu 계정의 sudo 설정을 확인한다. 현재 배포 스크립트는 파일 설치와 root 권한의 백엔드 스크립트 실행을 요구한다.

마지막 명령의 SHA256 지문은 다음 단계에서 비교한다.

### 5-3. edge 설정을 작성하고 연결을 확인한다

**실행 위치: 새 edge.**

~~~bash
install -d -m 700 /opt/chongchong/config
install -m 600 /opt/chongchong/deploy/config/edge.env.example /opt/chongchong/config/edge.env
sudoedit /opt/chongchong/config/edge.env
~~~

| 변수 | 입력할 값 |
| --- | --- |
| BACKEND_A_HOST, BACKEND_B_HOST | A/B의 사설 IP |
| SSH_USER | ubuntu |
| SSH_KEY | /root/.ssh/chongchong-deploy |
| SSH_KNOWN_HOSTS | /root/.ssh/known_hosts |
| SERVER_NAME | edge 인증서에 사용할 도메인 |
| CLOUD_WATCH_APP_ENV | dev 또는 prod |
| CLOUD_WATCH_AWS_REGION | ap-northeast-2 |
| NGINX_API_LOG_FORMAT | dev는 loadtest, prod는 main |

edge에서 A/B의 호스트 키를 조회한다.

~~~bash
source /opt/chongchong/config/edge.env
ssh-keyscan -t ed25519 "$BACKEND_A_HOST" "$BACKEND_B_HOST" > /root/.ssh/chongchong-hosts.pending
ssh-keygen -lf /root/.ssh/chongchong-hosts.pending
~~~

5-2에서 확인한 주소별 지문과 일치하면 등록하고 연결한다.

~~~bash
cat /root/.ssh/chongchong-hosts.pending >> "$SSH_KNOWN_HOSTS"
chmod 600 "$SSH_KNOWN_HOSTS"
for host in "$BACKEND_A_HOST" "$BACKEND_B_HOST"; do
  ssh -i "$SSH_KEY" -o UserKnownHostsFile="$SSH_KNOWN_HOSTS" \
    -o StrictHostKeyChecking=yes -o BatchMode=yes -o ConnectTimeout=10 \
    "$SSH_USER@$host" 'hostname; sudo -n true'
done
~~~

A/B의 hostname이 각각 나오고 종료 코드가 0이면 완료다. timeout이면 보안 그룹의 22번 포트, Permission denied면 공개키와 계정, sudo 오류면 대상 서버의 sudo 설정을 확인한다.

> [!NOTE]
> `edge.env`를 편집한 뒤에는 `source /opt/chongchong/config/edge.env`로 다시 읽는다.
> 새 SSM 세션에는 이전에 입력한 변수값이 없으므로 주소나 도메인을 사용하는 단계도 이 파일부터 읽는다.

## 6. edge에 앱 설정과 CodeDeploy Agent를 준비한다

**실행 위치: 새 edge.**

~~~bash
install -m 600 /opt/chongchong/deploy/config/.env.example /opt/chongchong/config/backend.env
sudoedit /opt/chongchong/config/backend.env
~~~

Docker Hub 인증 정보, S3, DB, 프론트엔드 URL, 인증·알림 값과 CLOUD_WATCH_APP_ENV를 채운다. 기존 서버를 확장할 때 DB·JWT·카카오·VAPID·클릭 토큰 값은 같은 환경에서 쓰던 값을 옮긴다. 새 임의 값으로 바꾸면 로그인이나 기존 토큰에 영향을 준다.

이 파일은 Bash에서 읽으므로 달러 기호나 공백이 포함된 값은 작은따옴표로 감싼다. 이미지 주소와 태그는 다음 단계의 image.env에서 정한다.

CodeDeploy Agent가 없는 edge에서 실행한다.

~~~bash
curl -fsSL \
  https://aws-codedeploy-ap-northeast-2.s3.ap-northeast-2.amazonaws.com/latestv2/install \
  -o /root/codedeploy-install
chmod 700 /root/codedeploy-install
/root/codedeploy-install auto
systemctl enable --now codedeploy-agent
systemctl is-active codedeploy-agent
~~~

active면 완료다. 설치 오류는 [AWS Ubuntu 설치 안내](https://docs.aws.amazon.com/codedeploy/latest/userguide/codedeploy-agent-operations-install-ubuntu.html)와 journalctl -u codedeploy-agent에서 확인한다.

## 7. 새 backend A에 첫 앱을 실행한다

**실행 위치: edge. 대상은 아직 사용자 요청을 받지 않는 새 A다.**

> [!CAUTION]
> 이 명령은 A의 앱 컨테이너를 교체한다. 현재 사용자 요청을 받고 있는 서버에는 실행하지 않는다.

일상 배포 스크립트는 현재 활성 앱부터 검사한다. 첫 구성에서는 아래 명령으로 A를 먼저 띄운다. 이미지와 태그는 기존 정상 배포의 Docker Hub 이미지 또는 성공한 CodeBuild 결과에서 가져온다.

~~~bash
read -r -p '백엔드 이미지 저장소: ' BACKEND_IMAGE
read -r -p '배포할 commit SHA 태그: ' BACKEND_IMAGE_TAG
printf 'BACKEND_IMAGE=%s\nBACKEND_IMAGE_TAG=%s\n' "$BACKEND_IMAGE" "$BACKEND_IMAGE_TAG" \
  > /opt/chongchong/deploy/image.env
chmod 600 /opt/chongchong/deploy/image.env
~~~

앱 시작 시 DB 마이그레이션과 알림 작업도 실행될 수 있다. 기존 prod와 같은 DB를 사용할 때는 기존 버전과 함께 실행 가능한 이미지를 선택한다.

~~~bash
(
set -euo pipefail
source /opt/chongchong/config/edge.env
seed_dir="$(mktemp -d /root/chongchong-seed.XXXXXX)"
install -m 600 /opt/chongchong/config/backend.env "$seed_dir/backend.env"
install -m 600 /opt/chongchong/deploy/image.env "$seed_dir/image.env"
cp /opt/chongchong/deploy/config/docker-compose{,.backend}.yml "$seed_dir/"
ssh_args=(-i "$SSH_KEY" -o UserKnownHostsFile="$SSH_KNOWN_HOSTS"
  -o StrictHostKeyChecking=yes -o BatchMode=yes -o ConnectTimeout=10)
ssh "${ssh_args[@]}" "$SSH_USER@$BACKEND_A_HOST" \
  'sudo -n install -d -m 700 /opt/chongchong/deploy-control'
ssh "${ssh_args[@]}" "$SSH_USER@$BACKEND_A_HOST" \
  'sudo -n tee /opt/chongchong/deploy-control/backend-release.sh >/dev/null' \
  < /opt/chongchong/deploy/scripts/backend/backend-release.sh
tar -C "$seed_dir" -cf - backend.env image.env docker-compose.yml docker-compose.backend.yml |
  ssh "${ssh_args[@]}" "$SSH_USER@$BACKEND_A_HOST" \
    "sudo -n bash /opt/chongchong/deploy-control/backend-release.sh a $BACKEND_A_HOST"
)
~~~

앱이 시작되면 edge에서 확인한다.

~~~bash
source /opt/chongchong/config/edge.env
curl -fsS --connect-timeout 2 --max-time 5 "http://$BACKEND_A_HOST:8080/actuator/health/readiness"
~~~

status가 UP이면 완료다. 연결되지 않으면 A에서 docker logs --tail 100 chongchong-backend로 DB·설정 오류를 확인한다.

~~~json
{"status":"UP"}
~~~

앱 시작 직후에는 아직 준비 중일 수 있다. A에서 다음을 실행해 컨테이너 상태와 마지막 로그를 확인한 뒤 readiness를 다시 호출한다.

~~~bash
docker ps --filter name=chongchong-backend
docker logs --tail 100 chongchong-backend
~~~

## 8. 새 edge에 인증서와 Nginx를 설치한다

**실행 위치: 새 edge.**

SERVER_NAME 도메인이 새 edge를 가리키고 80번 포트가 비어 있는 경우에는 standalone 방식으로 인증서를 발급한다.

> [!IMPORTANT]
> 아래 인증서 발급 명령은 **도메인이 이미 새 edge를 가리킬 때** 사용한다.
> 기존 prod 도메인으로 서비스 중이면 먼저 아래의 기존 prod 분기를 읽고 발급 방식을 정한다.

~~~bash
source /opt/chongchong/config/edge.env
apt-get install -y certbot
read -r -p '인증서 알림 이메일: ' CERTBOT_EMAIL
certbot certonly --standalone --non-interactive --agree-tos \
  --email "$CERTBOT_EMAIL" -d "$SERVER_NAME"
~~~

**기존 prod 도메인이 아직 기존 서버를 가리킨다면**, 이 단계에서 서비스 DNS를 먼저 바꾸지 않고 DNS 인증 방식으로 인증서를 발급하거나 기존 인증서의 fullchain.pem·privkey.pem과 갱신 구성을 옮긴다. DNS 제공자와 기존 인증서 관리 방식에 따라 명령이 달라진다. 먼저 사용할 도메인과 인증서 발급 방식을 정한다.

인증서가 아래 두 경로에 준비되면 계속 진행한다.

~~~bash
test -s "/etc/letsencrypt/live/$SERVER_NAME/fullchain.pem"
test -s "/etc/letsencrypt/live/$SERVER_NAME/privkey.pem"
openssl x509 -in "/etc/letsencrypt/live/$SERVER_NAME/fullchain.pem" -noout -dates -ext subjectAltName
~~~

**확인 결과:** 인증서 파일 검사에서 오류가 없고, subjectAltName에 SERVER_NAME 도메인이 있으며 notAfter가 미래 시각이어야 한다.

실행 설정과 갱신 작업을 설치한다.

~~~bash
cd /opt/chongchong/deploy
install -d /opt/chongchong/edge-runtime /opt/chongchong/edge-routing /var/www/certbot
install -m 644 config/nginx/edge.conf.template /opt/chongchong/edge-runtime/edge.conf.template
install -m 644 config/nginx/nginx.edge.conf /opt/chongchong/edge-runtime/nginx.conf
install -m 644 config/otel-collector.edge.yaml /opt/chongchong/edge-runtime/otel-collector.yaml
install -m 755 scripts/setup/renew-certificate.sh /usr/local/sbin/chongchong-renew-certificate
install -m 644 config/systemd/chongchong-certbot-renew.{service,timer} /etc/systemd/system/
systemctl daemon-reload
printf 'upstream chongchong_backend { server %s:8080; }\n' "$BACKEND_A_HOST" \
  > /opt/chongchong/edge-routing/upstream.conf
docker compose --env-file /opt/chongchong/config/edge.env \
  -f config/docker-compose.edge.yml --profile metrics config --quiet
docker compose --env-file /opt/chongchong/config/edge.env \
  -f config/docker-compose.edge.yml --profile metrics up -d
curl -fsS --resolve "$SERVER_NAME:443:127.0.0.1" -D - \
  "https://$SERVER_NAME/actuator/health"
~~~

본문의 status가 UP이고 X-Deploy-Backend가 A의 IP:8080이면 edge → A 연결이 완료됐다.
Nginx가 시작되지 않으면 docker logs --tail 100 chongchong-nginx에서 인증서와 마운트 경로 오류를 확인한다.

**응답 예시:** 주소는 실제 A 주소로 표시된다.

~~~text
HTTP/1.1 200 OK
X-Deploy-Backend: 10.0.0.189:8080
...
{"status":"UP"}
~~~

standalone으로 발급한 인증서는 Nginx 시작 후 다음 명령으로 갱신 방식을 webroot로 바꾼다. 인증서 도메인이 edge를 가리키는 상태에서 실행한다.

~~~bash
certbot reconfigure --cert-name "$SERVER_NAME" --webroot --webroot-path /var/www/certbot
systemctl enable --now chongchong-certbot-renew.timer
systemctl list-timers chongchong-certbot-renew.timer
~~~

DNS 인증을 사용한다면 위 webroot 갱신 명령 대신 DNS 제공자에 맞는 자동 갱신 설정을 사용한다. 저장소의 갱신 스크립트는 webroot 방식이다. 발급 방식에 따른 설정은 [Certbot 갱신 안내](https://eff-certbot.readthedocs.io/en/stable/using.html#modifying-the-renewal-configuration-of-existing-certificates)에 있다.

기존 prod에서 전환할 때는 새 edge의 응답을 확인한 뒤 현재 사용 중인 DNS·EIP·CloudFront 원본 연결을 변경한다. 변경 전 연결 대상을 기록하고, 전환 후 서비스 URL에서 health와 주요 API를 확인한다. B의 앱 교체는 기존 서버로 들어오던 요청이 끝난 뒤 진행한다.

## 9. 준비 검사를 실행하고 파이프라인을 연결한다

edge에서 실행한다.

~~~bash
bash /opt/chongchong/deploy/scripts/setup/check-server-setup.sh edge
~~~

A/B에서 각각 실행한다.

~~~bash
bash /opt/chongchong/deploy/scripts/setup/check-server-setup.sh backend
~~~

모두 OK가 나오면 [A/B 배포 절차](edge-deployment.md)로 이동한다. 그 문서에서 CodeBuild의 DEPLOYMENT_MODE와 CodeDeploy 대상 edge를 설정하고 첫 파이프라인을 실행한다.

**확인 결과 예시:**

~~~text
[OK] Ubuntu
[OK] ARM64
[OK] Docker 실행 및 부팅 자동 시작
...
~~~

`[누락/실패]`가 있으면 아래 단계로 돌아가 수정하고 검사 명령을 다시 실행한다.

| 실패한 항목 | 돌아갈 단계 |
| --- | --- |
| Docker·Compose·도구 | 2~3번 |
| CloudWatch Agent·메모리 설정 | 4번 |
| SSH 키·known_hosts·edge.env | 5번 |
| backend.env·CodeDeploy Agent | 6번 |
| 인증서·timer·edge-runtime·upstream | 8번 |

DNS 방식으로 별도 갱신 작업을 구성했다면 검사는 저장소의 webroot timer가 없다고 표시할 수 있다. 이 경우 선택한 DNS 갱신 작업의 실행 상태와 갱신 시험 결과를 확인한다.

새 B의 앱은 첫 A/B 배포에서 실행된다. 기존 서버를 B로 편입할 때도 외부 요청이 새 edge로 옮겨진 뒤 첫 A/B 배포를 실행한다.

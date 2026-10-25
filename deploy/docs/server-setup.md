# edge와 backend A/B를 처음 준비할 때

앱 배포와 서버 준비를 분리한다. `bootstrap-ec2.sh`는 기존 단일 서버용이며 A/B 백엔드에서는 실행하지 않는다.
새 서버는 아래 표를 따라 준비한 뒤 검사한다. 검사 실패는 출력만 남기는 경고가 아니라 종료 코드 1이다.
이 절차는 dev 검증을 위한 것이며 prod의 설치·권한·비용 변경을 자동 승인하지 않는다.

## 서버를 준비하는 순서

먼저 EC2 콘솔에서 해당 환경의 edge·backend A·backend B를 구분하고 사설 IP를 기록한다.
사람은 각 서버에 SSM으로 접속한다. SSM 접속이 된다고 edge에서 A/B로 배포할 수 있는 것은 아니다.
배포에는 별도의 SSH 키, 호스트 키 확인, 대상 계정의 비대화형 sudo가 필요하다.

1. 세 서버의 Ubuntu ARM64·Docker·Compose·필수 도구와 SSM 연결을 준비한다.
2. 아래 호스트 메모리 설정을 각 서버에 적용한다. CloudWatch에서 각 InstanceId의 수신도 확인한다.
3. A/B의 배포 계정과 SSH 공개키를 준비한다. 개인 개발자 키를 복사하거나 호스트 키 검증을 끄지 않는다.
4. edge에 CodeDeploy Agent, 인증서·갱신 작업, 배포 전용 개인키와 known_hosts를 준비한다.
5. edge의 앱 설정 원본과 실행 설정을 설치하고 현재 요청 경로를 확인한다.
6. 역할별 검사와 실제 통신 검증을 마친 뒤 파이프라인을 연결한다.

기존 단일 서버에서 전환하는 경우에는 EIP·DNS 이동과 기존 앱의 B 편입 계획을 먼저 정한다.
최초 Nginx 구성 교체는 컨테이너 재생성을 수반할 수 있다. 준비된 A/B 사이의 일상적인 무중단 배포와
최초 인프라 전환은 같은 작업이 아니다. 기존 요청 경로와 인증서·로그를 보존하고 복구 경로를 준비한다.

## edge에 어떤 파일을 설치하나요?

`/opt/chongchong/config/edge.env`는 `config/edge.env.example`을 바탕으로 환경에 맞게 작성한다.
`config/backend.env`는 `config/.env.example`의 앱·DB·인증·Docker Hub·로그 설정을 참고해 작성한다.
`BACKEND_IMAGE`와 태그는 빌드가 생성한 `image.env`에서 전달한다.
두 원본 파일은 root:600, config 디렉터리는 root:700으로 관리한다. 필수값의 빈칸을 예시 비밀값으로 채우지 않는다.

| 저장소의 원본 | edge에 설치할 위치 |
| --- | --- |
| `config/nginx/edge.conf.template` | `/opt/chongchong/edge-runtime/edge.conf.template` |
| `config/nginx/nginx.edge.conf` | `/opt/chongchong/edge-runtime/nginx.conf` |
| `config/otel-collector.edge.yaml` | `/opt/chongchong/edge-runtime/otel-collector.yaml` |
| `scripts/setup/renew-certificate.sh` | `/usr/local/sbin/chongchong-renew-certificate` |
| `config/systemd/chongchong-certbot-renew.service` | `/etc/systemd/system/chongchong-certbot-renew.service` |
| `config/systemd/chongchong-certbot-renew.timer` | `/etc/systemd/system/chongchong-certbot-renew.timer` |

인증서 갱신 실행 파일에는 실행 권한이 필요하다. unit 설치 후 daemon-reload와 timer 활성화를 수행한다.
인증서 최초 발급은 실제 도메인과 80번 포트의 현재 사용 상태를 확인한 별도 작업이다.

`/opt/chongchong/edge-routing/upstream.conf`에는 현재 정상 응답하는 백엔드를 지정한다.
다음은 A의 사설 IP가 `10.0.0.189`인 **예시**다. 새 환경에 그대로 복사하지 않는다.

```nginx
upstream chongchong_backend { server 10.0.0.189:8080; }
```

실행 파일과 설정을 준비한 뒤 edge에서 Compose를 검사한다.

```bash
sudo docker compose --env-file /opt/chongchong/config/edge.env \
  -f /opt/chongchong/deploy/config/docker-compose.edge.yml --profile metrics config --quiet
```

첫 실행은 기존 컨테이너의 이름과 Compose 프로젝트, 인증서, 복구 경로를 확인한 후 수행한다.
전체 `down`이나 `--remove-orphans`로 기존 서비스를 정리하지 않는다.

```bash
sudo docker compose --env-file /opt/chongchong/config/edge.env \
  -f /opt/chongchong/deploy/config/docker-compose.edge.yml --profile metrics up -d
```

Nginx가 현재 백엔드로 보내는 HTTPS 응답을 확인한 뒤 [배포 운영 절차](edge-deployment.md)의
파이프라인을 연결한다. 새 앱이 없는 빈 A/B 두 대만으로는 기존 활성 서버를 검증하는 배포를 시작할 수 없다.
최초 활성 앱을 먼저 준비하거나 기존 앱을 유지한 상태에서 전환해야 한다.

## 역할별 준비 항목

| 항목 | A/B 백엔드 | edge | 자동 검사 범위 |
| --- | --- | --- | --- |
| Ubuntu ARM64 | 필요 | 필요 | OS·아키텍처 |
| Docker Engine·Compose | 필요 | 필요 | 명령·daemon 접근·실행·자동 시작 |
| curl·Python 3·tar·flock·timeout·SSH·sudo | 필요 | 필요 | 명령 존재 |
| SSM Agent | 필요 | 필요 | deb 또는 snap 서비스 실행·자동 시작 |
| CloudWatch Agent 호스트 메모리 | 필요 | 필요 | 서비스와 적용된 설정 파일 일치 |
| CodeDeploy Agent | 불필요 | 필요 | 실행·자동 시작 |
| 앱 설정 | edge가 release로 전달 | config/backend.env 원본 | edge 파일 존재·root:600 |
| A/B 주소·환경·도메인 | release에서 전달 | config/edge.env | 필수값·A/B 구분·dev/prod·Compose 검증 |
| 배포 SSH 키·known_hosts | 공개키와 비대화형 sudo | 개인키와 호스트 키 | edge 개인키 권한·양쪽 호스트 키 등록 |
| Nginx·ADOT 런타임 설정 | 불필요 | edge-runtime | 파일 존재 |
| TLS 인증서·갱신 | 불필요 | 필요 | 인증서 파일·갱신 실행 파일·timer |
| 현재 라우팅 | 불필요 | edge-routing/upstream.conf | 파일 존재; 실제 응답은 아래 별도 검증 |

```bash
sudo bash deploy/scripts/setup/check-server-setup.sh backend
# edge에서는 다음 역할로 실행한다.
sudo bash deploy/scripts/setup/check-server-setup.sh edge
```

검사는 패키지를 설치하거나 서비스를 시작하지 않는다. 준비 중인 서버에서도 모든 누락을 한 번에 보여준다.
Docker·SSM 등의 설치는 승인된 Ubuntu 이미지와 기존 팀 운영 절차를 따른다. edge의 실행 설정 파일은
위의 ‘edge에 어떤 파일을 설치하나요?’ 표에 맞춰 준비한다. 검사만 통과했다고 배포 가능한 것은 아니다.

## 호스트 메모리 수집 적용

B에서 확인한 설정을 `config/cloudwatch-agent-memory.json`으로 보관한다. `ChongChong/Host`의
`mem_available`, `mem_available_percent`, `mem_total`을 10초마다 수집하며 InstanceId를 자동 식별한다.
리전은 현재 환경과 같은 서울이다. 다른 리전에는 설정 검토 후 적용한다.

새 고해상도 메트릭과 전송 비용, 인스턴스 역할 권한을 확인하고 설치·적용 승인을 받은 뒤 SSM에서 실행한다.
저장소의 deploy 디렉터리가 대상 서버에 있어야 하며 앱 배포 hook에서는 이 명령을 호출하지 않는다.

```bash
sudo bash deploy/scripts/setup/setup-host-memory.sh
sudo bash deploy/scripts/setup/check-server-setup.sh backend
sudo journalctl -u amazon-cloudwatch-agent --since '10 minutes ago' --no-pager
sudo tail -n 50 /opt/aws/amazon-cloudwatch-agent/logs/amazon-cloudwatch-agent.log
```

스크립트는 ARM64 Ubuntu용 AWS 공식 패키지를 에이전트가 없을 때만 설치한다. 기존 패키지는 업그레이드하지
않는다. 적용 파일과 마지막 성공 설정이 같으면 재적용하지 않으며, 서비스만 중지되었으면 시작한다.
설정이 바뀌면 `append-config`로 메모리 조각만 적용한다. 다른 로그 조각은 삭제하지 않는다. 다른 메트릭
조각이 있으면 전역 namespace 변경 영향을 검토하도록 중단한다. 이 경우 수동으로 설정을 병합해야 한다.

`observability-resources/memory-config.verified`는 설정 적용과 서비스 실행 검증이 모두 성공한 뒤에만
갱신한다. 기존 적용 파일이 있으면 이 기록과 내용이 일치할 때만 변경한다. 실행 중인 B처럼 수동 설정만
있는 서버는 자동으로 관리 대상으로 등록하지 않고 중단한다. 기존 설정의 출처와 전체 내용을 검토하고
정상 실행 여부를 확인한 뒤, 승인된 설정을 관리 기록으로 등록해야 한다.
실패 시 에이전트 로그를 확인한다. 적용 파일만 바뀌고 성공 기록이 갱신되지 않았다면 재실행도 중단하므로
두 파일을 대조하고 복구 또는 관리 기록 갱신 여부를 검토한다. 설정 적용 실패의 자동 롤백은 하지 않는다.
에이전트 실행 성공은 CloudWatch 전송 성공을 뜻하지 않는다.

## 서버 외부 의존성과 실제 동작 검증

아래 항목은 로컬 파일 검사로 증명할 수 없다. 서버마다 확인 결과를 남긴다.

- SSM: 새 Session Manager 세션을 열 수 있는지 확인한다. 서비스 실행만으로 IAM과 SSM 연결을 보장하지 않는다.
- IAM: 호스트 메트릭의 `cloudwatch:PutMetricData` 권한을 확인한다. namespace 제한을 쓰면
  `ChongChong/Host`를 포함한다. edge의 ADOT 메트릭 및 백엔드 awslogs 권한도 별도로 유지한다.
- CloudWatch: 해당 서버 InstanceId로 최근 `mem_available`, `mem_available_percent`, `mem_total`이
  실제 도착하는지 확인한다. 프로세스 실행, 빈 로그, 대시보드 범례만으로 성공을 판단하지 않는다.
- 네트워크: edge → A/B SSH·8080, A/B → RDS 5432, AWS·레지스트리 HTTPS outbound를 확인한다.
  8080은 edge에서만 허용하며 키 등록이나 보안 그룹을 검사 스크립트가 자동 변경하지 않는다.
- SSH: edge에서 양쪽에 `BatchMode=yes`, `StrictHostKeyChecking=yes`로 접속하고 `sudo -n true`를 확인한다.
- 앱: 각 A/B의 readiness와 Prometheus 응답, `/chongchong/dev/backend`의 `backend-a`·`backend-b`
  로그, edge ADOT의 양쪽 `@resource.service.instance.id` 시계열 도착을 확인한다.
- 인증서: edge의 갱신 timer 실행 결과와 인증서 유효 기간을 확인한다. 갱신 시험은 별도 승인된 시간에 수행한다.
- 라우팅: upstream의 주소와 실제 HTTPS 응답의 `X-Deploy-Backend`가 일치하는지 확인한다.
- 파이프라인: CodeDeploy 대상이 edge 한 대이고, `DEPLOYMENT_MODE=edge`, 환경별 QUEUED 실행인지 확인한다.

AWS 참고: [여러 설정 파일 적용](https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/create-cloudwatch-agent-configuration-file.html),
[설정 파일과 적용 방식](https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/CloudWatch-Agent-Configuration-File-Details.html).

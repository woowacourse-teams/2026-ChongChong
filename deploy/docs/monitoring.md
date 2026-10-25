# 모니터링 확인하기

앱 지표는 edge의 ADOT가 A/B 양쪽에서 수집하고, EC2 메모리는 각 서버의 CloudWatch Agent가 전송한다.

~~~mermaid
flowchart LR
    A["backend A /actuator/prometheus"] --> Collector[edge ADOT]
    B["backend B /actuator/prometheus"] --> Collector
    Collector --> CW[CloudWatch]
    Host["각 EC2의 CloudWatch Agent"] -->|호스트 메모리| CW
    Logs["A/B Docker awslogs"] -->|앱 로그| CW
~~~

## 1. 현재 요청을 받는 서버를 확인한다

**실행 위치: edge의 SSM 터미널.** 이 문서의 서버 명령은 root 셸에서 실행한다.

~~~bash
sudo -i
source /opt/chongchong/config/edge.env
curl -fsS --connect-timeout 2 --max-time 5 \
  --resolve "$SERVER_NAME:443:127.0.0.1" -D - \
  "https://$SERVER_NAME/actuator/health"
~~~

status가 UP인지 확인하고 X-Deploy-Backend에 나온 IP를 A/B 주소와 비교한다.

~~~bash
grep -E '^(BACKEND_A_HOST|BACKEND_B_HOST)=' /opt/chongchong/config/edge.env
~~~

## 2. 앱 지표가 양쪽에서 들어오는지 확인한다

**실행 위치: CloudWatch Query Studio.** dev에서는 아래 쿼리를 실행한다. prod를 확인할 때는 environment를 prod로 바꾼다.

AWS 콘솔에서 **CloudWatch**를 검색하고 왼쪽 메뉴의 **Query Studio**를 연다. PromQL 조회 화면에서 쿼리를 입력하고 시간 범위를 최근 15분으로 설정한 뒤 실행한다.

~~~promql
process_uptime_seconds{environment="dev",service="chongchong-backend"}
~~~

원본 결과의 **@resource.service.instance.id**에서 A/B의 IP:8080을 각각 찾는다. 최근 시각까지 값이 들어오면 양쪽 수집이 정상이다.

> [!NOTE]
> 앱을 아직 띄우지 않은 새 B는 결과에 나오지 않는다. 첫 A/B 배포로 B가 실행된 뒤 다시 조회한다.

한쪽이 없으면 edge에서 해당 앱의 Prometheus 응답을 확인한다.

~~~bash
source /opt/chongchong/config/edge.env
for host in "$BACKEND_A_HOST" "$BACKEND_B_HOST"; do
  printf '\n%s\n' "$host"
  curl -fsS --connect-timeout 2 --max-time 10 \
    "http://$host:8080/actuator/prometheus" | grep '^process_uptime_seconds'
done
~~~

연결 오류면 백엔드 실행 상태와 8080 보안 그룹을 확인한다. 앱의 응답은 있는데 CloudWatch 값이 없으면 수집기 로그를 확인한다.

~~~bash
docker ps --filter name=chongchong-metrics-collector
docker logs --since 10m chongchong-metrics-collector
~~~

자격 증명·AccessDenied 오류는 edge IAM 역할, timeout은 AWS outbound 연결, 수집 대상 오류는 A/B 주소를 확인한다. 기본 수집 간격은 60초이고 첫 누적값은 기준점으로 사용하므로, 수집기 시작 후 최소 두 주기를 기다려 조회한다.

## 3. EC2 메모리를 확인한다

**실행 위치: CloudWatch → 지표 → ChongChong/Host.**

A와 B의 InstanceId를 각각 선택하고 mem_available, mem_available_percent, mem_total의 최근 값을 확인한다. 앱의 JVM 메모리와 EC2 전체 메모리는 서로 다른 지표다.

**모든 지표 → 사용자 지정 네임스페이스 → ChongChong/Host → InstanceId가 포함된 차원**을 선택한다. EC2 콘솔에서 기록한 인스턴스 ID로 검색하고 지표 체크박스를 선택한다. 그래프의 마지막 점에 마우스를 올려 최근 시각의 값인지 확인한다.

값이 없는 서버의 SSM 터미널에서 실행한다.

~~~bash
sudo systemctl status amazon-cloudwatch-agent --no-pager
sudo tail -n 50 /opt/aws/amazon-cloudwatch-agent/logs/amazon-cloudwatch-agent.log
~~~

서비스가 없으면 [서버 준비](server-setup.md)의 4번을 실행한다. 서비스는 실행 중인데 AccessDenied가 나오면 EC2 역할의 cloudwatch:PutMetricData 권한을 확인한다.

## 4. 대시보드와 앱 로그를 확인한다

| 지표 | 확인할 대상 |
| --- | --- |
| 요청량·오류율·p95 | 서비스 전체. A/B별 요청량으로 트래픽 이동도 확인 |
| JVM·앱 CPU·Hikari | A/B 각각 |
| EC2 CPU·메모리 | 각 InstanceId |
| RDS | 공유 DB 전체 |
| 알림 큐 | A/B가 각각 관측한 값. 같은 큐를 읽는 값은 합산하지 않음 |

CloudWatch Logs에서 /chongchong/dev/backend 또는 /chongchong/prod/backend를 열고 backend-a·backend-b 스트림을 선택한다.

서버 터미널에서는 다음 명령으로 최근 로그를 본다.

~~~bash
sudo docker logs --since 10m chongchong-backend
~~~

edge의 Nginx 로그는 edge에서 확인한다.

~~~bash
docker logs --since 10m chongchong-nginx
~~~

## 앱 수집 설정을 바꿀 때

**실행 위치: edge.** 간격은 config/edge.env, 수집 지표 종류는 edge-runtime/otel-collector.yaml에서 바꾼다.

~~~bash
backup_dir="$(mktemp -d /root/chongchong-metrics-backup.XXXXXX)"
cp -p /opt/chongchong/config/edge.env "$backup_dir/"
cp -p /opt/chongchong/edge-runtime/otel-collector.yaml "$backup_dir/"
printf '백업 경로: %s\n' "$backup_dir"
sudoedit /opt/chongchong/config/edge.env
sudoedit /opt/chongchong/edge-runtime/otel-collector.yaml
~~~

저장소의 수집 설정을 적용하려는 경우에는 직접 편집하는 대신 deploy/config/otel-collector.edge.yaml을 edge-runtime/otel-collector.yaml로 복사한다. 이후 수집기만 재생성한다.

~~~bash
docker compose --env-file /opt/chongchong/config/edge.env \
  -f /opt/chongchong/deploy/config/docker-compose.edge.yml --profile metrics config --quiet
docker compose --env-file /opt/chongchong/config/edge.env \
  -f /opt/chongchong/deploy/config/docker-compose.edge.yml \
  --profile metrics up -d --no-deps --force-recreate metrics-collector
docker logs --since 2m chongchong-metrics-collector
~~~

2번 쿼리로 A/B 지표가 다시 들어오는지 확인한다. 실패하면 출력해 둔 백업 경로에서 두 파일을 원래 위치로 복원하고 같은 재생성 명령을 실행한다.

## 부하 테스트에서는 이렇게 본다

1. 1번 명령으로 활성 백엔드를 기록한다.
2. 서비스 URL에 부하를 보내고, 부하 도구의 처리량·오류·응답 시간과 활성 서버의 CPU·JVM·Hikari를 같은 시간대로 비교한다.
3. 대기 서버도 DB 연결과 알림 작업을 유지할 수 있으므로 RDS 전체 연결과 부하를 함께 본다.
4. 배포 전환 시험에서는 부하를 유지하며 배포하고 전환 시각, A/B 요청 이동, 긴 요청의 완료 여부를 기록한다.

성능 기준을 측정하는 시험에서는 배포를 함께 진행하지 않는다. 전환 시험과 구분해야 버전 교체의 영향을 분리해 볼 수 있다.

60초 수집은 수초짜리 부하의 최고점을 놓칠 수 있다. 짧은 dev 실험에서는 METRICS_SCRAPE_INTERVAL과 METRICS_SCRAPE_TIMEOUT을 함께 줄여 비교한다. timeout은 interval 이하로 설정한다. 시험 후에는 기본값 60s·10s로 되돌리고 수집기를 재생성한다. 간격을 줄이면 전송량과 수집기 자원 사용량도 늘어난다.

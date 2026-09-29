# 0044. ADOT으로 HTTP 메트릭을 수집해 CloudWatch에 저장한다

- 날짜: 2026-09-29
- 관련 이슈: [#408](https://github.com/woowacourse-teams/2026-ChongChong/issues/408)
- 관련 ADR: [0042. 백엔드 HTTP 메트릭을 Prometheus 형식으로 노출한다](0042-expose-backend-http-metrics-for-prometheus.md)
- 대체 대상: [0043. ADOT으로 HTTP 메트릭을 수집해 AMP에 저장한다](0043-collect-http-metrics-with-adot-and-amp.md)

## 배경

ADR-0042에 따라 백엔드는 `/actuator/prometheus`로 요청 수와 응답 시간 히스토그램을 노출한다. ADR-0043은 각 EC2의
ADOT 수집기가 이 출력을 읽어 Amazon Managed Service for Prometheus(AMP)에 전송하도록 결정했다. 그러나 팀에
제공된 우테코 AWS 계정의 사용 가능 서비스 목록에는 AMP가 없다. AMP workspace를 만들 수 없으므로 현재의
`prometheusremotewrite` 전송 설정은 활성화할 수 없다. CloudWatch는 허용 서비스이며 피즈의 로그 수집에도 사용한다.

2026-09-29 dev EC2에서 Instance Profile의 `ec2-project` 역할로 CloudWatch `PutMetricData` 호출이 `exit=0`으로
끝났다. 이는 해당 역할의 **기본 메트릭 쓰기 권한**을 확인한 결과다. ADOT 컨테이너에서의 자격 증명 접근과 OTLP
엔드포인트 전송 성공까지 증명한 것은 아니다. 운영 서버의 역할과 권한도 아직 별도로 확인하지 않았다.

dev는 `t4g.micro`(RAM 약 1GiB, 스왑 2GiB)로 측정 당시 가용 메모리가 약 209MiB였고, prod는 `t4g.small`
(RAM 약 2GiB)이다. 전체 Prometheus 저장소를 두 서버에 운영하지 않는다는 ADR-0043의 자원 판단은 유지한다.

## 결정

각 환경에서 기존 Docker Compose의 ADOT 수집기 한 개를 `metrics` profile로 선택적으로 실행한다. 수집기는
사설 Docker 네트워크에서 60초마다 `backend:8080/actuator/prometheus`를 읽고, Prometheus 형식의 HTTP 요청
메트릭을 OpenTelemetry 메트릭으로 변환해 해당 환경의 CloudWatch OTLP 메트릭 엔드포인트에 HTTPS로 전송한다.
전송은 SigV4로 서명하며 서비스 이름은 `monitoring`, 대상은
`https://monitoring.<리전>.amazonaws.com/v1/metrics`로 설정한다. 메트릭 리전은 기존
`CLOUD_WATCH_AWS_REGION`을 사용한다. 백엔드 계측과 CloudWatch Logs 경로는 변경하지 않는다.

- 수집기는 HTTP 요청 수·응답 시간 히스토그램과 수집 성공 여부를 판단하는 지표만 전송한다. Actuator의 JVM 등
  이번 이슈에 필요 없는 메트릭은 수집 단계에서 제외해 전송량을 제한한다. 실제 전송되는 시계열 수를 확인하고
  필요한 지표가 누락되면 필터를 조정한다.
- `service=chongchong-backend`와 `environment=dev|prod`를 고정된 속성으로 부여한다. 요청별 URI, 사용자 ID,
  이메일, 토큰, `request_id`를 메트릭 라벨에 넣지 않는다. 수집기에서 변환된 실제 메트릭 이름과 속성 이름은
  CloudWatch 조회 결과로 확인한 뒤 대시보드·알림 담당자와 공유한다.
- EC2 Instance Profile의 임시 자격 증명을 사용하며 정적 AWS 키를 배포 파일에 두지 않는다. 필요한 쓰기 작업은
  `cloudwatch:PutMetricData`다. dev에서 기본 API 호출은 성공했지만, 수집기 내부 IMDSv2 접근과 OTLP 전송은
  별도로 검증한다. 컨테이너가 역할 자격 증명을 읽지 못하면 IMDSv2 hop limit과 접근 범위를 확인한다.
- 메트릭용 포트를 호스트나 인터넷에 공개하지 않는다. 수집기에서 CloudWatch로 나가는 HTTPS 443만 사용한다.
- 기존의 192MB 컨테이너 메모리 상한과 로그 회전을 유지한다. dev에서는 profile을 상시 활성화하기 전에 단기
  시험으로 `MemAvailable`, 스왑 입출력, OOM, 백엔드 응답과 전송 상태를 확인한다. 자원 압박이 발생하면
  수집기를 중지하고 인스턴스 증설 또는 수집 위치 변경을 다시 결정한다. prod도 활성화 전후 자원을 확인한다.
- CloudWatch의 OpenTelemetry 메트릭을 PromQL로 조회한다. Grafana 화면, 알림 규칙 및 조회 권한은 바니의
  작업 범위다. 실제 메트릭 이름과 쿼리는 수집 검증 후 확정한다.

## 선택 이유

CloudWatch는 허용된 AWS 서비스이며, 서울 리전에서 OTLP 메트릭 수집과 PromQL 조회를 지원한다. 기존 백엔드의
Prometheus 출력과 EC2 내부 ADOT 수집기를 유지한 채 전송 대상만 바꿀 수 있다. CloudWatch가 시계열 저장을
담당하므로 작은 dev 서버에 Prometheus TSDB·WAL·백업 운영을 추가하지 않는다. dev·prod는 각자의 계정·리전으로
전송할 수 있어 환경 간 사설 네트워크 연결도 필요하지 않다.

CloudWatch의 OpenTelemetry 메트릭은 수집 데이터량에 따라 과금되며 보존 기간 15개월이 포함된다. 따라서
ADR-0043의 AMP workspace별 30일 보존 설정과 샘플·저장량 기준 비용 가정은 적용하지 않는다. 60초 수집
간격과 필요한 HTTP 지표만 전송하는 필터로 데이터량을 제한하고, 실제 수집량과 비용을 확인한다.

## 검토한 대안

### AMP를 계속 사용한다

Prometheus `remote_write`와 전용 workspace를 제공하지만 우테코 AWS의 사용 가능 서비스가 아니므로
채택할 수 없다.

### CloudWatch Classic 사용자 지정 메트릭으로 변환한다

`PutMetricData` 기본 API가 dev에서 동작하는 것은 확인했다. 그러나 Prometheus 히스토그램과 라벨을 Classic
메트릭의 차원·통계로 변환하는 별도 설계가 필요하고, 고유 메트릭 수에 따른 과금이 발생한다. 현재 필요한
PromQL 기반 요청량·오류율·p95 조회에는 CloudWatch OTLP 경로가 더 직접적이다.

### 별도 EC2에 Prometheus를 운영한다

AMP와 CloudWatch의 제약을 피할 수 있지만 별도 EC2 비용, 사설 연결, 시계열 저장소의 보존·백업·업데이트·
장애 복구 운영이 추가된다. 현재 규모와 운영 여력에서는 선택하지 않는다.

## 영향

### 긍정적 영향

- 백엔드 계측과 로그 수집 경로를 유지하면서 허용된 서비스에 메트릭을 저장할 수 있다.
- 환경별 HTTP 요청량, 5xx 오류율과 p95 응답 시간을 PromQL로 조회할 수 있는 경로를 마련한다.
- EC2에 시계열 저장소를 두지 않아 디스크 보존과 백업을 직접 운영하지 않는다.

### 부정적 영향과 위험

- dev의 적은 메모리에 ADOT 컨테이너가 추가된다. 192MB 상한은 안전한 여유를 보장하지 않는다.
- OTLP 전송 실패나 수집기 중단 중에는 메트릭 공백이 생길 수 있다. 백엔드 요청 처리는 수집기와 독립적이다.
- Prometheus 형식에서 OTLP로 변환되며 메트릭 이름과 속성 표현이 달라질 수 있어 실제 조회 결과로 쿼리를
  확정해야 한다.
- CloudWatch 수집 데이터량과 API 조회량에 따른 비용이 발생하고, 기존 AMP 설정과 배포 안내를 교체해야 한다.
- dev의 Classic `PutMetricData` 호출 성공만으로 OTLP 전송, prod 권한 또는 Grafana 조회 권한을 보장하지 않는다.

## 미확정 사항

- 실제 OTLP 전송 후 CloudWatch에서 확인되는 HTTP 메트릭 이름, 환경 속성, 히스토그램 형태와 PromQL.
- dev·prod의 수집량 및 비용, 수집기 가동 전후 자원 사용량과 prod 역할의 권한.
- Grafana의 CloudWatch OpenTelemetry 메트릭 조회 방식과 알림 조건·수신 채널. 이는 바니의 범위다.

## 후속 작업

- 수집기 exporter를 AMP `remote_write`에서 CloudWatch OTLP HTTP로 변경하고, Compose 환경 변수 검증과
  배포 문서에서 AMP workspace·권한·보존 설정을 제거한다. 수집기 이미지를 변경한다면 ARM64 지원과 구성
  요소의 실제 포함 여부를 검증한다.
- 로컬에서 수집기 구성과 백엔드 Prometheus 출력, 비공개 접근 경로를 확인한다.
- dev에서 수집기를 단기 실행해 자격 증명, 전송 오류, 실제 요청 전후 메트릭 증가, 5xx·p95 조회 및 자원
  상태를 확인한다. 안전한 경우에만 상시 profile을 활성화한다.
- 검증한 구성을 prod에 적용하기 전 권한과 자원을 확인하고, 환경별 전송과 조회를 재검증한다.
- 확인된 메트릭 이름·속성·PromQL과 검증 방법을 바니에게 전달한다.

## 참고 자료

- [AWS: CloudWatch OpenTelemetry 메트릭 출시와 지원 리전](https://aws.amazon.com/about-aws/whats-new/2026/06/amazon-cloudwatch-otel-metrics/)
- [AWS: CloudWatch OTLP 엔드포인트](https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/CloudWatch-OTLPEndpoint.html)
- [AWS: CloudWatch agent의 OTLP HTTP·SigV4 설정 예시](https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/CloudWatch-OTLPCloudWatchAgent.html)
- [AWS: OpenTelemetry 메트릭 전송 권한](https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/metrics-otel-send.html)
- [AWS: OpenTelemetry 메트릭 비용과 보존](https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/metrics-otel-pricing.html)

# 0043. ADOT으로 HTTP 메트릭을 수집해 AMP에 저장한다

- 날짜: 2026-09-29
- 관련 이슈: [#408](https://github.com/woowacourse-teams/2026-ChongChong/issues/408)
- 관련 ADR: [0042. 백엔드 HTTP 메트릭을 Prometheus 형식으로 노출한다](0042-expose-backend-http-metrics-for-prometheus.md)

## 배경

ADR-0042는 백엔드의 `/actuator/prometheus` 출력을 결정했지만 실제 수집기와 저장소는 정하지 않았다. 배포 시점의
운영 서버는 `t4g.small`(RAM 2GiB), 개발 서버는 `t4g.micro`(RAM 1GiB, 스왑 2GiB)이며 모두 ARM64다.
이는 ADR-0016·0017에 기록된 당시의 예정 사양과 다르다. 2026-09-29 측정에서 운영 서버는 가용 메모리 766MiB,
개발 서버는 209MiB였다. 개발 서버는 스왑 337MiB를 사용 중이고 백엔드 컨테이너는 약 305MiB를 사용했다.
운영 서버의 루트 파일시스템은 측정 후 사용자가 확장했다고 알려왔으며, 확장 후 여유 용량은 배포 전에 다시 확인한다.
두 환경의 사설 네트워크 연결도 확인되지 않았다. 따라서 기존 서버에 전체 Prometheus 저장소를 함께 운영하거나
환경 간 메트릭 포트를 공개하는 설계는 채택하지 않는다.

## 결정

각 환경의 백엔드와 같은 Docker Compose 네트워크에서 AWS Distro for OpenTelemetry(ADOT) 수집기 **한 개**를 실행한다.
수집기는 60초마다 `backend:8080/actuator/prometheus`를 사설 네트워크에서 읽고, SigV4로 서명한 HTTPS
`remote_write`로 해당 환경 계정·리전의 Amazon Managed Service for Prometheus(AMP) workspace에 전송한다.
`dev`와 `prod` workspace 및 EC2 Instance Profile의 쓰기 권한은 분리한다. 수집기의 정적 라벨은
`job=chongchong-backend`, `service=chongchong-backend`, `environment=dev|prod`로 고정한다.

- 수집기 이미지는 ARM64와 x86_64를 지원하는 버전 태그로 고정한다. Compose의 `metrics` profile로 선택적으로
  실행하고, AMP workspace·권한·서버 여유 자원 확인 전에는 활성화하지 않는다.
- AMP workspace마다 보존 기간을 **30일**로 설정한다. 백업과 시계열 디스크 운영은 AMP에 맡기며, 로컬 수집기의
  전송 실패 중 메모리 큐를 초과하거나 재시작하면 일부 샘플이 유실될 수 있음을 받아들인다.
- 수집기에는 192MB 컨테이너 메모리 상한과 로그 회전을 설정한다. 이 상한은 실제 사용량이나 안전성을 보장하는
  수치가 아니다. dev의 209MiB 가용 메모리에서는 상한까지 사용하면 여유가 거의 없으므로, dev에 바로 상시
  활성화하지 않는다. 짧은 통제 시험에서 백엔드 응답, `MemAvailable`, 스왑 입출력, OOM 및 수집기 상태를
  관찰한다. 자원 압박이 확인되면 profile을 끄고 dev를 증설하거나 수집 위치를 재결정한 뒤 다시 검증한다.
- prod도 시작 전 `free -h`, `df -h /`, `docker stats --no-stream`으로 확장 후 디스크와 현재 자원을 재확인하고,
  가동 후 백엔드·수집기의 메모리 사용 및 OOM 여부를 관찰한다.
- EC2 Instance Profile에 해당 AMP workspace의 `aps:RemoteWrite`만 추가한다. 정적 AWS access key를 파일이나
  환경 변수에 두지 않는다. 컨테이너가 IMDSv2 자격 증명을 읽을 수 있는지 확인한다. 필요할 경우 EC2의 IMDSv2
  응답 hop limit을 2로 설정하되, 이 변경으로 같은 호스트의 다른 컨테이너에서도 Instance Profile 접근이 가능해질
  수 있으므로 권한을 workspace에 한정하고 적용 전 접근 범위를 검토한다.
- 백엔드 메트릭 포트는 호스트 또는 인터넷에 publish하지 않는다. AMP로 나가는 HTTPS 443만 사용한다. Grafana가
  AMP를 조회하는 권한과 데이터 소스는 바니의 범위이며, `aps:QueryMetrics` 읽기 권한을 별도로 부여한다.

## 선택 이유

prod는 2GiB RAM이지만 기존 백엔드와 배포 작업이 함께 실행되고, dev는 1GiB RAM에서 이미 스왑을 사용한다.
전체 Prometheus 서버의 TSDB·WAL·디스크 보존을 각 서버에서 운영하는 부담을 피하기 위해 ADOT은 같은 Docker
네트워크에서 비공개 endpoint만 수집하고 AMP가 시계열을 저장하게 한다. 환경별 workspace를 같은 계정에 강제로
모으지 않아 dev·prod 계정이 분리되어 있어도 동작한다. 기존 CloudWatch Logs의 로그 수집과 백엔드 코드는
변경하지 않는다.

비용은 AMP의 **수집 샘플 수 + 저장량 + 조회 샘플 수**로 발생한다. 이 방식에는 AMP 관리형 수집기의 시간당 요금이
붙지 않는다. 한 환경에서 활성 시계열이 200개이고 60초 간격으로 30일간 수집하면 약 864만 샘플이며,
두 환경이면 약 1,728만 샘플이다. 이는 **가정에 따른 사용량 예시이지 견적이 아니다**. 실제 시계열 수와 리전별
단가, 무료 사용량 적용 여부, Grafana 조회량을 확인해 비용을 다시 계산한다. 수집기의 EC2 자원 사용량과 통신
비용도 별도 관찰한다.

## 검토한 대안

### 기존 EC2에 전체 Prometheus 서버를 둔다

추가 관리형 저장 비용을 줄일 수 있다. 그러나 dev의 가용 메모리가 209MiB이고 스왑을 사용 중이며, prod에서도
백엔드와 저장소가 자원을 경쟁한다. TSDB 보존·백업·업데이트·장애 복구까지 직접 관리해야 하므로 선택하지 않는다.

### 환경별 별도 EC2에 Prometheus를 둔다

백엔드와 자원은 분리되지만 EC2 고정 비용과 저장소 운영이 추가된다. 환경별 사설 접근 경로도 만들어야 한다.

### AMP 관리형 수집기를 사용한다

관리 부담이 적지만 현재 AWS 문서의 관리형 수집기는 EKS 클러스터용이며, 수집기 시간당 요금이 발생한다. 단일 EC2
Docker Compose 배포에는 ADOT이 더 직접적이다.

## 영향과 검증

- AMP 또는 인터넷 연결이 중단되면 메트릭에 공백이 생길 수 있다. 백엔드 요청 처리는 수집기와 독립적이다.
- 수집기 profile이 활성화되면 배포 시 backend와 함께 재생성될 수 있다. 배포 후 수집기 상태와 AMP의 `up` 및
  `http_server_requests_seconds_count`를 확인한다.
- 먼저 dev에서 수집기 단기 시험 중 메모리·스왑 입출력·OOM·백엔드 상태를 확인한다. 안전 여유가 부족하면 dev
  인스턴스 증설 또는 수집 위치 재결정 후 다시 검증한다. 이어 실제 요청 전후 시계열 증가·환경 라벨·공개 endpoint
  차단을 확인하고 prod에 같은 구성을 적용한다. 의도적인 5xx 실패는 로컬 또는 통제된 dev에서만 검증한다.
- 이 ADR은 실제 workspace 생성, IAM 연결 또는 dev·prod 배포 완료를 뜻하지 않는다. 각 환경의 활성화 직전
  메모리·디스크, 수집기 가동 전후 자원 사용량, AMP workspace ID·리전·30일 보존 설정, 권한·IMDS 상태와 비용을
  배포 기록에 남긴다.

## 참고 자료

- [AWS: AMP customer managed collectors](https://docs.aws.amazon.com/prometheus/latest/userguide/self-managed-collectors.html)
- [AWS: AMP pricing](https://aws.amazon.com/prometheus/pricing/)
- [AWS: AMP ingestion and retention](https://docs.aws.amazon.com/prometheus/latest/userguide/AMP-ingest-methods.html)
- [AWS: EC2 IMDSv2 in containers](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/configuring-IMDS-new-instances.html)
- [ADOT: Prometheus remote write exporter](https://aws-otel.github.io/docs/getting-started/prometheus-remote-write-exporter/)

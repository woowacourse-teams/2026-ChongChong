# 0041. 백엔드 구조화 로그를 CloudWatch Logs로 수집한다

- 날짜: 2026-09-29
- 관련 이슈: [#401](https://github.com/woowacourse-teams/2026-ChongChong/issues/401)

## 배경

스프린트 1에서 운영 중인 요청과 Web Push 전달 결과를 검색하고 원인을 추적할 수 있는 로그 관찰 체계가 필요하다.
기존 로그는 HTTP 요청과 백그라운드 Web Push 작업이 서로 다른 형식과 기준으로 기록되어 요청 하나의 흐름과 전달 결과를
함께 추적하기 어려웠다. 개발 환경과 배포 환경을 구분할 필드도 필요했고, Web Push provider의 응답 본문처럼 민감할 수
있는 값이 로그에 남지 않도록 기준을 정해야 했다.

현재 백엔드는 AWS 관리형 서비스 기반의 CI/CD와 EC2 Docker Compose 배포를 사용한다. 아직 트래픽이 많지 않은 단계에서
별도의 로그 수집·저장·검색 서버를 직접 운영하면 운영 관찰을 시작하기 전에 설치, 저장소, 백업, 업데이트와 장애 복구를
추가로 관리해야 한다. 따라서 애플리케이션 로그의 형식과 보존 위치를 먼저 표준화하고, 현재 배포 환경에서 바로 검색할 수
있는 수집 방식을 선택한다.

## 결정

백엔드 애플리케이션 로그를 한 줄 구조화 JSON으로 출력하고, 배포 환경에서는 Docker의 `awslogs` 로깅 드라이버를 통해
CloudWatch Logs로 수집한다. CloudWatch Logs Insights를 로그 검색과 집계 도구로 사용한다.

- HTTP 요청마다 요청 식별자를 설정하고 응답 헤더와 MDC에 함께 저장한다. 요청 헤더의 값이 허용된 형식이 아니면 새
  식별자를 생성한다.
- HTTP 요청 로그에는 `timestamp`, `level`, `service`, `environment`, `request_id`, `method`, `route`, `status`,
  `duration_ms`, `error_code`, `message`를 사용한다.
- 매칭된 Spring 경로 패턴을 `route`로 기록한다. 매칭되지 않은 요청은 원본 URI를 기록하지 않고
  `UNMATCHED_ROUTE`로 기록하여 URI에 포함될 수 있는 토큰이나 개인정보를 보호한다.
- 예상 가능한 HTTP 응답은 `INFO`, CSRF 거부와 rate limit은 `WARN`, 서버 오류는 `ERROR`로 기록한다.
- Web Push 전달 로그는 `NotificationDeliveryLogger`로 모은다. 구독 만료(404·410)는 `INFO`, 재시도가 예약된
  429·5xx는 `WARN`, 재시도 소진과 영구 실패는 `ERROR`로 기록한다.
- Web Push provider의 응답 본문과 민감할 수 있는 응답 값은 기록하지 않는다.
- `CLOUD_WATCH_APP_ENV`로 `dev`와 `prod`를 구분하고, CloudWatch 로그 그룹은 `/chongchong/{environment}/backend` 형식으로
  분리한다.
- CloudWatch Logs 리전은 EC2·CodeDeploy 리전과 다를 수 있으므로 `AWS_REGION`과
  `CLOUD_WATCH_AWS_REGION`을 별도 설정값으로 관리한다.
- 로그 그룹은 배포 전에 생성하고, Docker Compose에서는 `awslogs-create-group=false`로 설정한다. EC2 Instance
  Profile에는 해당 로그 그룹에 대한 로그 스트림 생성과 로그 이벤트 기록 권한을 부여한다.
- 메트릭 수집, 대시보드와 알림 채널은 이 ADR의 결정 범위에 포함하지 않는다. 로그 필드를 기반으로 한 CloudWatch
  대시보드와 알림은 후속 운영 설정에서 결정한다.

## 선택 이유

CloudWatch Logs는 현재 배포가 이미 AWS 관리형 서비스와 EC2를 중심으로 구성되어 있어 별도의 수집기와 저장 서버를
추가하지 않고 적용할 수 있다. 애플리케이션은 표준 출력으로 JSON 로그를 출력하고 Docker 로깅 드라이버가 수집을
담당하므로 애플리케이션 코드가 CloudWatch SDK나 AWS 자격 증명에 직접 의존하지 않는다.

개발과 운영을 로그 그룹과 `environment` 필드로 분리하면 같은 로그 형식을 유지하면서도 환경별 조회가 가능하다. JSON
필드는 Logs Insights에서 `request_id`, `route`, `status`, `duration_ms`, `error_code`로 바로 검색·집계할 수 있어 현재
필요한 장애 원인 추적과 성능 확인을 빠르게 시작할 수 있다.

또한 로그 저장소, 접근 권한과 수집 경로를 AWS에서 함께 관리하므로 팀이 별도 서버의 디스크, 백업, 업데이트와 장애
복구를 운영하지 않아도 된다. 초기 트래픽과 팀의 운영 여력을 고려하면 직접 관찰 스택을 구성하는 것보다 현재 배포
구조에 맞는 관리형 서비스를 먼저 사용하는 편이 적합하다.

## 검토한 대안

### Loki + Prometheus + Grafana

Loki의 LogQL과 Prometheus의 PromQL을 사용하면 로그와 메트릭을 세밀하게 조합하고 Grafana에서 통합 대시보드와 알림을
구성할 수 있다. AWS 외 환경으로 확장할 때도 선택의 폭이 넓다.

그러나 Loki, Prometheus, Grafana와 각 수집기를 별도로 설치하고 저장소, 디스크 용량, 백업, 업데이트, 장애 복구와
권한을 직접 운영해야 한다. 현재는 운영 대상 트래픽이 적고 AWS 배포 환경이 이미 마련되어 있으므로 초기 구성과
유지보수 부담이 더 작은 CloudWatch Logs를 선택했다. 메트릭과 대시보드 요구가 구체화되면 별도 ADR로 재검토한다.

### EC2 호스트 파일에 로그를 저장한다

애플리케이션 설정만으로 시작하기 쉽고 외부 서비스 의존성이 적다. 하지만 로그가 특정 서버의 디스크와 수명에 묶이고,
로그 순환·보존·백업·검색 기능을 별도로 마련해야 한다. 장애로 EC2에 접근할 수 없는 상황에서 로그를 확인하기도
어려우므로 선택하지 않았다.

### 애플리케이션에서 CloudWatch SDK로 직접 전송한다

로그 전송을 애플리케이션이 직접 제어할 수 있고 Docker 외 실행 환경에서도 동일한 전송 방식을 사용할 수 있다. 그러나
AWS SDK와 자격 증명 관리가 애플리케이션 런타임에 들어오며, 전송 실패·재시도·버퍼링이 업무 코드와 운영 코드에 영향을
준다. 표준 출력과 Docker 로깅 드라이버로 책임을 분리할 수 있으므로 선택하지 않았다.

## 영향

### 긍정적 영향

- HTTP 요청과 Web Push 작업을 같은 JSON 필드 규칙으로 CloudWatch Logs Insights에서 검색할 수 있다.
- `request_id`로 요청과 전역 예외 처리 결과를 연결할 수 있고, `duration_ms`로 느린 요청을 찾을 수 있다.
- `dev`와 `prod` 로그가 환경별 로그 그룹으로 분리된다.
- provider 응답 본문과 매칭되지 않은 원본 URI를 저장하지 않아 민감 정보 노출 가능성을 줄인다.
- 별도의 로그 수집 서버와 저장소를 운영하지 않고 AWS 권한과 배포 설정으로 수집을 시작할 수 있다.

### 부정적 영향과 위험

- 로그 저장과 조회량에 따라 CloudWatch 비용이 발생하고, AWS 서비스와 로그 형식·검색 방식에 종속된다.
- EC2 Instance Profile 권한, 로그 그룹 생성과 리전 설정이 잘못되면 컨테이너 로그 수집이 실패할 수 있다.
- `awslogs-create-group=false`이므로 배포 전에 각 환경의 로그 그룹을 만들어야 한다.
- CloudWatch Logs만으로는 메트릭 대시보드와 Slack 알림이 완성되지 않으므로 별도 운영 설정이 필요하다.
- Loki + Prometheus + Grafana가 제공하는 세밀한 조합 분석과 이식성은 일부 포기한다.

## 미확정 사항

- `dev`와 `prod` CloudWatch 로그 그룹의 보존 기간과 비용 알림 기준
- EC2 Instance Profile에 적용할 로그 그룹별 최소 권한 정책
- 로그 필드를 활용한 CloudWatch 대시보드, 오류율·지연시간 알림과 수신 채널
- 실제 장애 또는 성능 문제 1건을 재현하고 원인·조치·조치 후 지표를 제출 문서에 기록하는 방법

## 후속 작업

- `dev`와 `prod`에 `/chongchong/{environment}/backend` 로그 그룹을 생성하고 보존 기간을 설정한다.
- EC2 Instance Profile에 `logs:CreateLogStream`, `logs:DescribeLogStreams`, `logs:PutLogEvents` 권한을 로그 그룹 범위로
  제한하여 추가한다.
- 두 환경에서 배포 후 실제 요청과 Web Push 시나리오를 실행하고 Logs Insights에서 구조화 필드 검색을 확인한다.
- 오류율, 응답 시간과 Web Push 실패 결과를 기준으로 대시보드와 알림 조건을 별도 결정한다.
- 관찰한 장애 또는 성능 문제 한 건의 원인, 조치와 조치 후 지표를 스프린트 제출 문서에 기록한다.

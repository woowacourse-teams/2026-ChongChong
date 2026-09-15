# 0031. Testcontainers PostgreSQL로 데이터베이스 특화 테스트를 실행하고 CI에서 Docker를 확인한다

- 날짜: 2026-09-09
- 관련 이슈: [#286](https://github.com/woowacourse-teams/2026-ChongChong/issues/286)
- 관련 ADR: [0003. 백엔드 테스트 범위와 역할을 정의한다](0003-define-backend-test-strategy.md),
  [0016. AWS 관리형 서비스로 백엔드 CI/CD 파이프라인을 구성한다](0016-establish-backend-ci-cd-pipeline.md),
  [0029. 스터디 멤버십 상한을 행 잠금과 트랜잭션으로 보호한다](0029-protect-study-membership-limits-with-row-locks.md)

## 배경

일반적인 서비스와 API 테스트는 H2를 사용하면 빠르게 실행할 수 있다. 그러나 PostgreSQL의 `ON CONFLICT` upsert,
트랜잭션 격리와 행 잠금처럼 데이터베이스 구현에 의존하는 동작은 H2의 PostgreSQL 호환 모드만으로 실제 운영 데이터베이스의
동작을 증명하기 어렵다.

푸시 토큰의 동시 upsert와 스터디 멤버십 상한의 동시성 제어는 실제 PostgreSQL에서 하나의 행으로 수렴하는지와 잠금이
기대한 순서로 동작하는지를 검증해야 한다. 또한 GitHub Actions에서 Docker를 사용할 수 없는 경우 Testcontainers 테스트가
비활성화된 채 전체 테스트가 성공하면 PostgreSQL 특화 검증이 빠진 사실을 놓칠 수 있다.

## 결정

### 테스트 데이터베이스를 위험에 따라 분리한다

- 일반 단위 테스트와 데이터베이스 특화 동작이 없는 API 테스트는 기존처럼 H2를 사용한다.
- PostgreSQL의 upsert, 제약 조건, 트랜잭션 또는 동시성 동작을 검증하는 테스트는 Testcontainers가 실행한 PostgreSQL을
  사용한다.
- 공통 기반 클래스 `PostgresContainerTest`가 `postgres:16-alpine` 컨테이너의 생명주기와 Spring datasource 속성을 관리한다.
- PostgreSQL 테스트는 컨테이너의 JDBC URL·사용자·비밀번호를 `@DynamicPropertySource`로 주입하고,
  `ddl-auto=create-drop`과 Flyway 비활성화로 테스트 스키마를 격리한다.
- 현재 `PushTokenConcurrencyTest`와 `StudyMembershipConcurrencyTest`가 이 기반 클래스를 사용한다.
- 테스트 데이터 정리는 데이터베이스 제품명을 확인해 H2와 PostgreSQL에 맞는 테이블 조회 및 truncate 구문을 사용한다.

### CI는 Docker를 Testcontainers 테스트의 사전 조건으로 확인한다

- backend CI job은 Gradle 테스트와 JAR 빌드 전에 `docker info`를 실행한다.
- Docker 확인이 실패하면 `./gradlew test bootJar`를 실행하지 않고 CI를 실패시킨다.
- PostgreSQL 컨테이너의 시작과 종료는 CI workflow가 아니라 Testcontainers가 담당한다. 별도의 공유 PostgreSQL 서버나
  저장소에 포함된 데이터베이스 상태를 사용하지 않는다.
- Docker가 없는 로컬 환경에서는 `@Testcontainers(disabledWithoutDocker = true)`에 따라 PostgreSQL 특화 테스트를 건너뛸 수
  있지만, Docker가 제공되는 CI에서는 사전 확인을 통과한 뒤 해당 테스트가 실행되어야 한다.

## 선택 이유

H2를 일반 테스트에 계속 사용하면 빠른 피드백을 유지하면서도, PostgreSQL 의미론이 중요한 위험에만 실제 데이터베이스를
투입할 수 있다. Testcontainers는 테스트가 필요한 버전의 PostgreSQL을 직접 만들고 정리하므로 공유 개발 데이터베이스의
상태와 포트에 의존하지 않는다.

CI의 `docker info` 사전 확인은 Testcontainers가 Docker 부재를 이유로 테스트를 조용히 비활성화하는 상황을 조기에 드러낸다.
따라서 CI가 성공했다는 사실이 PostgreSQL 특화 테스트까지 실행되었다는 전제와 일치한다.

## 검토한 대안

### 모든 테스트를 H2에서 실행한다

실행 속도와 환경 구성은 가장 단순하지만 PostgreSQL의 upsert, 행 잠금과 트랜잭션 동작을 검증하지 못한다. 데이터베이스
특화 위험을 테스트 전략에서 누락하므로 선택하지 않았다.

### 공유 개발 PostgreSQL 또는 CI 서비스 컨테이너를 사용한다

별도 데이터베이스를 준비하면 실제 PostgreSQL을 사용할 수 있지만 데이터 격리, 초기화, 접속 정보와 버전 관리가 테스트
환경 외부로 빠져나간다. Testcontainers가 테스트 단위로 생명주기를 소유하는 편이 재현성이 높으므로 선택하지 않았다.

### Docker Compose로 PostgreSQL을 직접 관리한다

개발자가 이미 사용하는 Compose 구성을 재사용할 수 있지만 테스트마다 컨테이너 준비와 종료를 별도로 관리해야 하고 CI의
포트·서비스 이름·정리 절차가 테스트 코드와 분리된다. 현재 범위에서는 Testcontainers 기반 공통 클래스를 선택한다.

### 모든 테스트를 PostgreSQL 컨테이너에서 실행한다

운영 데이터베이스와의 일치도는 높아지지만 일반 단위 테스트까지 컨테이너 시작과 데이터베이스 초기화 비용을 부담한다.
PostgreSQL 의미론이 필요한 테스트만 분리하는 현재 테스트 전략보다 피드백이 느려지므로 선택하지 않았다.

### Docker 확인 없이 Testcontainers 테스트를 조건부로 건너뛴다

Docker가 없는 개발 환경에서는 편하지만 CI가 핵심 동시성 테스트를 실행하지 않은 채 성공할 수 있다. CI에서 Docker를 명시적으로
확인해 환경 문제를 테스트 실패로 드러내는 방식을 선택한다.

## 영향

### 긍정적 영향

- PostgreSQL의 실제 upsert, 유일 제약과 동시성 동작을 검증할 수 있다.
- 일반 테스트의 빠른 H2 피드백은 유지한다.
- 컨테이너가 테스트별 데이터베이스 생명주기를 소유해 공유 상태와 포트 충돌을 줄인다.
- CI에서 Docker가 준비되지 않은 경우 PostgreSQL 특화 테스트 누락을 조기에 발견한다.
- 스터디 멤버십 행 잠금과 푸시 토큰 동시 upsert를 같은 실데이터베이스 전략으로 재현할 수 있다.

### 부정적 영향과 위험

- PostgreSQL 특화 테스트를 실행하려면 로컬과 CI에 Docker가 필요하다.
- 첫 실행 시 `postgres:16-alpine` 이미지 다운로드와 컨테이너 시작으로 테스트 시간이 늘어날 수 있다.
- H2와 PostgreSQL의 정리 구문·upsert 구문을 각각 유지해야 하므로 데이터베이스별 코드가 증가한다.
- `disabledWithoutDocker = true`인 로컬 실행은 PostgreSQL 테스트를 건너뛸 수 있으므로 개발자가 해당 결과를 전체 검증으로
  오해하지 않아야 한다.
- 이미지 태그가 변경되거나 외부 Registry에 접근할 수 없으면 테스트 재현성과 실행 여부에 영향을 줄 수 있다.

## 미확정 사항

- `postgres:16-alpine` 이미지 digest 고정 및 버전 업데이트 주기
- CI에서 PostgreSQL 이미지 레이어를 캐시하는 방법
- PostgreSQL 컨테이너로 확장할 추가 Repository·API 테스트 범위

## 후속 작업

- 로컬 개발자가 Docker와 PostgreSQL 특화 테스트 실행 조건을 확인할 수 있도록 개발 문서에 사용법을 추가한다.
- PostgreSQL 이미지 업데이트 시 동시성·upsert 테스트를 재실행하고 버전 변경 근거를 기록한다.
- 새로운 데이터베이스 특화 쿼리나 동시성 제약을 추가할 때 해당 테스트를 `PostgresContainerTest` 기반으로 분류한다.

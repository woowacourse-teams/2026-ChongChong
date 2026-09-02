# 0027. 스터디 조회 응답 조립과 영속 타입 의존을 분리한다

- 날짜: 2026-09-02
- 관련 이슈: [#245](https://github.com/woowacourse-teams/2026-ChongChong/issues/245)
- 관련 ADR: [0002. 도메인 중심으로 패키지를 구성한다](0002-organize-packages-by-domain.md),
  [0005. 레이어 경계에서 전용 데이터 객체를 사용한다](0005-transfer-data-across-layer-boundaries.md)

## 배경

스터디 상세 응답 DTO가 Assignment와 Notice 엔티티 및 Repository Projection을 직접 참조하고 있었다.
이로 인해 sibling 도메인의 영속 조회 형태가 변경되면 Study HTTP 응답 DTO까지 함께 변경해야 했다.

또한 응답 조립 책임이 DTO와 StudyService에 나뉘어 있어 DTO가 HTTP 응답 형식과 sibling 도메인의 영속 구조를
동시에 알아야 했다.

## 결정

- Study의 HTTP Request/Response DTO는 `study.controller.dto` 패키지에 둔다.
- Study 응답 DTO는 Assignment·Notice 엔티티, Repository Projection 등 sibling 도메인의 영속 타입을 직접
  참조하지 않는다.
- StudyService는 Study 조회 유스케이스의 조립 경계로서 sibling Repository와 Projection을 조회할 수 있다.
- StudyService는 조회 결과를 응답 DTO가 요구하는 scalar/read value로 변환한 뒤 전용 응답 DTO를 생성한다.
- 단순한 응답 생성은 응답 DTO의 생성자를 사용하고, 범용 Mapper 계층은 도입하지 않는다.
- StudyService가 Assignment와 Notice의 조회 데이터를 조합하는 것은 허용한다. 다만 sibling 영속 타입이 HTTP
  응답 DTO의 계약으로 누출되지 않도록 한다.

## 선택 이유

응답 DTO가 sibling 도메인의 영속 구조를 알지 않으면 조회 방식이나 Projection 필드가 변경되어도 HTTP 응답 계약과
DTO의 변경 범위를 줄일 수 있다.

조회 데이터의 변환을 StudyService의 조립 경계에 모으면 DTO는 응답 형식에 집중하고, 서비스는 유스케이스에 필요한
데이터 조회와 조립 순서를 책임질 수 있다.

현재 변환은 유스케이스에 한정된 단순 scalar 매핑이므로 별도 Mapper를 도입하면 클래스와 추상화만 늘어난다. 변환
규칙의 복잡도나 재사용성이 커질 때 별도 컴포넌트 도입을 재검토한다.

## 검토한 대안

### 응답 DTO가 sibling 엔티티와 Projection을 직접 참조

변환 코드가 줄어들고 구현은 간단하지만, sibling 도메인의 영속 구조가 HTTP 응답 계층으로 전파된다. 도메인 간 변경
결합이 커지므로 선택하지 않았다.

### 범용 Mapper 계층 도입

변환 책임을 별도 계층으로 분리할 수 있지만, 현재는 Study 조회 조립에 한정된 단순 변환만 존재한다. 일반화된
Mapper를 먼저 도입하면 불필요한 추상화와 탐색 비용이 생기므로 선택하지 않았다.

### Controller에서 sibling 조회 결과를 응답 DTO로 변환

HTTP 경계에서 변환할 수 있지만 Controller가 Repository 조회 형태와 sibling 영속 타입을 알아야 한다. 서비스의
유스케이스 조립 책임이 Controller로 이동하므로 선택하지 않았다.

## 영향

### 긍정적 영향

- Study HTTP 응답 DTO가 Assignment·Notice 영속 타입에 결합되지 않는다.
- sibling 도메인의 조회 방식 변경이 응답 DTO까지 전파되는 범위를 줄인다.
- 응답 형식과 조회 데이터 조립 책임을 구분할 수 있다.
- 현재 규모에 불필요한 범용 Mapper 계층을 추가하지 않는다.

### 부정적 영향과 위험

- StudyService가 여러 sibling Repository와 Projection에 의존할 수 있다.
- 조회 조립 코드가 유스케이스별로 중복될 수 있다.
- StudyService가 지나치게 많은 도메인 데이터를 조합하게 되면 별도 읽기 조립 컴포넌트를 검토해야 한다.

## 미확정 사항

- Study 조회 유스케이스가 증가해 조립 로직의 변경 이유가 분리되면 전용 Assembler 또는 읽기 모델을 도입할지
  재검토한다.

## 후속 작업

- Study 응답 DTO에 sibling 엔티티와 Projection 타입이 추가되지 않는지 리뷰한다.
- 조회 조립 변경 시 StudyService 단위 테스트와 Study API 인수 테스트를 함께 수정한다.
- 조립 로직의 복잡도와 재사용성이 증가하면 별도 컴포넌트 도입 여부를 결정한다.

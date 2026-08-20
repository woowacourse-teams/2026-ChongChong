# 0008. 백엔드 코드의 이름과 생성 형식을 통일한다

- 날짜: 2026-08-20
- 관련 이슈: 없음

## 배경

같은 공지 수신자 개념을 `member`, `recipient`, `complete`처럼 서로 다른 용어로 표현하고, Response DTO의 생성
방식도 생성자, `from`, `of`, `toLeader`처럼 혼용하고 있다. 서비스 메서드와 Controller 파라미터 순서, Repository의
반환 방식 및 aggregate 자식 삭제 방식에도 서로 다른 형태가 존재한다. 이러한 차이는 이름만으로 코드의 의미와 사용법을
예측하기 어렵게 하고, 기능을 추가할 때 기존 규칙을 선택하기 어렵게 한다.

## 결정

### 도메인 용어

- 하나의 개념에는 하나의 용어를 사용한다.
- 공지 작성자는 Java 코드와 물리 컬럼 모두 `writer`로 표현한다. 공지 작성자 외래 키는 `writer_id`를 사용한다.
- 공지 수신자는 `recipient`로 표현한다.
- 수신자 수는 `recipientCount`, 읽은 수신자 수는 `readRecipientCount`로 표현한다.
- 컬렉션의 개수를 반환하는 메서드는 단수 명사 뒤에 `Count`를 붙인다. 예를 들어 `getRecipientCount`를 사용한다.
- 여러 리마인더 중 가장 늦은 시각은 `latestRemindAt`으로 표현한다.

### DTO 생성 메서드

- 하나의 주요 객체로부터 변환하면 `from`을 사용한다.
- 여러 개별 값으로 생성하면 `of`를 사용한다.
- 사용자 역할이나 사용 목적에 따라 생성 방식이 달라지면 `forLeader`, `forMember`처럼 `for`를 사용한다.
- 정적 팩터리 메서드를 제공한 DTO는 호출부에서 생성자를 직접 호출하지 않는다.

### 서비스와 Controller

- 서비스의 생성, 수정, 삭제 메서드는 각각 `create`, `update`, `delete`를 사용한다.
- 서비스의 목록 및 상세 조회 메서드는 각각 `getList`, `getDetail`을 사용한다.
- Controller 메서드의 파라미터는 인증 정보, Path Variable, Query Parameter, Request Body 순서로 배치한다.
- 응답 본문이 없는 수정과 삭제 성공 응답은 `204 No Content`로 통일한다.

### Repository와 aggregate

- Spring Data Repository 인터페이스에는 별도의 `@Repository`를 붙이지 않는다.
- 조회 결과가 없을 수 있으면 nullable Entity 대신 `Optional`을 반환한다.
- 여러 식별자가 포함된 파생 쿼리 메서드는 aggregate 식별자를 먼저 작성한다.
- aggregate 자식은 aggregate root의 cascade와 orphan removal을 통해 함께 삭제한다. 자식 Repository에서 먼저
  명시적으로 삭제하지 않는다.
- 사용되지 않는 Repository는 향후 사용 가능성만으로 유지하지 않는다.

### 물리 스키마 변경

- 물리 컬럼 이름도 도메인 용어와 일치시킨다.
- 테스트 fixture의 물리 컬럼 이름도 Entity의 컬럼 매핑과 일치시킨다.

## 선택 이유

같은 개념을 같은 이름과 형태로 표현하면 호출자가 구현을 열어보지 않고도 의미와 사용법을 예측할 수 있다. DTO 팩터리,
서비스 메서드와 Repository 규칙을 제한하면 새로운 기능에서도 기존 코드와 같은 선택을 반복할 수 있다. aggregate root를
통해 자식의 생명주기를 관리하면 도메인 모델과 영속성 처리 방식이 일치한다.

## 검토한 대안

### 구현자가 상황에 따라 자유롭게 이름과 생성 방식을 선택한다

각 코드에서 가장 짧은 구현을 선택할 수 있지만 같은 개념과 역할에 여러 표현이 생기고, 리뷰할 때마다 형식에 대한 논의가
반복된다.

### 물리 컬럼은 기존 이름을 유지한다

마이그레이션이 필요 없다는 장점이 있지만 Java의 `writer`와 데이터베이스의 `member_id`가 계속 다른 의미를 표현하게 된다.
기존 데이터를 보존하는 컬럼 이름 변경 SQL을 제공할 수 있으므로 채택하지 않았다.

## 영향

### 긍정적 영향

- 이름만으로 객체와 값의 역할을 예측하기 쉬워진다.
- DTO 생성 및 서비스 메서드의 형태가 일정해진다.
- aggregate 생명주기와 영속성 삭제 방식이 일치한다.
- Java 모델과 물리 데이터베이스의 용어가 일치한다.

### 부정적 영향과 위험

- 응답 필드 이름 변경은 프론트엔드 API 계약 변경을 동반한다.
- 정적 팩터리 규칙을 적용하기 위한 작은 변환 코드가 추가될 수 있다.

## 미확정 사항

- 리더에게는 전체 읽음 완료 여부, 스터디원에게는 본인의 읽음 여부를 뜻하는 `isComplete` 필드의 의미 분리는 추후
  별도로 결정한다.

## 후속 작업

- 프론트엔드가 `totalMemberCount`, `readMemberCount` 대신 `recipientCount`, `readRecipientCount`를 사용하도록 API
  계약을 반영한다.

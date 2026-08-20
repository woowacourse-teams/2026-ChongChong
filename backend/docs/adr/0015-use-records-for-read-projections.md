# 0015. 조회 Projection을 record로 정의한다

- 날짜: 2026-08-21
- 관련 이슈: 없음
- 관련 ADR: [0005. 레이어 경계에서 전용 데이터 객체를 사용한다](0005-transfer-data-across-layer-boundaries.md)

## 배경

ADR-0005는 특정 조회에 필요한 필드만 읽기 전용 Projection으로 조회할 수 있다고 결정했지만, Projection의 구체적인 형태는
정하지 않았다. Spring Data의 인터페이스 기반 Projection은 별칭과 getter만 선언하면 구현체를 작성하지 않아도 되지만,
런타임 프록시와 getter 이름을 통해 값이 연결된다. 프로젝트의 요청과 응답 DTO는 불변 데이터 전달 객체임을 명시하기 위해
record로 통일하고 있으므로 읽기 전용 Projection에도 같은 기준이 필요하다.

## 결정

- 읽기 전용 Projection은 record로 정의한다.
- Projection은 해당 도메인의 `repository.projection` 패키지에 두고 이름에 `Projection` 접미사를 사용한다.
- JPQL에서는 생성자 표현식으로 Projection record를 직접 생성한다.
- 서비스는 record 접근자를 사용하여 조회 결과를 애플리케이션 응답 형태로 변환한다.
- 조회 결과만으로 계산할 수 있고 상태를 변경하지 않는 편의 메서드는 Projection record에 둘 수 있다.
- 프레임워크 제약으로 record를 사용할 수 없는 경우에는 별도 결정 없이 인터페이스 Projection을 혼용하지 않고 팀에서 다시
  검토한다.

## 선택 이유

record는 전달할 필드와 생성자를 코드에 명시하고 생성 이후 상태를 변경할 수 없으므로 읽기 전용 조회 결과의 성격을 잘
표현한다. 요청 및 응답 DTO와 같은 형태를 사용하면 접근자와 생성 방식이 일관되고, Spring Data가 런타임에 만드는 프록시와
쿼리 별칭 규칙에 대한 의존을 줄일 수 있다. Repository 통합 테스트에서 실제 생성자 Projection 조회를 검증하면 JPQL과
record 생성자 사이의 계약도 보호할 수 있다.

## 검토한 대안

### Spring Data 인터페이스 Projection을 사용한다

JPQL 조회 필드에 별칭을 지정하고 getter만 선언하면 되어 코드가 짧다. 그러나 구현체가 런타임 프록시로 생성되고 일반 DTO의
record 접근자와 다른 getter 규칙을 사용한다. 프로젝트의 데이터 전달 타입을 record로 통일하기 위해 채택하지 않았다.

### 일반 클래스로 Projection을 정의한다

생성자 Projection을 명시적으로 사용할 수 있지만 필드, 생성자와 접근자를 반복해서 작성해야 한다. 동일한 불변 데이터
전달 목적을 더 간결하게 표현하는 record가 있으므로 채택하지 않았다.

## 영향

### 긍정적 영향

- 요청, 응답과 조회 Projection의 데이터 전달 타입이 record로 통일된다.
- Projection의 구성 값과 불변성이 코드에 명시된다.
- 런타임 프록시와 getter 별칭 연결에 의존하지 않는다.

### 부정적 영향과 위험

- JPQL 생성자 표현식에 Projection의 전체 패키지 이름을 작성해야 한다.
- Projection의 이름이나 생성자 구성을 변경하면 관련 JPQL도 함께 수정해야 한다.
- 쿼리와 생성자 사이의 불일치는 컴파일 시점이 아니라 애플리케이션 컨텍스트 또는 Repository 테스트 실행 시 발견된다.

## 미확정 사항

- 없음

## 후속 작업

- 새로운 읽기 전용 Projection을 추가할 때 record와 생성자 Projection을 사용한다.

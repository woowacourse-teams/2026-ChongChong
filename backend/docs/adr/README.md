# Architecture Decision Records

Architecture Decision Record(ADR)는 백엔드에 영향을 주는 중요한 기술적 결정을 배경 및 근거와 함께 기록한다.
ADR은 코드가 무엇을 하는지만 설명하지 않고, 당시 어떤 대안을 검토했고 왜 현재 방식을 선택했는지 남기는 것을 목적으로 한다.

## 문서 규칙

- ADR은 `backend/docs/adr`에 저장한다.
- 파일명은 `NNNN-kebab-case-title.md` 형식을 사용한다.
- 번호는 네 자리 일련번호이며, 기존 번호를 재사용하지 않는다.
- 하나의 ADR에는 함께 승인하거나 철회할 수 있는 하나의 결정만 기록한다.
- 새 ADR은 [템플릿](template.md)을 복사하여 작성하고 아래 목록에 추가한다.
- 기존 결정을 바꿀 때는 기존 문서의 결론을 고치지 않고 새 ADR을 작성하며 두 문서에 대체 관계를 표시한다.
- 오탈자나 의미를 바꾸지 않는 설명 보완은 승인된 ADR에서도 수정할 수 있다.

## ADR 목록

| 번호                                                  | 제목                                        |
| ----------------------------------------------------- | ------------------------------------------- |
| [0001](0001-adopt-architecture-decision-records.md)   | Architecture Decision Record를 도입한다     |
| [0002](0002-organize-packages-by-domain.md)           | 도메인 중심으로 패키지를 구성한다           |
| [0003](0003-define-backend-test-strategy.md)          | 백엔드 테스트 범위와 역할을 정의한다        |
| [0004](0004-standardize-error-response.md)            | 에러 응답과 에러 코드 형식을 통일한다       |
| [0005](0005-transfer-data-across-layer-boundaries.md) | 레이어 경계에서 전용 데이터 객체를 사용한다 |
| [0006](0006-design-common-exception-types.md)         | 공통 예외 처리 타입의 책임을 분리한다       |
| [0007](0007-establish-access-token-authentication-boundary.md) | Access Token 인증 경계를 구성한다 |
| [0008](0008-establish-auth-token-and-session-lifecycle.md) | 인증 토큰 발급과 세션 생명주기를 정의한다 |
| [0009](0009-establish-provider-independent-social-login-boundary.md) | 제공자 독립 소셜 로그인 경계를 구성한다 |
| [0010](0010-choose-per-study-count-queries-for-my-study-list.md) | 내 스터디 목록에서 스터디별 집계 쿼리를 사용한다 |
| [0011](0011-explicitly-delete-study-dependencies.md) | 스터디 삭제 시 하위 데이터를 서비스에서 명시적으로 삭제한다 |
| [0012](0012-standardize-backend-code-conventions.md) | 백엔드 코드의 이름과 생성 형식을 통일한다 |
| [0013](0013-unify-application-time-source.md) | 애플리케이션 기준 시각을 Clock으로 통일한다 |
| [0014](0014-introduce-cursor-page-request.md) | 커서 페이지 요청을 값 객체로 관리한다 |

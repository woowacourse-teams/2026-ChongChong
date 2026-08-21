# 0009. 제공자 독립 소셜 로그인 경계를 구성한다

- 날짜: 2026-08-20
- 관련 이슈: [#72](https://github.com/woowacourse-teams/2026-ChongChong/issues/72)
- 후속 결정: [0016. RN 소셜 로그인 HTTP 계약을 정의한다](0016-define-rn-social-login-http-contract.md)

## 배경

[Access Token 인증 경계 ADR](0007-establish-access-token-authentication-boundary.md)에서는 총총 Access Token을
검증해 내부 사용자 ID를 복원하는 방식을 결정했다. [인증 토큰과 세션 ADR](0008-establish-auth-token-and-session-lifecycle.md)
에서는 이미 존재하는 내부 사용자 ID를 기준으로 Access/Refresh Token을 발급하고 사용자당 하나의 활성
`AuthSession`을 유지하는 기반을 구성했다.

그러나 현재는 소셜 제공자가 확인한 사용자를 총총 `User`와 연결하는 모델과 로그인 흐름이 없다. 실제 Google 통신부터
구현하면 다음 책임이 한 변경에 섞인다.

- Authorization Code를 Google Token으로 교환하는 외부 통신
- 제공자 Token과 사용자 정보 검증
- 제공자 사용자와 총총 사용자 연결
- 처음 로그인한 사용자의 자동 가입
- 총총 Token 발급과 인증 Session 교체
- 동시 첫 로그인 데이터 정합성

외부 제공자 통신 실패와 총총 내부 Transaction 실패를 분리하고, Google 없이도 신규·기존 사용자 로그인 흐름을
검증할 수 있는 애플리케이션 경계가 필요하다.

## 결정

### 제공자 독립 로그인 경계

- 소셜 제공자의 인증 증명을 검증하는 책임을 `SocialLoginClient` 인터페이스 뒤에 둔다.
- Client는 제공자별 입력을 검증하고 총총 내부 표준 결과인 `SocialUserInfo`를 반환한다.
- 로그인 애플리케이션 서비스는 Google Token 응답, HTTP Client와 제공자별 JSON 형식을 알지 않는다.
- 실제 지원 여부는 `SocialProvider` 값의 존재가 아니라 해당 Provider를 처리하는 Client가 등록되어 있는지로 판단한다.
- Provider Client를 선택하는 Registry 또는 Resolver는 등록되지 않은 Provider 요청을 공통 Auth 오류로 거부한다.

내부 표준 결과에는 다음 정보만 포함한다.

| 정보 | 용도 |
| --- | --- |
| `provider` | 제공자 구분 |
| `providerUserId` | 제공자가 보장하는 불투명 사용자 식별자 |
| `displayName` | 최초 총총 User 이름 생성 |
| `profileImageUrl` | 최초 총총 User 프로필 이미지 생성, 선택 값 |

Authorization Code, Provider Access Token, Provider Refresh Token과 ID Token 원문은 `SocialUserInfo`에 포함하지 않는다.
제공자 검증을 마친 뒤에는 해당 값을 총총의 DB 계층이나 Token 발급 서비스로 전달하지 않는다.

### SocialAccount가 외부 사용자와 총총 User를 연결

`social_accounts`는 다음 정보를 저장한다.

| 컬럼 | 규칙 |
| --- | --- |
| `id` | 소셜 계정 식별자 |
| `user_id` | 총총 내부 사용자, `NOT NULL`, FK |
| `provider` | 제공자 종류, `NOT NULL` |
| `provider_user_id` | 제공자 사용자 식별자, `NOT NULL`, 최대 255자 |
| `created_at` | 기존 `BaseEntity` 규칙 사용 |
| `updated_at` | 기존 `BaseEntity` 규칙 사용 |

- `(provider, provider_user_id)`에 데이터베이스 유일 제약을 둔다.
- `SocialAccount`는 `User`를 지연 로딩 단방향 관계로 참조한다.
- `user_id`에는 유일 제약을 두지 않아 한 총총 User가 향후 여러 제공자 계정과 연결될 수 있는 구조를 유지한다.
- `SocialAccount`는 사용자 프로필이 아니라 외부 인증 주체와의 연결이므로 `auth` 도메인이 소유한다.
- `User`는 `SocialProvider`, Authorization Code와 Provider Token을 알지 않는다.
- 현재 프로젝트의 스키마 관리 방식인 JPA `ddl-auto`를 유지하며 이 이슈에서 Flyway를 함께 도입하지 않는다.

`providerUserId`는 이메일, 표시 이름 또는 프로필 URL이 아니라 제공자가 보장하는 안정적인 사용자 식별자를 사용한다.
이 값은 불투명 식별자로 취급하고 총총이 임의로 소문자 변환하거나 형식을 해석하지 않는다.

### 신규 사용자와 기존 사용자 처리

Provider 검증이 끝난 뒤 내부 로그인 서비스는 다음 순서로 동작한다.

1. `(provider, providerUserId)`로 `SocialAccount`를 조회한다.
2. 기존 SocialAccount가 있으면 연결된 `User`를 사용한다.
3. 없으면 검증된 `displayName`, `profileImageUrl`로 `User`를 생성한다.
4. 새 User와 Provider 사용자를 연결하는 `SocialAccount`를 생성한다.
5. 확정된 User ID로 기존 `AuthTokenService.issue(userId)`를 호출한다.
6. 총총 Access/Refresh Token과 단일 `AuthSession`을 발급한다.

`SocialUserInfo.displayName`은 공백이 아닌 값이어야 한다. 총총 로그인 Core는 임의의 기본 이름을 만들어 Provider 응답
오류를 숨기지 않는다. 실제 Provider가 이름을 제공하지 않는 경우의 사용자 경험 정책은 해당 Provider Adapter를 구현하기
전에 별도로 결정한다.

재로그인은 인증과 계정 연결만 수행한다. 기존 `User`의 이름과 프로필 이미지를 Provider 값으로 자동 덮어쓰지 않는다.
프로필 동기화는 사용자 수정 정책과 함께 별도 기능으로 결정한다.

### Provider 호출과 DB Transaction을 분리

- `SocialLoginFacade`는 DB Transaction 밖에서 `SocialLoginClient`를 호출한다.
- Client가 반환한 검증 완료 결과만 `SocialLoginService`에 전달한다.
- `SocialLoginService`는 User 조회·생성, SocialAccount 연결과 Token 발급을 짧은 DB Transaction에서 처리한다.
- Provider 응답을 기다리는 동안 DB Connection이나 행 잠금을 보유하지 않는다.
- User 또는 SocialAccount 저장과 Token 발급 중 하나라도 실패하면 내부 변경을 모두 rollback한다.

바깥 `SocialLoginService` Transaction이 이미 시작되면 내부 `AuthTokenService`의 Transaction 격리 수준 선언이 새로
적용되지 않을 수 있다. 소셜 로그인 내부 Transaction도 `READ_COMMITTED`를 명시하여 기존 사용자 행 잠금과 Session
조회가 같은 격리 가정에서 동작하게 한다.

같은 클래스의 내부 메서드 호출로 `@Transactional` Proxy를 우회하지 않는다. Transaction 재시도가 필요하면 실패한
Transaction 안에서 반복하지 않고 Facade 또는 별도 Retry 경계가 새 Transaction을 호출한다.

### 동시 첫 로그인은 DB 제약과 새 Transaction으로 처리

동일한 `(provider, providerUserId)`에 대한 두 첫 로그인 요청은 모두 사전 조회에서 계정이 없다고 판단할 수 있다.
존재하지 않는 SocialAccount를 비관적 잠금으로 조회해도 잠글 행이 없으므로 빈 결과 조회만으로 중복 생성을 막지 않는다.

- `(provider, provider_user_id)` 데이터베이스 유일 제약을 최종 정합성 경계로 사용한다.
- 신규 SocialAccount는 필요한 시점에 flush하여 중복 제약 실패를 Transaction 경계 안에서 확인한다.
- 예상한 소셜 계정 유일 제약 위반은 현재 Transaction을 rollback한 뒤 새 Transaction에서 기존 SocialAccount를 다시
  조회한다.
- 다른 제약 위반과 모든 `DataIntegrityViolationException`을 중복 로그인으로 간주하지 않는다.
- 두 요청이 모두 유효한 Provider 인증 결과라면 같은 총총 User에 수렴하도록 한다.
- 두 요청이 모두 Token을 발급하면 단일 Session 정책에 따라 마지막 로그인에서 발급한 Refresh Token만 현재 Session에
  남는다.

동시 첫 로그인은 실제 DB와 두 실행 흐름을 사용하는 통합 테스트로 User, SocialAccount와 AuthSession의 최종 개수를
검증한다. 현재 H2 테스트는 기본 회귀를 제공하지만 PostgreSQL의 실제 제약 대기와 격리 동작까지 증명하지 않으므로,
PostgreSQL 직접 검증을 수행하지 못하면 PR에 미검증 범위를 기록한다.

### 테스트 Fake는 운영 경계 밖에 둔다

- Fake `SocialLoginClient`는 `src/test` 또는 테스트 `@TestConfiguration`에만 둔다.
- 운영 `src/main`에는 고정 Authorization Code, Fake 사용자와 테스트 Provider 응답을 넣지 않는다.
- `/dev/token`, `/test/login`, `X-User-Id` 같은 인증 우회 Endpoint를 만들지 않는다.
- 운영 애플리케이션이 Fake Bean에 의존해 시작하지 않도록 한다.
- 실제 Provider Client가 아직 없어도 서버가 시작될 수 있게 빈 Client 목록을 처리하거나, 실제 Adapter 전까지 로그인
  Controller 구성을 미루는 방식을 사용한다.

Fake는 “Provider가 신원을 검증했다”는 경계 뒤의 총총 내부 로직을 결정적으로 테스트하기 위한 대역이다. 실제 OAuth
보안 검증을 대체하지 않는다.

### 인증과 오류 응답

- `/auth/login`은 총총 Access Token 없이 접근할 수 있는 기존 공개 경로를 유지한다.
- 잘못된 요청 형식은 기존 공통 입력 오류 규격을 사용한다.
- 등록되지 않은 Provider는 `400 Bad Request` 계열 Auth 오류로 처리한다.
- Provider 인증 증명이 유효하지 않으면 `401 Unauthorized` 계열 Auth 오류로 처리한다.
- Provider의 타임아웃과 5xx는 사용자 인증 실패와 구분되는 외부 연동 오류로 처리한다.
- 외부 응답에는 Provider 오류 본문, Authorization Code, Provider Token, DB 제약 이름과 Stack Trace를 포함하지 않는다.

정확한 외부 오류 코드와 메시지는 Controller 구현 전에 기존 `AuthErrorCode` 형식에 맞춰 확정한다.

### 로그인 HTTP 응답과 Refresh Token 전달은 후속 결정

> 2026-08-21: RN 로그인 요청·응답과 Token 전달 책임은 ADR 0016에서 확정했다. 아래 내용은 이 ADR을 작성할 당시의
> 미확정 배경을 보존한다.

이번 결정은 제공자 독립 로그인 Core와 자동 회원가입 경계를 정의한다. 로그인 HTTP 응답에서 원문 Refresh Token을
전달하는 방식은 다음 항목이 확정된 뒤 구현한다.

- 웹 Refresh Cookie의 이름
- `Secure`, `HttpOnly`, `SameSite`, `Path`, `Domain`과 만료 속성
- 브라우저 Cookie 기반 요청의 CSRF 방어
- 앱에서 Refresh Token을 전달하고 저장하는 방식
- 웹과 앱의 응답을 구분하는 신뢰 가능한 기준

정책을 결정하지 않은 상태에서 운영 로그인 응답에 Refresh Token을 임시 JSON 필드로 추가하지 않는다. HTTP 계약이
이번 이슈 중 확정되지 않으면 Service Core와 테스트를 먼저 완료하고 Controller는 후속 이슈로 분리한다.

### 이번 결정에 포함하지 않는 범위

- Google, Kakao, Apple과 실제 네트워크 통신
- Authorization Code 교환과 Provider Token 요청
- OIDC ID Token의 서명, `iss`, `aud`, `exp`, `nonce` 검증
- OAuth `state`, 실제 Redirect URI와 모바일 PKCE 검증
- 실제 OAuth Client ID와 Client Secret
- Refresh Token Rotation과 재사용 탐지
- `/auth/refresh`와 `/auth/logout`
- 여러 기기의 동시 로그인 Session
- 여러 SocialAccount를 하나의 User로 합치는 계정 연결 기능
- 이메일 저장과 프로필 자동 동기화
- 회원 탈퇴와 사용자 정지
- `study_members` 기반 스터디별 인가

## 선택 이유

Provider 세부 구현을 Client 경계 뒤에 두면 Google의 HTTP·OIDC 구현을 추가하기 전에도 총총 내부 사용자 연결과 Token
생명주기를 독립적으로 검증할 수 있다. 이후 Kakao나 Apple을 추가할 때에도 로그인 Core의 User·SocialAccount·Session
Transaction을 반복해서 구현하지 않는다.

Provider 호출과 DB Transaction을 분리하면 외부 지연이나 장애 중 DB Connection과 잠금을 점유하는 시간을 줄인다.
검증된 내부 결과를 짧은 Transaction에 전달하면 신규 User, SocialAccount와 AuthSession이 함께 성공하거나 함께
rollback되는 경계를 만들 수 있다.

애플리케이션 사전 조회와 DB 유일 제약을 함께 사용하면 일반 흐름은 읽기 쉽게 유지하면서 다중 서버의 동시 첫 로그인도
최종적으로 중복 계정을 남기지 않는다. 실패한 Transaction 밖에서 예상한 중복만 다시 조회하면 정상적인 동시 요청을
500 오류로 끝내지 않으면서 다른 데이터 오류를 숨기지 않을 수 있다.

테스트 Fake를 운영 코드와 분리하면 실제 Google 연결 전에도 신규·기존·동시 로그인 시나리오를 빠르게 실행하면서 운영
인증 우회 기능이 배포되는 위험을 피할 수 있다.

## 검토한 대안

### Google 구현을 로그인 Service에서 직접 호출

첫 Provider 구현은 빠를 수 있지만 Google DTO, HTTP 오류와 Token 검증이 User 생성·Session Transaction에 섞인다.
다른 Provider를 추가하거나 Fake로 내부 흐름을 테스트하기 어려워 선택하지 않았다.

### Provider 네트워크 호출 전체를 DB Transaction으로 처리

한 메서드에서 흐름을 보기 쉽지만 Provider 응답을 기다리는 동안 DB Connection과 잠금을 오래 보유한다. 외부 장애가 내부
Transaction 시간과 실패율을 직접 늘리므로 선택하지 않았다.

### 이메일로 기존 User 조회

이메일은 Provider와 동의 범위에 따라 없거나 변경될 수 있고 서로 다른 Provider에서 같은 계정임을 자동 보장하지 않는다.
계정 연결 정책 없이 이메일로 User를 합치면 의도하지 않은 계정 결합 위험이 있어 선택하지 않았다.

### SocialAccount를 user 패키지에 배치

`SocialAccount`는 사용자 프로필보다 외부 인증 주체와의 연결을 표현한다. User 도메인이 OAuth Provider 개념에 의존하지
않도록 Auth 패키지가 소유하는 방식을 선택했다.

### 사전 조회만으로 중복 가입 방지

동시 요청은 모두 계정이 없다는 결과를 볼 수 있으므로 다중 인스턴스에서 중복 User를 만들 수 있다. 데이터베이스 유일
제약 없는 사전 조회만으로는 최종 정합성을 보장할 수 없어 선택하지 않았다.

### 존재하지 않는 SocialAccount를 비관적 잠금 조회

조회 결과가 없으면 잠글 행도 없다. 데이터베이스 엔진별 Gap Lock에 암묵적으로 의존하지 않고 유일 제약과 새
Transaction 재조회 방식을 선택했다.

### Fake 로그인을 운영 Profile에 제공

로컬 수동 테스트는 쉬워지지만 설정 실수로 고정 인증 값이나 Token 발급 우회가 배포될 수 있다. Fake는 테스트 소스에만
두고 실제 수동 로그인은 Google Adapter가 연결된 뒤 검증하기로 했다.

## 긍정적 영향

- Google 없이 신규·기존 사용자 로그인 Core를 자동 테스트할 수 있다.
- 실제 Provider Adapter와 총총 내부 Transaction 책임이 분리된다.
- `SocialAccount` 복합 유일 제약으로 소셜 계정 중복을 데이터베이스에서 방지한다.
- 향후 Provider 추가가 User·Token 발급 서비스 변경으로 번지는 범위를 줄인다.
- Provider Token과 총총 Token이 타입과 저장 경계에서 섞이지 않는다.
- 운영 인증 우회 기능 없이 HTTP와 Service 흐름을 검증할 수 있다.

## 부정적 영향과 위험

- Facade, Client Registry와 내부 Transaction 서비스로 타입과 계층이 늘어난다.
- 유일 제약 충돌을 새 Transaction으로 복구하는 동시성 코드와 테스트가 필요하다.
- 단일 Session 정책에서 동시 로그인 두 요청이 모두 성공하면 먼저 받은 Refresh Token이 즉시 교체될 수 있다.
- 실제 Google Adapter가 없으므로 이번 구현만으로 사용자가 운영 환경에서 로그인할 수 없다.
- H2 동시성 테스트만으로 PostgreSQL의 실제 제약 대기와 격리 동작을 완전히 보장할 수 없다.
- Refresh Token 전달 정책이 확정되지 않으면 실제 로그인 Controller 완료가 후속으로 밀릴 수 있다.

## 미확정 사항

- 첫 실제 Provider를 Google로 최종 확정할지
- Provider가 표시 이름을 주지 않을 때 사용할 사용자 이름 정책
- 로그인 요청 DTO에 `redirectUri`, `codeVerifier`와 Client 구분을 어떻게 표현할지
- 로그인 성공 시 웹과 앱의 Refresh Token 전달 방식
- 웹 Refresh Cookie와 CSRF 방어 방식
- Provider 오류 상황별 정확한 외부 에러 코드와 메시지
- PostgreSQL Testcontainers를 현재 테스트 인프라에 도입할지

## 후속 작업

- `SocialAccount` Entity와 Repository를 구현한다.
- 제공자 독립 `SocialLoginClient`와 내부 결과 타입을 구현한다.
- 테스트 전용 Fake Client를 구성한다.
- 신규·기존·동시 로그인과 자동 회원가입 서비스를 구현한다.
- 로그인 HTTP 계약이 확정되면 `/auth/login`을 구현한다.
- 후속 이슈에서 Refresh Token Rotation, 재발급과 로그아웃을 구현한다.
- 후속 이슈에서 Google Authorization Code 교환과 OIDC 검증을 구현한다.

## 참고 자료

- [Access Token 인증 경계 ADR](0007-establish-access-token-authentication-boundary.md)
- [Token과 Session 생명주기 ADR](0008-establish-auth-token-and-session-lifecycle.md)
- [OAuth 2.0 for Native Apps — RFC 8252](https://www.rfc-editor.org/info/rfc8252/)
- [OAuth 2.0 Security Best Current Practice — RFC 9700](https://www.rfc-editor.org/info/rfc9700/)
- [Spring Transaction Management](https://docs.spring.io/spring-framework/reference/data-access/transaction.html)
- [Spring Data JPA Locking](https://docs.spring.io/spring-data/jpa/reference/jpa/locking.html)

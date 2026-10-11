# 0036. 로그인 응답에 사용자 식별자를 제공한다

- 날짜: 2026-09-23
- 관련 이슈: [#360](https://github.com/woowacourse-teams/2026-ChongChong/issues/360)
- 일부 변경하는 결정: [0020. 웹 인증 토큰 전달 경계를 구성한다](0020-establish-web-authentication-token-boundary.md)
- 관련 ADR: [0008. 인증 토큰 발급과 세션 생명주기를 정의한다](0008-establish-auth-token-and-session-lifecycle.md),
  [0009. 제공자 독립 소셜 로그인 경계를 구성한다](0009-establish-provider-independent-social-login-boundary.md)

## 배경

ADR 0020은 웹 로그인과 토큰 재발급 성공 응답에서 내부 User ID를 제외하고, 사용자 정보가 필요하면 별도의 명시적인 API를
사용하도록 결정했다. 현재 로그인 응답은 Access Token 정보만 제공하고 Refresh Token은 HttpOnly Cookie로 전달한다.

프론트엔드는 로그인 완료 시점부터 PostHog 이벤트를 동일한 사용자에게 연결해야 한다. PostHog의 `distinct_id`로 사용할
안정적인 식별자가 로그인 응답에 없으면 프론트는 로그인 직후 `/users/me`을 추가 호출하거나 Access Token의 `sub` Claim을
직접 해석해야 한다.

`/users/me` 추가 호출은 분석 식별을 위해 네트워크 왕복과 실패 지점을 늘린다. Access Token 해석은 프론트엔드를 JWT 내부
형식에 결합하고, Token을 불투명한 인증 자격 증명으로 다루는 경계를 약화한다. 따라서 로그인 결과를 이미 확정한 백엔드가
해당 사용자의 식별자를 성공 응답에 명시적으로 제공한다.

## 결정

### 로그인 성공 응답에 userId를 포함한다

`POST /auth/login`의 `200 OK` 응답은 다음 형태를 사용한다.

```json
{
  "userId": 1,
  "tokenType": "Bearer",
  "accessToken": "chongchong-access-token",
  "accessTokenExpiresAt": "2026-08-24T01:00:00Z"
}
```

- `userId`는 로그인이 확정된 총총 `User`의 데이터베이스 기본키다.
- OpenAPI에서는 양의 `integer(int64)` 필수 필드로 정의한다.
- 프론트엔드는 `String(userId)`를 PostHog의 `distinct_id`로 사용한다.
- 신규 소셜 로그인과 기존 사용자 재로그인 모두 실제 로그인한 `User`의 ID를 반환한다.
- `userId`는 인증 또는 인가 판단을 위한 클라이언트 입력으로 신뢰하지 않는다. 보호 API의 사용자 식별은 계속 검증된
  Access Token의 `sub`를 사용한다.

### 변경 범위를 로그인 응답으로 제한한다

- `POST /auth/refresh` 응답에는 이번 결정으로 `userId`를 추가하지 않는다.
- `GET /users/me`의 프로필 응답에도 이번 결정으로 `userId`를 추가하지 않는다.
- 로그인과 토큰 재발급이 응답 DTO 또는 OpenAPI 스키마를 공유하고 있다면 두 계약을 분리한다.
- Refresh Token, Refresh Token 만료 시각, AuthSession ID, Refresh Token 해시, Kakao Authorization Code와 Provider
  Token은 기존과 같이 JSON 응답에 포함하지 않는다.
- Token을 포함하는 성공 응답의 `Cache-Control: no-store`와 Refresh Token의 HttpOnly Cookie 전달 정책을 유지한다.

이 결정은 ADR 0020의 웹 인증 토큰 전달 경계 전체를 대체하지 않는다. ADR 0020의 “응답에 내부 User ID를 포함하지
않는다”는 결정만 `POST /auth/login`에 한해 변경한다.

## 선택 이유

로그인 처리 과정에서는 소셜 계정과 연결된 총총 `User`가 이미 확정되어 있다. 이 시점에 사용자 기본키를 함께 반환하면
추가 조회 없이 로그인 성공과 PostHog 식별을 하나의 HTTP 흐름에서 완료할 수 있다.

`users.id`는 인증 자격 증명이나 비밀값이 아니다. 로그인한 본인의 ID만 로그인 성공 응답으로 제공하고, 서버의 인가 판단은
계속 Access Token 검증 결과를 사용하므로 ID 노출만으로 다른 사용자의 권한을 획득할 수 없다.

로그인과 재발급 계약을 분리하면 현재 필요한 변경 범위를 명확히 유지할 수 있다. 재발급 시점에도 식별자가 필요하다는 제품
요구가 생기면 프론트의 PostHog 식별자 지속 방식과 함께 별도로 검토한다.

## 검토한 대안

### /users/me에서 userId를 조회한다

프로필과 식별자를 하나의 사용자 조회 응답으로 제공할 수 있다. 그러나 로그인 완료 직후 PostHog를 식별하기 위해 추가
요청이 필요하고, 프로필 조회 실패가 분석 식별까지 지연시키므로 선택하지 않았다.

### 프론트엔드에서 Access Token의 sub를 해석한다

추가 API 필드 없이 현재 Token에서 사용자 ID를 얻을 수 있다. 그러나 프론트엔드가 JWT 구조와 Claim 규칙에 결합되고,
Access Token을 서버만 해석하는 자격 증명으로 다루기 어려워지므로 선택하지 않았다.

### PostHog 전용 무작위 식별자를 별도로 저장한다

데이터베이스 기본키 노출을 피하고 분석 도구 전용 수명 주기를 만들 수 있다. 반면 별도 컬럼, 생성·마이그레이션·중복 방지와
탈퇴 이후 처리 정책이 필요하다. 현재는 이미 안정적으로 존재하는 사용자 기본키로 요구를 충족할 수 있어 선택하지 않았다.

### 토큰 재발급 응답에도 userId를 포함한다

페이지 복구나 앱 재실행 시 다시 식별하기 쉽다. 이번 요구는 로그인 완료 시점의 식별이며 기존 재발급 계약을 함께 변경할
필요가 확인되지 않았으므로 범위에서 제외했다. 필요성이 확인되면 별도 계약 변경으로 결정한다.

## 영향

### 긍정적 영향

- 로그인 성공 직후 추가 API 호출 없이 PostHog 사용자를 식별할 수 있다.
- 프론트엔드가 Access Token의 JWT 구조를 알 필요가 없다.
- OpenAPI에서 분석 식별자의 출처와 타입을 명확하게 표현할 수 있다.
- 서버의 인증·인가 경계와 사용자 식별자 전달 책임을 분리해서 유지한다.

### 부정적 영향과 위험

- 로그인과 토큰 재발급의 성공 응답 DTO 및 OpenAPI 스키마를 별도로 관리해야 한다.
- 순차 증가하는 내부 사용자 기본키가 로그인한 사용자에게 노출된다.
- 프론트엔드가 숫자와 문자열 형태의 PostHog 식별자를 혼용하면 한 사용자의 이벤트가 분리될 수 있다.
- 새 응답 필드를 처리하지 못하는 클라이언트가 엄격한 응답 검증을 사용한다면 배포 순서를 조율해야 한다.

## 미확정 사항

- 페이지 새로고침과 토큰 재발급 이후에도 프론트엔드의 PostHog `distinct_id`가 유지되는지 확인한다.
- 향후 재발급 응답에도 `userId`가 필요해지면 해당 계약과 응답 DTO를 별도로 검토한다.

## 후속 작업

- 이슈 #360의 OpenAPI 로그인 응답에 필수 `userId` 필드를 반영한다.
- 로그인과 토큰 재발급의 응답 DTO 및 스키마를 분리한다.
- 신규 로그인과 재로그인에서 응답 `userId`가 실제 `User.id`와 일치하는 인수 테스트를 추가한다.
- `/auth/refresh`와 `/users/me` 응답 계약이 유지되는 회귀 테스트를 추가한다.
- 프론트엔드는 로그인 성공 후 `String(userId)`로 PostHog `identify`를 호출한다.

# 0016. RN 소셜 로그인 HTTP 계약을 정의한다

- 날짜: 2026-08-21
- 관련 이슈: [#86](https://github.com/woowacourse-teams/2026-ChongChong/issues/86)
- 관련 ADR: [0008. 인증 토큰 발급과 세션 생명주기를 정의한다](0008-establish-auth-token-and-session-lifecycle.md),
  [0009. 제공자 독립 소셜 로그인 경계를 구성한다](0009-establish-provider-independent-social-login-boundary.md)

## 배경

ADR 0008에서는 총총 Access/Refresh Token과 단일 활성 `AuthSession`을 발급하는 내부 기반을 정의했다. ADR 0009에서는
소셜 제공자의 인증 결과를 총총 `User`와 연결하고 Token을 발급하는 제공자 독립 로그인 Core를 구성했다. 그러나 두
결정 모두 원문 Refresh Token을 클라이언트에 전달하는 방식과 실제 로그인 HTTP 요청·응답을 후속 결정으로 남겼다.

제품 클라이언트는 RN과 WebView로 구성하며 로그인은 RN에서만 수행한다. RN은
`@react-native-google-signin/google-signin`으로 Google 로그인을 수행할 수 있고, 총총 백엔드는 Google API에 접근할
권한을 위임받을 필요 없이 Google 사용자의 신원만 확인하면 된다. 이에 따라 RN은 Google ID Token을 백엔드에 전달하고,
백엔드는 이를 검증한 사용자에게 총총 자체 Token을 발급하는 계약을 사용한다.

현재 운영 코드에는 Google ID Token을 실제로 검증하는 `SocialLoginClient`가 없다. 이번 결정은 HTTP 경계와 RN·WebView의
Token 책임을 먼저 확정하며, 실제 Google 공개키와 Claim 검증은 후속 Provider Adapter에서 구현한다.

## 결정

### RN이 로그인을 소유한다

- Google 로그인은 WebView가 아니라 RN에서 수행한다.
- RN은 Google 로그인 성공 결과에서 Google ID Token을 얻는다.
- WebView와 일반 브라우저를 위한 별도 로그인 흐름은 이번 범위에 포함하지 않는다.
- WebView는 Google ID Token과 총총 Refresh Token을 알지 않는다.

### 로그인 HTTP 요청

로그인 Endpoint와 요청은 다음과 같이 정의한다.

```http
POST /auth/login
Content-Type: application/json
```

```json
{
  "provider": "GOOGLE",
  "idToken": "google-id-token"
}
```

- `/auth/login`은 총총 Access Token 없이 호출할 수 있는 공개 경로다.
- `provider`는 필수이며 `SocialProvider` 중 요청을 처리할 Client를 선택하는 데 사용한다.
- `idToken`은 공백이 아닌 필수 문자열이다.
- 알 수 없는 Provider 문자열은 기존 공통 잘못된 요청 규격으로 처리한다.
- Enum에는 있지만 처리할 Client가 등록되지 않은 Provider는 `UNSUPPORTED_SOCIAL_PROVIDER`로 처리한다.
- 요청받은 Google ID Token 원문을 로그, 예외 메시지와 영속 Entity에 넣지 않는다.

현재 `SocialLoginCommand.authorizationCode`는 Google ID Token을 받는 계약과 의미가 다르다. HTTP DTO는 프론트엔드가
전달하는 값의 의미를 명확히 보여주기 위해 `idToken`을 사용한다. Provider 독립 Core에서는 해당 값을
`credential`이라는 중립적 이름으로 변환하여 각 `SocialLoginClient`가 해석하게 한다. 이번에는 여러 증명 종류를 동시에
표현하는 별도 타입 계층을 만들지 않는다.

### 로그인 HTTP 응답

로그인 성공은 `200 OK`와 다음 JSON으로 응답한다.

```json
{
  "tokenType": "Bearer",
  "accessToken": "chongchong-access-token",
  "accessTokenExpiresAt": "2026-08-21T01:00:00Z",
  "refreshToken": "chongchong-refresh-token",
  "refreshTokenExpiresAt": "2026-09-20T01:00:00Z"
}
```

- `tokenType`은 `Bearer`로 고정한다.
- Access Token과 Refresh Token의 원문 및 각각의 만료 시각을 JSON으로 반환한다.
- 만료 시각은 UTC `Instant`를 ISO-8601 문자열로 직렬화한다.
- 응답에 내부 Session ID, Refresh Token 해시와 입력받은 Google ID Token을 포함하지 않는다.
- 응답에 `userId`를 추가하지 않는다. 클라이언트가 JWT `sub`를 직접 해석하도록 요구하지 않으며, 사용자 정보가
  필요해지면 `/users/me`와 같은 명시적 API를 사용한다.

### RN과 WebView의 Token 책임

| 위치 | Access Token | Refresh Token |
| --- | --- | --- |
| 총총 백엔드 | 발급하지만 발급 상태를 저장하지 않음 | 원문이 아닌 SHA-256 해시만 `AuthSession`에 저장 |
| RN | 메모리에 보관 | SecureStore에 보관 |
| WebView | RN에서 전달받아 메모리에만 보관 | 전달받지 않음 |

- RN은 WebView에 Access Token과 만료 시각만 전달한다.
- WebView는 Access Token을 `localStorage`, `sessionStorage`와 URL Query Parameter에 저장하지 않는다.
- WebView는 백엔드 요청에 `Authorization: Bearer <Access Token>` Header를 사용한다.
- WebView가 새로고침되면 RN이 현재 Access Token을 다시 전달한다.
- Refresh Token 재발급과 RN·WebView 사이의 401 처리 흐름은 후속 재발급 API에서 정의한다.

### Cookie와 일반 웹 로그인은 포함하지 않는다

현재 로그인과 장기 Token 저장의 주체는 RN이므로 로그인 응답에 Cookie를 발급하지 않는다. 이번 계약에는 Cookie의
`Secure`, `HttpOnly`, `SameSite`, `Path`, credential CORS와 Cookie 기반 CSRF 정책을 적용하지 않는다.

향후 일반 브라우저 로그인을 지원하면 RN 응답 계약을 클라이언트가 보내는 임의 Header로 분기하지 않는다. 웹에 적합한
HttpOnly Refresh Cookie와 CSRF 방어를 별도 HTTP 계약 또는 Adapter로 결정한다.

### Google ID Token 검증은 후속 Adapter가 담당한다

이번 이슈의 Fake `SocialLoginClient`는 ID Token 문자열을 불투명한 테스트 증명값으로 사용한다. 운영 Google
`SocialLoginClient`는 다음을 검증한 뒤에만 `SocialUserInfo`를 반환한다.

- Google 공개키를 이용한 서명
- `iss`가 허용한 Google 발급자인지
- `aud`가 총총에서 허용한 Google Web Client ID인지
- `exp`가 지나지 않았는지
- 제공자 사용자 식별자로 검증된 `sub`를 사용하는지

RN의 `@react-native-google-signin/google-signin`에 설정하는 `webClientId`와 백엔드의 허용 Audience는 동일한 Web 유형
OAuth Client ID를 사용한다. 실제 Client ID 값, Google 공개키 조회·캐시와 Claim 변환은 후속 Google Adapter 이슈에서
구현한다.

Google ID Token 검증 Adapter가 등록되기 전 운영 `/auth/login` 요청은 성공하지 않는다. 테스트를 위해 Fake나 고정 ID
Token을 `src/main`에 추가하지 않는다.

### 오류 응답

| 상황 | HTTP 상태 | 에러 코드 |
| --- | ---: | --- |
| `provider` 또는 `idToken` 누락·공백 | 400 | 기존 입력 오류 규격 |
| 알 수 없는 Provider 문자열 | 400 | 기존 잘못된 요청 규격 |
| 등록되지 않은 Provider Client | 400 | `UNSUPPORTED_SOCIAL_PROVIDER` |
| Provider 인증 증명 실패 | 401 | `SOCIAL_AUTHENTICATION_FAILED` |
| 예상하지 못한 내부 오류 | 500 | 기존 내부 오류 규격 |

Provider 오류 본문, Google ID Token, 총총 Access/Refresh Token, DB 제약 이름과 Stack Trace를 오류 응답에 포함하지 않는다.
Provider timeout과 5xx의 외부 오류 규격은 실제 Adapter에서 실패 종류를 확인한 뒤 별도로 결정한다.

## 선택 이유

Google ID Token은 Google API에 대한 오프라인 접근이 아니라 현재 로그인 사용자의 신원만 확인하려는 요구에 맞는다. RN이
이미 얻은 ID Token을 백엔드로 전달하면 Authorization Code 교환, Client Secret, Redirect URI와 PKCE verifier를 이번
로그인 계약에 추가하지 않고도 사용자 신원을 서버에서 검증할 수 있다.

Refresh Token을 JSON으로 반환하면 RN이 원문을 SecureStore에 직접 저장할 수 있다. WebView에 Refresh Token을 전달하지
않고 짧은 수명의 Access Token만 제공하면 WebView 침해 시 노출되는 장기 자격 증명의 범위를 줄일 수 있다.

HTTP에서는 실제 입력 의미를 나타내는 `idToken`을 사용하고 내부에서는 `credential`로 일반화하면 프론트엔드 계약을
명확히 유지하면서 Provider별 증명 형식이 로그인 Service와 User·Session Transaction으로 번지는 것을 막을 수 있다.

## 검토한 대안

### Authorization Code와 PKCE verifier를 백엔드로 전달한다

백엔드가 Google API를 사용하기 위한 Provider Access/Refresh Token을 얻어야 할 때 적합하다. 현재는 Google 사용자 신원
확인만 필요하고 iOS와 Android의 Authorization Code 획득 흐름 차이까지 HTTP 계약에 포함할 이유가 없어 선택하지 않았다.

### Provider별 로그인 Endpoint를 만든다

`POST /auth/login/google`은 요청에서 Provider를 생략할 수 있지만 기존 `SocialLoginClients`의 제공자 선택 경계와 공통
로그인 Endpoint를 중복 표현한다. 현재는 `POST /auth/login`과 명시적인 `provider`를 유지한다.

### 요청 필드를 `credential`로 노출한다

Provider별 증명값을 하나의 필드로 받을 수 있지만 프론트엔드 API 명세만 보고 Google ID Token을 전달해야 한다는 의미를
알기 어렵다. HTTP 계약에는 `idToken`을 사용하고 Provider 독립 내부 타입에서만 `credential`을 사용한다.

### Refresh Token을 HttpOnly Cookie로 반환한다

일반 웹에서는 JavaScript의 직접 접근을 차단하는 장점이 있다. 현재 로그인 주체는 RN이고 Refresh Token은 SecureStore에
저장해야 하므로 Cookie를 채택하지 않았다. 일반 웹 로그인 도입 시 CSRF 정책과 함께 다시 결정한다.

### Refresh Token을 WebView에 전달한다

WebView가 직접 재발급할 수 있지만 장기 자격 증명이 JavaScript 실행 환경에 노출된다. RN이 로그인과 보안 저장을
소유한다는 경계를 약화하므로 선택하지 않았다.

## 영향

### 긍정적 영향

- RN과 백엔드가 구현할 요청·응답 필드와 상태 코드가 명확해진다.
- Access Token과 Refresh Token의 클라이언트 보관 책임이 분리된다.
- WebView가 장기 Refresh Token을 알지 않는다.
- 기존 제공자 독립 Core와 단일 Session 발급 기반을 재사용한다.
- Google Adapter 없이도 Fake 기반 HTTP 인수 흐름을 검증할 수 있다.
- 일반 웹 Cookie 정책을 현재 RN 계약에 억지로 섞지 않는다.

### 부정적 영향과 위험

- 로그인 성공 응답 본문에 Refresh Token 원문이 있으므로 RN이 응답과 Token을 로그에 남기지 않아야 한다.
- RN 또는 기기가 침해되면 SecureStore 사용만으로 모든 Token 탈취를 막을 수 없다.
- 실제 Google Adapter 전에는 운영 환경에서 정상 로그인을 수동 검증할 수 없다.
- HTTP의 `idToken` 필드는 향후 ID Token이 아닌 증명을 사용하는 Provider에 그대로 적용하기 어렵다.
- 일반 브라우저 로그인을 추가하면 별도의 Cookie·CSRF 계약이 필요하다.
- Access Token 만료와 WebView 새로고침 시 RN Bridge가 올바르게 Token을 다시 전달해야 한다.

## 미확정 사항

- 실제 Google Web Client ID 값과 환경별 설정 관리 위치
- Google이 표시 이름을 제공하지 않을 때의 사용자 이름 정책
- Google Provider timeout·5xx의 외부 에러 코드와 메시지
- RN과 WebView 사이 Token Bridge 메시지 형식과 허용 Origin 검증
- `/auth/refresh`와 `/auth/logout`의 요청·응답 및 동시 요청 정책
- 일반 웹 로그인을 지원할 경우의 Refresh Cookie와 CSRF 정책

## 후속 작업

- 이슈 #86에서 요청·응답 DTO와 `POST /auth/login`을 구현한다.
- `SocialLoginCommand`와 테스트 Fake의 Authorization Code 전용 명칭을 Provider 증명값에 맞게 변경한다.
- Fake Provider 기반 HTTP 인수 테스트에서 신규 로그인, 재로그인과 발급 Access Token 인증을 검증한다.
- 후속 이슈에서 실제 Google ID Token 검증 `SocialLoginClient`를 구현한다.
- 후속 이슈에서 Refresh Token Rotation, `/auth/refresh`와 `/auth/logout`을 구현한다.

## 참고 자료

- [Google 백엔드 ID Token 검증](https://developers.google.com/identity/sign-in/web/backend-auth)
- [React Native Google Sign-In 설정](https://react-native-google-signin.github.io/docs/original)
- [React Native Google Sign-In Expo 설정](https://react-native-google-signin.github.io/docs/setting-up/expo)
- [Expo SecureStore](https://docs.expo.dev/versions/latest/sdk/securestore/)

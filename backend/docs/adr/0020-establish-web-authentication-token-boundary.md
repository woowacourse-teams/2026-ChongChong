# 0020. 웹 인증 토큰 전달 경계를 구성한다

- 날짜: 2026-08-24
- 관련 이슈: [#109](https://github.com/woowacourse-teams/2026-ChongChong/issues/109)
- 대체하는 결정: [0018. RN 소셜 로그인 HTTP 계약을 정의한다](0018-define-rn-social-login-http-contract.md)
- 관련 ADR: [0007. Access Token 인증 경계를 구성한다](0007-establish-access-token-authentication-boundary.md),
  [0008. 인증 토큰 발급과 세션 생명주기를 정의한다](0008-establish-auth-token-and-session-lifecycle.md),
  [0009. 제공자 독립 소셜 로그인 경계를 구성한다](0009-establish-provider-independent-social-login-boundary.md)

## 배경

ADR 0018은 제품 클라이언트를 RN과 WebView로 전제했다. 로그인은 RN이 수행하고 Access Token은 RN 메모리,
Refresh Token은 SecureStore에 보관하기 위해 두 Token의 원문과 만료 시각을 로그인 JSON 응답으로 반환했다. WebView에는
Access Token만 전달하고 Cookie·CSRF·일반 웹 로그인은 후속 범위로 남겼다.

이후 제품의 우선 클라이언트가 웹 브라우저로 변경됐다. 기존 JSON 응답을 웹에서 그대로 사용하면 JavaScript가 장기
자격 증명인 Refresh Token 원문을 받아야 한다. `localStorage`, `sessionStorage`와 일반 JavaScript 메모리 중 어느 곳도
RN SecureStore와 같은 장기 보관 경계가 아니며, 페이지 새로고침 뒤 인증 복구, 재발급과 로그아웃 계약도 현재 운영
코드에는 없다.

웹 브라우저는 Cookie를 요청에 자동으로 포함하므로 Refresh Token을 Cookie로 옮기는 결정은 전달 형식만의 변경이 아니다.
Cookie 범위, CSRF와 credential CORS를 함께 결정해야 한다. 현재 `SecurityConfig`는 Bearer Token만 사용하는 전제에서
CSRF를 전체 비활성화하고 있으므로 Cookie 기반 인증 상태 변경 Endpoint를 추가한 뒤 같은 설정을 근거 없이 유지할 수 없다.

한편 `SocialLoginFacade`, `SocialLoginService`, `AuthTokenService`, `AuthSession`과 `IssuedTokenPair`는 RN 타입을 직접 알지
않는다. 이 클라이언트 중립 Core를 유지하면 현재 웹 전달 방식과 미래 앱 전달 방식을 HTTP 경계에서만 분리할 수 있다.

## 결정

### 기존 인증 Core와 OAuth 2.0 구조를 유지한다

- 첫 소셜 로그인 제공자는 Kakao로 변경한다.
- 웹은 Kakao 로그인 Redirect에서 받은 일회성 Authorization Code를 백엔드에 전달한다.
- 백엔드는 Authorization Code를 Kakao Token으로 교환하고 Kakao 사용자 정보를 조회한 뒤 총총 자체 Access Token과
  Refresh Token을 발급한다.
- Kakao 로그인은 OAuth 2.0 Authorization Code 흐름을 사용한다. OpenID Connect와 Kakao ID Token은 현재 로그인에
  필수로 도입하지 않는다.
- 총총 보호 API는 Spring Security OAuth2 Resource Server와 Bearer Access Token을 사용한다.
- `User`, `SocialAccount`, `AuthSession`, 자동 가입 Transaction과 JWT `sub` 기반 `AuthenticatedUser` 전달 방식은
  변경하지 않는다.
- 보호 API는 Cookie가 아니라 기존 `Authorization: Bearer <access-token>` 형식을 유지한다.

### 웹 로그인 요청은 Kakao Authorization Code 형식으로 전환한다

```http
GET /auth/csrf
```

웹은 먼저 CSRF Token을 발급받아 로그인 요청 Header에 포함한다. 실제 응답 DTO 이름은 구현 단계에서 프로젝트 관례에
맞추되 Token 값은 응답 본문 또는 JavaScript가 읽을 수 있는 CSRF Cookie로 전달할 수 있어야 한다.

```http
POST /auth/login
Content-Type: application/json
X-XSRF-TOKEN: <csrf-token>
```

```json
{
  "provider": "KAKAO",
  "authorizationCode": "kakao-authorization-code"
}
```

- `/auth/csrf`와 `/auth/login`은 총총 Access Token 없이 접근할 수 있다.
- `provider`와 `authorizationCode`는 필수이며 기존 오류 응답 규격을 유지한다.
- 웹 프론트엔드는 Kakao JavaScript SDK 또는 인가 Endpoint로 로그인하고 Redirect URI에서 `code`와 `state`를 받는다.
- 로그인 요청마다 고유한 `state`를 사용하고 Redirect로 돌아온 값과 일치하는지 확인한 뒤 Authorization Code를
  백엔드에 전달한다. OAuth `state`는 `/auth/csrf`의 애플리케이션 CSRF Token과 목적과 생명주기가 다른 값이다.
- 백엔드는 Kakao REST API 키, Client Secret과 동일한 Redirect URI로 Authorization Code를 Token으로 교환한다.
- Kakao가 발급한 Access·Refresh Token은 사용자 정보 조회에 필요한 범위에서만 사용하고 총총 Token으로 반환하거나
  현재 DB에 저장하지 않는다.
- Authorization Code, Kakao Token과 총총 Token 원문은 로그, 예외 메시지와 문자열 표현에 노출하지 않는다.
- 실제 Kakao Token 교환과 사용자 정보 조회 Adapter는 후속 이슈에서 구현한다.

### Access Token은 JSON, Refresh Token은 HttpOnly Cookie로 전달한다

로그인 성공은 `200 OK`와 다음 형태를 사용한다.

```http
HTTP/1.1 200 OK
Cache-Control: no-store
Set-Cookie: refresh_token=<opaque-token>; HttpOnly; Secure; SameSite=Lax; Path=/auth; Max-Age=...
```

```json
{
  "tokenType": "Bearer",
  "accessToken": "chongchong-access-token",
  "accessTokenExpiresAt": "2026-08-24T01:00:00Z"
}
```

- 로그인과 재발급 JSON에서 `refreshToken`과 `refreshTokenExpiresAt`을 제거한다.
- 웹은 Access Token을 JavaScript 메모리에만 보관하고 보호 API의 Authorization Header에 사용한다.
- Access Token과 Refresh Token을 `localStorage`, `sessionStorage`와 URL Query Parameter에 저장하지 않는다.
- 페이지 새로고침으로 Access Token 메모리가 사라지면 `/auth/csrf` 후 `/auth/refresh`를 호출해 복구한다.
- 응답에 내부 User ID, Session ID, Refresh Token 해시와 Kakao Authorization Code·Provider Token을 포함하지 않는다.
- Token을 포함하는 성공 응답은 `Cache-Control: no-store`를 유지한다.

`refreshTokenExpiresAt`은 비밀값은 아니지만 웹이 Cookie 수명을 직접 관리하지 않고 서버의 `Max-Age`가 이를 표현한다.
웹 DTO를 현재 필요한 값으로 제한하고, 미래 앱에서 필요하면 앱 전용 DTO에 다시 포함한다.

### Refresh Cookie 속성을 제한한다

Refresh Cookie는 다음 기본 계약을 사용한다.

| 속성 | 결정 |
| --- | --- |
| 이름 | `refresh_token` |
| `HttpOnly` | 항상 `true` |
| `Secure` | 배포 환경에서 `true`, 로컬 HTTP 개발은 별도 설정 |
| `SameSite` | 기본 `Lax` |
| `Path` | `/auth` |
| `Domain` | 지정하지 않는 Host-only Cookie |
| `Max-Age` | 발급 Refresh Token의 남은 유효 시간과 일치 |

- Cookie 발급과 삭제는 이름, `Path`와 `Domain` 범위가 같아야 한다.
- `Path=/auth`는 Refresh Token을 공지·과제·스터디 같은 도메인 API에 보내지 않으면서 로그인·재발급·로그아웃 경로를
  함께 포함한다.
- `Domain`을 생략해 다른 하위 도메인으로 Cookie 범위를 넓히지 않는다.
- 운영 `Secure=false`, `HttpOnly=false`, 공백 이름, 비양수 Max-Age와 허용하지 않는 SameSite 설정은 시작 또는 생성
  시점에 거부한다.
- `Secure=false`는 명시적인 `local` 프로필에서만 로컬 HTTP 개발 용도로 허용하며, 기본·테스트·그 밖의 프로필에서는
  애플리케이션 시작을 실패시킨다.
- 실제 Refresh Token 원문, Cookie Header와 환경별 Client ID를 일반 애플리케이션 로그에 기록하지 않는다.

현재 실제 배포 URL은 확정되지 않았다. 웹과 API가 cross-site인 환경에서는 `SameSite=None; Secure`가 필요할 수 있지만,
이를 기본값으로 넓히지 않는다. cross-site 배포가 확정되면 브라우저의 third-party Cookie 정책과 CSRF 위험을 함께 검토해
이 ADR을 대체하거나 보완하는 새 결정을 먼저 작성한다.

### Refresh Token을 회전한다

```http
POST /auth/refresh
X-XSRF-TOKEN: <csrf-token>
Cookie: refresh_token=<opaque-token>
```

- Controller는 Cookie 원문을 클라이언트 중립 Refresh Service에 전달한다.
- Service는 원문을 해시하고 해시로 `AuthSession`을 잠금 조회한다.
- Session 존재와 만료를 확인한 뒤 새 Access Token과 Refresh Token을 발급한다.
- 새 Refresh Token 해시와 만료 시각으로 기존 Session을 같은 Transaction에서 교체한다.
- 성공 응답은 새 Access Token JSON과 새 Refresh Cookie를 반환한다.
- 기존 Refresh Token은 성공한 Rotation 직후 현재 Session과 일치하지 않아야 한다.
- Cookie 누락, 형식 오류, 존재하지 않음, 만료와 이미 교체된 Token은 외부에서 구분하지 않는 공통 `401`로 처리한다.
- 오류 응답은 Token 원문, 해시, Session 존재 여부와 DB 제약 이름을 포함하지 않는다.

같은 Refresh Token의 동시 요청에서 데이터 정합성을 보장하는 잠금과 Transaction은 이번 이슈에서 검증한다. Token Family
전체 재사용 탐지, 공격 감지 뒤 모든 Session 폐기와 프론트엔드 Single-flight는 별도 고도화로 남긴다.

### 로그아웃은 Session과 Cookie를 함께 정리한다

```http
POST /auth/logout
X-XSRF-TOKEN: <csrf-token>
Cookie: refresh_token=<opaque-token>
```

- 유효한 Refresh Token과 일치하는 `AuthSession`을 제거하거나 무효화한다.
- 응답에서 같은 이름·Path·Domain의 Refresh Cookie를 `Max-Age=0`으로 만료시킨다.
- Cookie 또는 Session이 이미 없어도 `204 No Content`를 반환하는 멱등 동작을 사용한다.
- 로그아웃 요청으로 Session 존재 여부를 외부에 구체적으로 노출하지 않는다.
- 프론트엔드는 응답 뒤 메모리 Access Token을 제거한다.

로그아웃 시점에 이미 발급한 Access Token을 즉시 폐기하는 목록은 만들지 않는다. Access Token은 기존 짧은 수명까지
유효할 수 있으며, 즉시 폐기가 제품 요구가 되면 별도 ADR에서 Stateful 검증 비용과 함께 결정한다.

### Cookie를 사용하는 인증 상태 변경 요청에 CSRF를 적용한다

- `/auth/login`, `/auth/refresh`, `/auth/logout`의 안전하지 않은 HTTP Method는 CSRF 검증 대상이다.
- `/auth/csrf`는 총총 Access Token 없이 CSRF Token을 얻는 안전한 GET Endpoint다.
- Spring Security의 `CsrfTokenRepository`를 사용하고, 웹은 발급받은 Token을 `X-XSRF-TOKEN` Header로 다시 보낸다.
- CSRF Token 자체는 Refresh Token과 다른 요청 위조 방지값이며, Refresh Token Cookie의 `HttpOnly`를 해제하지 않는다.
- 보호 도메인 API는 브라우저가 자동 첨부하지 않는 Bearer Access Token을 사용하므로 이번 Cookie CSRF Matcher에 포함하지
  않는다.
- CSRF 실패는 기존 공통 오류 응답 형식으로 변환하고 Token이나 Framework 예외 원문을 노출하지 않는다.
- `SameSite=Lax`와 CORS는 보조 경계이며 CSRF Token 검증을 대체하지 않는다.

Spring Security는 상태를 저장하지 않는 API에서도 Cookie 같은 브라우저 자동 자격 증명을 사용하면 CSRF 위협을 고려해야
한다. OAuth2 Resource Server의 `SessionCreationPolicy.STATELESS`는 유지하며, CSRF Token 저장을 위해 `JSESSIONID` 기반
로그인 Session을 도입하지 않는다.

구현 결과 `/auth/csrf`는 `Cache-Control: no-store`와 함께 `headerName`, `token` JSON을 반환한다. 검증 기준값은
`XSRF-TOKEN` Cookie에 `Path=/auth`, `Secure`, `HttpOnly`, `SameSite=Lax`로 저장하며, JavaScript는 Cookie를 읽지 않고
응답의 마스킹된 `token`을 메모리에 보관해 `X-XSRF-TOKEN` Header로 전송한다. Spring Security의 기본 CSRF 응답
마스킹과 검증을 유지하므로 응답 Token과 Cookie 원문은 서로 다르지만 올바른 한 쌍으로 검증된다.

CSRF Matcher는 `POST /auth/login`, `POST /auth/refresh`, `POST /auth/logout`에만 적용한다. Bearer Access Token을
사용하는 도메인 API는 브라우저가 자격 증명을 자동 첨부하지 않으므로 기존 401·403 인증 경계를 유지하고 CSRF Token을
추가로 요구하지 않는다. 누락과 불일치 CSRF Token은 외부에서 구분하지 않고 공통 `403 INVALID_CSRF_TOKEN`으로
응답한다.

### same-origin 배포를 우선하고 CORS는 정확한 Origin만 허용한다

- 웹 정적 자원과 API는 가능하면 Reverse Proxy를 통해 same-origin으로 제공한다.
- same-origin 환경에는 불필요한 CORS 허용 코드를 추가하지 않는다.
- 프론트엔드와 API Origin이 다르면 환경별로 설정한 정확한 프론트엔드 Origin만 허용한다.
- credential 요청은 `allowCredentials=true`와 구체적인 `Access-Control-Allow-Origin`을 사용한다.
- credential CORS에서 wildcard Origin을 사용하지 않는다.
- 허용 Method와 Header는 로그인·CSRF·재발급·로그아웃 및 실제 API 호출에 필요한 범위로 제한한다.
- 로컬 개발 Origin을 운영 기본값에 포함하지 않고 환경별 설정으로 분리한다.
- CORS는 다른 Origin의 응답 읽기를 제한하는 브라우저 정책이며 사용자 인증과 CSRF 검증을 대신하지 않는다.

실제 배포 주소는 환경 설정에 두고 ADR, Git 기본값과 테스트 Fixture에 운영 주소를 하드코딩하지 않는다.

현재 배포 Origin은 아직 확정되지 않아 Stage 6에서는 `CorsConfigurationSource`나 허용 Origin 목록을 추가하지 않는다.
이는 모든 Origin을 허용한다는 뜻이 아니라 same-origin을 안전한 기본값으로 유지한다는 뜻이다. 신뢰하지 않는 Origin의
사전 요청에 `Access-Control-Allow-Origin`을 반환하지 않는 테스트를 두었으며, 분리 Origin 배포가 확정되면 그 환경의
정확한 Origin만 설정하고 credential wildcard를 사용하지 않는다.

### 웹 전달 방식은 HTTP Adapter 책임이다

- `SocialLoginCommand`, `SocialUserInfo`, `SocialLoginFacade`, `SocialLoginService`, `AuthTokenService`, `AuthSession`과
  `IssuedTokenPair`는 웹·Cookie·앱 저장소 타입을 알지 않는다.
- Web Controller와 Cookie Writer가 `IssuedTokenPair`를 Access Token JSON과 Refresh Cookie로 변환한다.
- Cookie와 Spring MVC 타입을 Core Service와 Entity로 전달하지 않는다.
- 클라이언트가 보내는 `clientType`, 임의 Header와 `User-Agent`로 같은 Endpoint의 Cookie·JSON 응답을 분기하지 않는다.

향후 앱 지원이 실제로 결정되면 명시적인 앱 Endpoint 또는 API 버전과 DTO를 추가한다. 앱 Adapter는 같은 Core에서 받은
Token을 앱에 적합한 JSON으로 전달할 수 있지만, 현재 이슈에서 사용하지 않는 앱 Controller와 설정을 미리 만들지 않는다.

### 공개 경로와 자격 증명을 구분한다

- `/auth/csrf`, `/auth/login`, `/auth/refresh`, `/auth/logout`은 Access Token 없이 Security Filter Chain을 통과한다.
- 공개 경로라는 사실은 자격 증명 없이 Token을 발급하거나 Session을 변경한다는 뜻이 아니다.
- 로그인은 유효한 Provider 증명과 CSRF Token, 재발급은 유효한 Refresh Cookie와 CSRF Token을 요구한다.
- 로그아웃은 CSRF Token을 요구하고 Session 정리와 Cookie 만료를 멱등 처리한다.
- 운영 소스에 임의 사용자 Access Token을 발급하는 테스트 Endpoint, `X-User-Id` 우회와 고정 Token을 추가하지 않는다.

## 선택 이유

Access Token을 기존 Bearer Header로 유지하면 Spring Security Resource Server와 모든 도메인 Controller의 인증 경계를
바꾸지 않는다. 웹 JavaScript가 짧은 수명의 Access Token만 메모리에 보관하고 장기 Refresh Token은 HttpOnly Cookie에
두면 XSS가 발생했을 때 Refresh Token 원문을 직접 읽는 경로를 줄일 수 있다. HttpOnly가 XSS 자체나 악성 요청 실행을
막는 것은 아니므로 Content Security Policy와 입력·출력 안전성은 별도 웹 보안 책임으로 남는다.

Refresh Cookie의 Domain과 Path를 좁히면 장기 자격 증명이 불필요한 요청에 포함되는 범위를 줄일 수 있다. Cookie는
브라우저가 자동 전송하므로 CSRF Token과 정확한 Origin CORS를 함께 결정해야 Token 저장 개선이 요청 위조 위험으로
바뀌지 않는다.

클라이언트 중립 Core와 웹 Adapter를 분리하면 미래 앱을 지원할 때 로그인 Transaction, Provider 검증, Token 발급과
Session Rotation을 복제하지 않는다. 동시에 존재하지 않는 앱 요구를 위해 현재 Endpoint에 런타임 분기를 넣지 않아 웹
계약의 명확성과 안전성을 유지한다.

## 검토한 대안

### Access Token과 Refresh Token을 모두 웹 저장소에 저장한다

백엔드 변경이 가장 작고 기존 RN JSON 계약을 그대로 사용할 수 있다. 그러나 XSS로 JavaScript 실행 권한을 얻으면 장기
Refresh Token 원문까지 직접 읽을 수 있고 페이지 새로고침과 로그아웃 시 저장소 정리 책임도 프론트엔드에 남으므로
선택하지 않았다.

### Access Token과 Refresh Token을 모두 HttpOnly Cookie로 전달한다

JavaScript가 두 Token 원문을 읽지 않는 장점이 있다. 그러나 모든 도메인 API가 Cookie 인증으로 바뀌어 현재 OAuth2
Resource Server의 Bearer 추출 방식과 CSRF 범위가 크게 변경된다. 기존 도메인과 미래 앱이 공통 Bearer API를 사용하는
경계를 유지하기 위해 선택하지 않았다.

### 서버 `HttpSession` 로그인으로 전환한다

브라우저 Cookie와 Spring Security Session 기능을 직접 사용할 수 있다. 하지만 이미 구현한 자체 Access Token, API
Resource Server, `AuthSession`과 앱 재사용 경계를 모두 바꾸며 OAuth2 기반 API 계약을 유지하려는 제품 방향과 맞지 않아
선택하지 않았다.

### `SameSite`와 CORS만 사용하고 CSRF Token을 사용하지 않는다

구현은 단순하다. 그러나 SameSite는 배포 Site 구성과 브라우저 정책에 의존하고, CORS는 응답 읽기 정책이지 모든 요청
전송과 인증 상태 변경을 막는 인증 수단이 아니다. 배포 주소가 아직 확정되지 않은 상태에서 두 보조 경계만으로 Cookie
요청을 보호하지 않기로 했다.

### `clientType` 또는 User-Agent로 웹·앱 응답을 분기한다

Endpoint 수를 줄일 수 있지만 클라이언트가 임의로 보낼 수 있는 값이 장기 자격 증명 전달 방식을 결정하게 된다. Validation,
문서와 테스트 분기가 늘고 잘못된 분기에서 웹 JavaScript에 Refresh Token이 노출될 수 있어 선택하지 않았다.

### 현재 `/auth/login`을 보존하고 `/auth/web/login`을 새로 만든다

기존 RN 계약을 유지할 수 있다. 그러나 실제 RN 제품이 배포되지 않았고 현재 우선 클라이언트는 웹이다. 사용하지 않는
RN HTTP 계약을 운영에 함께 남기면 Refresh Token JSON 노출 경로와 유지 비용만 늘어난다. 현재 `/auth/login`을 웹
계약으로 전환하고 미래 앱이 확정될 때 명시적인 앱 Endpoint를 추가한다.

### cross-site Cookie를 기본값으로 허용한다

프론트엔드와 API를 어떤 Domain에도 배치하기 쉽다. 반면 `SameSite=None; Secure`, credential CORS와 브라우저의
third-party Cookie 제한을 항상 감수한다. 현재 배포 주소가 미정이므로 same-origin·same-site에 적합한 제한적 기본값을
먼저 선택하고 실제 cross-site 요구가 생길 때 재결정한다.

## 영향

### 긍정적 영향

- 웹 JavaScript와 JSON 응답에서 Refresh Token 원문을 제거한다.
- 기존 Bearer Access Token과 도메인 인증 코드를 유지한다.
- 페이지 새로고침, Access Token 만료와 로그아웃의 명시적 흐름이 생긴다.
- Cookie·CSRF·CORS를 하나의 보안 경계로 검증할 수 있다.
- Refresh Rotation과 Session 무효화가 서버 Transaction으로 관리된다.
- 미래 앱은 Provider·Token·Session Core를 재사용할 수 있다.
- 웹과 앱 전달 방식이 임의 Header가 아닌 명시적인 Adapter 계약으로 분리된다.

### 부정적 영향과 위험

- 프론트엔드는 CSRF Token bootstrap, credential Cookie와 Access Token 메모리 상태를 함께 관리해야 한다.
- 페이지 시작 시 `/auth/csrf`와 `/auth/refresh` 요청이 추가된다.
- 프론트엔드와 API가 분리 Origin이면 credential CORS 설정과 환경별 Origin 관리가 필요하다.
- HttpOnly는 Refresh Token 읽기를 제한하지만 XSS가 현재 사용자 권한으로 요청을 보내는 것까지 막지 않는다.
- 로그아웃 뒤 기존 Access Token은 만료 시점까지 유효할 수 있다.
- 단일 활성 `AuthSession` 정책 때문에 다른 브라우저에서 새로 로그인하면 기존 Refresh Token이 무효화된다.
- Rotation 동시 요청에서 한 요청만 성공할 수 있으므로 프론트엔드 Single-flight가 없으면 나머지 요청이 401을 받을 수
  있다.
- 미래 앱 추가 시 앱 전용 Endpoint, DTO와 저장 책임을 새 ADR로 결정해야 한다.

## 미확정 사항

- 실제 웹과 API의 배포 Origin 및 same-origin Reverse Proxy 사용 여부
- 로컬 개발 환경의 정확한 프론트엔드 Origin
- 실제 Kakao JavaScript·REST API 키와 Client Secret의 환경별 설정 위치
- Kakao Redirect URI의 실제 값과 OAuth `state`의 정확한 생성·보관 구현 방식
- Kakao 닉네임 동의·누락 정책과 Provider 장애 오류 계약
- 프론트엔드의 다중 401 Refresh Single-flight 및 원 요청 재시도 정책
- 향후 앱을 다시 지원할지와 앱 전용 Endpoint 경로

위 항목은 실제 값을 추측해 Git에 넣지 않는다. 배포 Origin은 Cookie·CORS 구현을 완료하기 전에 환경 담당자와 확인하며,
cross-site로 확정되면 SameSite 결정을 다시 검토한다.

## 후속 작업

- 이슈 #109에서 Refresh Cookie Properties와 Cookie Writer를 구현한다.
- 로그인 응답 JSON에서 Refresh Token과 만료 시각을 제거하고 Refresh Cookie를 발급한다.
- Refresh 검증·Rotation Service와 `POST /auth/refresh`를 구현한다.
- 멱등 로그아웃과 `POST /auth/logout`을 구현한다.
- CSRF Token 발급 Endpoint와 로그인·재발급·로그아웃 CSRF 검증을 구성한다.
- 배포 Origin이 다르면 정확한 허용 Origin을 사용하는 credential CORS를 구성한다.
- Cookie 속성, CSRF·Origin 실패와 전체 웹 인증 HTTP 인수 테스트를 작성한다.
- 웹 인증 전환 완료 후 Kakao Authorization Code 교환과 사용자 정보 조회 Adapter를 별도 이슈로 구현한다.
- 프론트엔드 이슈에서 Kakao 웹 로그인과 `state` 검증, Access Token 메모리, CSRF bootstrap, Refresh Single-flight와
  로그아웃을 구현한다.

## 참고 자료

- [Spring Security CSRF](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html)
- [Spring Security CORS](https://docs.spring.io/spring-security/reference/servlet/integrations/cors.html)
- [MDN Set-Cookie](https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Headers/Set-Cookie)
- [Kakao 로그인 REST API](https://developers.kakao.com/docs/ko/kakaologin/rest-api)
- [Kakao JavaScript SDK 로그인](https://developers.kakao.com/docs/ko/kakaologin/js)
- [Kakao 보안 권장 사항](https://developers.kakao.com/docs/ko/getting-started/security-guideline)

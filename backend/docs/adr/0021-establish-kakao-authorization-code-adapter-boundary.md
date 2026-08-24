# 0021. Kakao Authorization Code Adapter 경계를 구성한다

- 날짜: 2026-08-24
- 관련 이슈: [#114](https://github.com/woowacourse-teams/2026-ChongChong/issues/114)
- 관련 ADR: [0009. 제공자 독립 소셜 로그인 경계를 구성한다](0009-establish-provider-independent-social-login-boundary.md),
  [0020. 웹 인증 토큰 전달 경계를 구성한다](0020-establish-web-authentication-token-boundary.md)

## 배경

현재 `POST /auth/login`은 웹이 전달한 Kakao Authorization Code를 `SocialLoginCommand`로 변환하고, 제공자 중립 로그인
Core가 `SocialUserInfo`를 기준으로 User·SocialAccount·AuthSession과 총총 Token을 생성한다. 그러나 운영
`SocialLoginClient`가 없어 실제 Kakao가 발급한 Code를 검증하거나 Kakao 사용자를 조회할 수 없다. 테스트
`FakeSocialLoginClient`는 테스트 소스에만 있으므로 현재 운영 로그인은 의도적으로 완성되지 않은 상태다.

웹 Callback, Kakao Developers와 백엔드 Token 요청이 서로 다른 Redirect URI를 사용하면 Code 교환이 실패한다. 또한
OAuth `state`, Spring Security CSRF Token, Kakao Provider Token과 총총 Token은 목적과 생명주기가 달라 각 책임과
노출 범위를 먼저 고정해야 한다.

## 결정

### Kakao REST API의 OAuth 2.0 Authorization Code 흐름을 사용한다

```text
웹 → Kakao 인가 요청 → 웹 Callback(code, state) → POST /auth/login(code)
   → Kakao Token 요청 → /v2/user/me → SocialUserInfo → 기존 총총 로그인 Core
```

- 웹은 Kakao 인가 Endpoint에서 Authorization Code를 받는다.
- 백엔드는 `POST https://kauth.kakao.com/oauth/token`으로 Code를 Kakao Access Token으로 교환한다.
- 백엔드는 Kakao Access Token으로 `GET https://kapi.kakao.com/v2/user/me`를 호출한다.
- Kakao 회원번호 `id`는 문자열 `providerUserId`, `kakao_account.profile.nickname`은 `displayName`으로 변환한다.
- 프로필 이미지 URL은 선택값이며 없으면 `null`을 사용한다.
- Kakao OpenID Connect와 ID Token은 활성화하거나 검증하지 않는다.
- Kakao Access·Refresh Token은 Kakao 사용자 정보 조회에 필요한 범위에서만 사용하고 총총 응답·DB에 저장하지 않는다.

### Redirect URI와 OAuth `state` 책임을 웹 Callback에 둔다

- 로컬 Redirect URI는 `http://localhost:3005/auth/kakao/callback`을 사용한다.
- Kakao Developers 등록값, 웹 인가 요청의 `redirect_uri`, 백엔드 Token 요청의 `redirect_uri`는 완전히 같아야 한다.
- 배포 Redirect URI는 프론트엔드 배포 주소가 확정된 뒤 HTTPS URI로 추가하고 환경별 설정으로 주입한다.
- 웹은 로그인 시도마다 고유한 `state`를 생성·보관하고 Callback 응답의 값과 일치하는지 확인한 뒤 Code를 백엔드로 보낸다.
- OAuth `state`는 Kakao Redirect 요청을 보호하고, `/auth/csrf`가 발급하는 Spring Security CSRF Token은 총총
  `POST /auth/login` 요청을 보호한다. 두 값을 공유하거나 서로 대체하지 않는다.
- 서버 측 `state` 저장소와 PKCE는 현재 계약에 추가하지 않는다.

### Kakao 설정과 비밀값을 서버 환경으로 제한한다

```yaml
auth:
  social:
    kakao:
      rest-api-key: ${AUTH_KAKAO_REST_API_KEY}
      client-secret: ${AUTH_KAKAO_CLIENT_SECRET}
      redirect-uri: ${AUTH_KAKAO_REDIRECT_URI}
      token-uri: ${AUTH_KAKAO_TOKEN_URI:https://kauth.kakao.com/oauth/token}
      user-info-uri: ${AUTH_KAKAO_USER_INFO_URI:https://kapi.kakao.com/v2/user/me}
```

- REST API 키, Client Secret과 Redirect URI는 운영 기본값을 Git에 넣지 않는다.
- Token·사용자 정보 URI는 공식 HTTPS Endpoint를 기본값으로 사용하되 Stub HTTP 서버 테스트에서 교체할 수 있게 한다.
- 필수 설정의 누락·공백을 애플리케이션 시작 시 거부한다.
- URI는 HTTPS만 허용하고, 로컬 Callback과 Stub 서버에 필요한 loopback HTTP만 예외로 허용한다.
- 설정 객체의 문자열 표현에서 REST API 키와 Client Secret을 가린다.
- Client Secret, Authorization Code와 Kakao Token을 로그·예외 메시지·HTTP 응답에 포함하지 않는다.

### 닉네임은 신규 가입에 필요한 값으로 취급한다

- Kakao Developers에서 닉네임 동의항목을 사용한다.
- 사용자 정보에 회원번호 또는 동의받은 닉네임이 없으면 임의 값이나 이메일을 대신 만들지 않고 공통 소셜 인증 실패로
  처리한다.
- 이메일은 현재 사용자 식별이나 기존 계정 병합에 사용하지 않는다.
- 프로필 이미지는 총총 로그인 성공의 필수값으로 취급하지 않는다.

### Kakao 오류는 Provider Adapter 안에서 공통 인증 실패로 변환한다

- Kakao 4xx·5xx, Timeout, 읽을 수 없는 응답과 필수 사용자 정보 누락은 Adapter 밖으로 Kakao DTO나 오류 본문을
  노출하지 않는다.
- 외부에는 기존 `SOCIAL_AUTHENTICATION_FAILED` 경계를 사용한다.
- Provider 호출은 DB Transaction 밖에서 끝내고, 성공한 `SocialUserInfo`만 기존 Transaction Core에 전달한다.
- 이번 이슈에서는 재시도, Circuit Breaker, Provider Token 갱신과 프로필 주기적 동기화를 구현하지 않는다.

## 선택 이유

현재 제품은 로그인 뒤 Kakao API를 지속적으로 사용하지 않으므로 Authorization Code를 한 번 검증하고 Kakao 회원번호와
닉네임만 얻으면 총총 자체 Token·Session 흐름으로 전환할 수 있다. 이 방식은 이미 구현한 제공자 중립 Core와 웹
Access JSON·Refresh HttpOnly Cookie 계약을 변경하지 않는다.

환경 설정을 별도 타입으로 검증하면 키 누락이나 잘못된 Redirect URI를 실제 로그인 요청 시점보다 앞서 발견할 수 있다.
외부 Endpoint를 주입 가능하게 두면 실제 비밀값이나 Kakao 네트워크 없이 요청 형식과 오류 처리를 자동화할 수 있다.

## 검토한 대안

### Kakao JavaScript SDK와 ID Token을 검증한다

웹 SDK를 사용하면 브라우저 연동 기능을 활용할 수 있지만 현재 필요한 결과는 Authorization Code이며, Kakao OIDC와 ID
Token 검증을 추가하면 nonce, 공개키, Claim 검증 책임이 늘어난다. 현재 REST 인가 Endpoint와 사용자 정보 조회로 제품
요구를 충족하므로 선택하지 않았다.

### Kakao Redirect를 백엔드 Endpoint로 받는다

서버가 `state`와 Code를 함께 처리할 수 있지만 현재 HTTP 계약은 웹 Callback이 `state`를 검증하고 Code만
`POST /auth/login`으로 전달한다. 백엔드 Callback, 브라우저 Redirect 응답과 서버 측 `state` 저장소를 새로 만들지 않기
위해 선택하지 않았다.

### Kakao Provider Token을 DB에 저장한다

향후 Kakao API를 계속 호출하기는 쉽지만 현재 그런 제품 요구가 없다. 외부 장기 자격 증명의 암호화, 갱신, 폐기와 유출
대응 책임만 늘어나므로 저장하지 않는다.

### 실패한 Kakao 요청을 자동 재시도한다

일시 장애의 성공률을 높일 수 있지만 Authorization Code는 짧게 살고 한 번만 사용할 수 있어 무분별한 재시도는 실패
의미를 흐릴 수 있다. 실제 장애와 멱등성 요구를 관찰하기 전에는 도입하지 않는다.

## 영향

### 긍정적 영향

- 실제 Kakao 계정이 기존 제공자 중립 로그인·Token·Session Core에 연결된다.
- Kakao 비밀값과 Provider Token의 생존 범위가 HTTP Adapter 내부로 제한된다.
- 실제 Kakao 네트워크 없이 Stub 서버로 요청과 실패 경계를 검증할 수 있다.
- 미래 Provider를 추가해도 Kakao DTO와 HTTP 규칙이 Core로 전파되지 않는다.

### 부정적 영향과 위험

- 프론트엔드가 OAuth `state` 생성·보관·검증을 올바르게 구현해야 한다.
- 실제 로그인은 Kakao 가용성과 응답 형식에 의존한다.
- 배포 프론트엔드 주소가 바뀌면 Kakao Developers, 웹과 백엔드 Redirect URI를 함께 변경해야 한다.
- 닉네임 동의가 없으면 현재 별도 이름 입력 화면 없이 로그인을 완료할 수 없다.

## 미확정 사항

- 운영 프론트엔드 배포 주소와 HTTPS Redirect URI
- Kakao 프로필 이미지를 제품 화면에서 실제로 사용할지 여부

두 항목은 현재 Adapter의 필수 로그인 성공 경계를 바꾸지 않는다. 운영 Redirect URI는 배포 주소가 확정된 뒤 환경별로
추가하며, 프로필 이미지가 없어도 로그인은 허용한다.

## 후속 작업

- Authorization Code를 Kakao Token으로 교환하는 HTTP Client를 구현한다.
- Kakao 사용자 정보를 `SocialUserInfo`로 변환하는 운영 `SocialLoginClient`를 구현한다.
- Stub Kakao 서버를 사용해 성공·4xx·5xx·Timeout·잘못된 응답을 검증한다.
- 실제 Kakao 개발 앱으로 로컬 로그인·보호 API·재발급·로그아웃을 수동 검증한다.
- PKCE, 서버 측 `state`, OIDC, 재시도와 Circuit Breaker는 실제 요구가 생기면 별도 이슈와 ADR로 검토한다.

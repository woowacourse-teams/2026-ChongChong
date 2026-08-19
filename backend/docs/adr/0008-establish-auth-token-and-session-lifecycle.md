# 0008. 인증 토큰 발급과 세션 생명주기를 정의한다

- 날짜: 2026-08-19
- 관련 이슈: [#65](https://github.com/woowacourse-teams/2026-ChongChong/issues/65)

## 배경

[Access Token 인증 경계 ADR](0007-establish-access-token-authentication-boundary.md)에서는 클라이언트가 전달한 총총
Access Token의 서명과 표준 Claim을 검증하고, `sub`를 내부 사용자 ID로 변환하는 방식을 결정했다. 그러나 현재 운영
코드에는 총총 Access Token과 Refresh Token을 발급하거나 Refresh Token의 유효 상태를 저장하는 기능이 없다.

소셜 로그인과 자동 회원가입, Token 재발급 및 로그아웃은 모두 총총 자체 Token을 발급하고 폐기하는 공통 기반을
필요로 한다. 이 기반을 소셜 제공자 연동과 함께 구현하면 제공자 통신, 사용자 생성, Token 보안 정책과 동시성 제어가 한
변경에 섞여 각각의 책임을 독립적으로 검증하기 어렵다.

따라서 이 이슈에서는 이미 존재하는 총총 사용자 ID를 기준으로 Access/Refresh Token을 발급하고, 사용자당 하나의
활성 Refresh Session을 저장하는 내부 기반을 먼저 구성한다. 실제 로그인, 재발급 및 로그아웃 API는 이 기반을 사용하는
후속 작업으로 분리한다.

## 결정

### Token의 역할

- Access Token은 총총 API 요청에서 현재 사용자를 증명하는 짧은 수명의 자격 증명이다.
- Refresh Token은 Access Token을 다시 발급받기 위한 긴 수명의 자격 증명이다.
- Access Token은 서버에 발급 상태를 저장하지 않는 JWT로 사용한다.
- Refresh Token은 서버가 현재 유효 상태를 `auth_sessions`에서 관리하는 불투명 Token으로 사용한다.
- 총총의 HTTP Session은 인증 상태 저장에 사용하지 않는다.

### Access Token 발급

- Access Token은 Spring Security의 `JwtEncoder`를 사용해 발급한다.
- 별도의 Auth JWT 라이브러리를 추가하지 않는다.
- 서명은 1차 인증 경계와 동일한 HMAC SHA-256(`HS256`)을 사용한다.
- 발급기와 검증기는 동일한 Base64 서명 키와 `issuer`, `audience` 설정을 사용한다.
- 서명 키를 Base64로 복호화하고 최소 256비트인지 확인하는 책임은 발급기와 검증기가 공유한다.
- Access Token의 기본 만료 시간은 30분이며 환경 설정으로 변경할 수 있다.
- 발급·만료 시각은 주입한 `Clock`의 `Instant`를 기준으로 계산한다.
- `Clock`의 기본 운영 값은 UTC 기준 시스템 시계다.

Access Token에는 1차 ADR에서 결정한 다음 표준 Claim을 포함한다.

| Claim | 값 |
| --- | --- |
| `iss` | 설정된 총총 Token 발급자 |
| `aud` | 설정된 총총 API audience |
| `sub` | 양의 정수인 총총 내부 `users.id`의 문자열 표현 |
| `iat` | 발급 시각 |
| `exp` | 발급 시각에 Access Token 만료 시간을 더한 시각 |
| `jti` | Token마다 새로 생성한 고유 식별자 |

Access Token에는 다음 값을 넣지 않는다.

- `MEMBER`, `LEADER`와 같은 스터디별 역할
- 닉네임, 프로필 이미지, 이메일과 같은 개인정보
- 소셜 제공자의 사용자 ID와 Access/Refresh Token
- 총총 Refresh Token 또는 그 해시

### Refresh Token 생성

- Refresh Token은 Claim을 담지 않는 불투명한 무작위 문자열로 발급한다.
- Java `SecureRandom`으로 32바이트(256비트)의 무작위 값을 생성한다.
- 생성한 바이트는 URL-safe Base64 방식으로 padding 없이 인코딩한다.
- Refresh Token의 기본 만료 시간은 30일이며 환경 설정으로 변경할 수 있다.
- Access Token과 Refresh Token의 만료 시간은 서로 다른 설정으로 관리한다.
- 원문 Refresh Token은 발급 결과를 만드는 애플리케이션 경계까지만 전달하고 로그, 예외 메시지와 영속 Entity에 넣지
  않는다.

### Refresh Token 해시

- 데이터베이스에는 Refresh Token 원문 대신 SHA-256 해시를 저장한다.
- 해시 바이트는 소문자 16진수 64자리 문자열로 표현한다.
- 같은 원문은 같은 해시를 만들고, 원문을 전달받은 재발급 요청은 같은 방식으로 해시한 뒤 저장 값과 비교한다.
- Refresh Token은 256비트의 무작위 엔트로피를 가지므로 비밀번호처럼 사람이 정한 낮은 엔트로피 값을 전제로 하는
  느린 비밀번호 해시를 사용하지 않는다.
- 원문과 해시가 타입 또는 변수 이름에서 명확히 구분되도록 한다.

### 인증 Session 저장

`auth_sessions`는 다음 정보를 저장한다.

| 컬럼 | 규칙 |
| --- | --- |
| `id` | Session 식별자 |
| `user_id` | 총총 내부 사용자, `NOT NULL`, `UNIQUE` |
| `refresh_token_hash` | SHA-256 해시 문자열, `NOT NULL`, `UNIQUE`, 길이 64 |
| `expires_at` | Refresh Token 만료 시각, `NOT NULL` |
| `created_at` | 기존 `BaseEntity` 규칙 사용 |
| `updated_at` | 기존 `BaseEntity` 규칙 사용 |

- `AuthSession`은 `auth` 도메인이 소유한다.
- `AuthSession`은 `User`를 참조하지만 `User`가 `AuthSession` 컬렉션을 갖는 양방향 관계는 만들지 않는다.
- `user_id` 유일 제약으로 MVP에서 사용자당 활성 Refresh Session을 하나만 허용한다.
- 현재 ERD는 향후 사용자와 Session의 1:N 확장을 허용하는 형태지만, 이번 구현에서는 이슈 #65의 단일 활성 Session
  성공 기준을 우선해 `user_id` 유일 제약을 적용한다.
- `refresh_token_hash` 유일 제약으로 같은 Refresh Token 해시가 여러 Session에 저장되지 않게 한다.
- 현재 프로젝트의 스키마 관리 방식인 JPA `ddl-auto`를 유지한다. 이 이슈에서 Flyway 등 별도 마이그레이션 도구를
  함께 도입하지 않는다.

### 최초 Token 발급과 Session 교체

Token 발급 서비스는 다음 순서로 동작한다.

1. 전달받은 사용자 ID가 양수인지 확인한다.
2. 해당 `User`를 비관적 쓰기 잠금으로 조회해 같은 사용자에 대한 동시 발급 요청을 직렬화한다.
3. 새 Access Token과 원문 Refresh Token을 생성한다.
4. Refresh Token 원문을 SHA-256으로 해시한다.
5. 기존 `AuthSession`이 없으면 새로 저장하고, 있으면 해시와 만료 시각을 새 값으로 교체한다.
6. Transaction이 정상적으로 끝난 뒤 호출자에게 Token 쌍과 각각의 만료 시각을 반환한다.

두 번째 Token 쌍을 발급하면 이전 Refresh Token의 해시는 더 이상 현재 Session에 남지 않는다. 따라서 후속 재발급
기능에서 이전 Refresh Token을 현재 Token으로 인정하지 않는다.

Token 생성 중 오류가 발생하거나 Session 저장에 실패하면 Token 결과를 호출자에게 반환하지 않는다. Session 교체는
하나의 Transaction에서 처리한다.

### 동시 발급

- 동일 사용자에 대한 Token 발급은 `User` 행의 비관적 쓰기 잠금으로 직렬화한다.
- `user_id` 데이터베이스 유일 제약을 최종 안전장치로 함께 사용한다.
- 서로 다른 사용자의 Token 발급은 서로의 Session을 교체하거나 잠그지 않는다.
- 동시성 보장은 Mock Repository 단위 테스트가 아니라 실제 데이터베이스와 Transaction을 사용하는 통합 테스트로
  확인한다.

### Token 발급 서비스의 경계

- 이번 이슈의 Token 발급 서비스는 이미 존재하는 총총 내부 사용자 ID를 입력으로 받는다.
- 외부 클라이언트가 임의의 사용자 ID로 Token을 발급받을 수 있는 HTTP API를 제공하지 않는다.
- 운영 코드에 `/dev/token`, 고정 사용자 로그인 또는 `X-User-Id` 인증 우회 기능을 만들지 않는다.
- 후속 소셜 로그인 서비스는 소셜 제공자 검증과 사용자 조회·생성을 완료한 뒤 이 Token 발급 서비스를 호출한다.
- Controller와 도메인 서비스가 JWT를 직접 생성하거나 파싱하지 않는다.

### 후속 재발급과 로그아웃에서 지킬 원칙

이번 이슈에서 재발급과 로그아웃 API를 구현하지는 않지만 후속 작업은 다음 원칙을 따른다.

- 정상 재발급 때마다 Access Token과 Refresh Token을 모두 새로 발급한다.
- 새 Refresh Token을 저장하면 직전 Refresh Token은 더 이상 사용할 수 없다.
- 현재 Session 해시만 저장하는 이번 구조는 교체된 이전 Token을 거부할 수 있지만, 어떤 Token 계열에서 과거 Token이
  재사용됐는지를 식별하지는 못한다.
- Token family 또는 사용 이력을 이용한 적극적인 재사용 탐지는 후속 정책으로 결정한다.
- 로그아웃은 현재 사용자의 `AuthSession`을 제거해 이후 재발급을 차단한다.
- Stateless Access Token은 로그아웃 시 서버에서 즉시 폐기하지 않으며 최대 30분의 남은 만료 시간 동안 유효할 수 있다.

### 웹과 앱의 Refresh Token 전달

이번 이슈의 내부 발급 결과는 원문 Refresh Token을 호출자에게 반환할 수 있지만, HTTP 응답에서 클라이언트로 전달하는
방법은 결정하지 않는다.

- 웹에서 `Secure`, `HttpOnly` Cookie를 사용할지
- 앱에서 응답 본문과 운영체제 보안 저장소를 사용할지
- Cookie의 `SameSite`, `Path`, `Domain`과 만료 속성
- Cookie 사용 시 CSRF 방어 방식

위 항목은 실제 로그인·재발급 API를 구현하기 전에 별도 후속 결정으로 확정한다. Cookie를 인증 정보 전달에 사용하면서
현재의 `csrf.disable()`을 근거 없이 유지하지 않는다.

## 선택 이유

짧은 수명의 JWT Access Token은 각 API 요청에서 데이터베이스 Session 조회 없이 1차 인증 경계로 검증할 수 있다.
Refresh Token만 서버 Session에서 관리하면 재발급과 로그아웃을 제어하면서 모든 Access Token의 발급 상태를 저장하는
비용을 피할 수 있다.

발급과 검증에 Spring Security의 `JwtEncoder`, `JwtDecoder`와 동일한 서명 설정을 사용하면 두 구현의 알고리즘, 키와
Claim 규칙이 달라지는 위험을 줄일 수 있다. `Clock`을 주입하면 실제 시간을 기다리지 않고 발급 및 만료 경계를 안정적으로
테스트할 수 있다.

Refresh Token을 256비트 무작위 값으로 만들고 SHA-256 해시만 저장하면 데이터베이스가 노출되더라도 저장된 값 자체를
Refresh Token으로 사용할 수 없다. 사용자당 하나의 Session과 사용자 행 잠금은 MVP의 단일 로그인 정책을
데이터베이스 제약과 Transaction 수준에서 함께 보장한다.

## 검토한 대안

### Access Token을 직접 조합하거나 별도 JWT 라이브러리로 발급

Token 생성 형식을 자유롭게 제어할 수 있지만 1차 검증에 사용하는 Spring Security 설정과 알고리즘·Claim 처리가
분리된다. 같은 Auth Access Token에 두 JWT 구현을 사용할 필요가 없어 선택하지 않았다. 기존 JJWT 사용 코드는 스터디
초대 Token이라는 별도 책임으로 유지한다.

### Refresh Token도 JWT로 발급

만료 시각과 사용자 정보를 Token 자체에 담을 수 있지만 서버가 현재 유효 Session과 Rotation 상태를 관리하려면 결국
저장소가 필요하다. Refresh Token Claim을 클라이언트가 해석할 이유가 없고 노출할 정보만 늘어나므로 불투명 Token을
선택했다.

### Refresh Token 원문 저장

원문 비교가 단순하지만 데이터베이스가 노출되면 공격자가 저장 값을 그대로 사용해 Token을 재발급할 수 있다. 해시
계산 비용보다 유출 위험이 크므로 선택하지 않았다.

### 사용자당 여러 활성 Session 허용

기기별 로그인 유지와 선택적 로그아웃에 유리하지만 Session 식별, 기기 관리, 전체 로그아웃과 재사용 탐지 정책이 추가로
필요하다. 현재 MVP 방향은 사용자당 활성 Session 하나이므로 선택하지 않았다. 다중 기기 요구가 확정되면 `user_id`
유일 제약과 Session 모델을 새 ADR 및 DB 변경으로 재검토한다.

### 애플리케이션 조회만으로 단일 Session 보장

Session 저장 전에 기존 행을 조회하고 삭제하는 방식은 단순하지만 동일 사용자의 동시 요청이 모두 기존 Session이 없다고
판단할 수 있다. 사용자 행 잠금과 데이터베이스 유일 제약 없이 애플리케이션의 사전 조회만 사용하는 방식은 선택하지
않았다.

### 서버 HTTP Session 인증

서버가 로그인 상태를 즉시 폐기하기 쉽지만 웹과 이후 모바일 앱이 동일한 Bearer Token API를 사용하고 총총 자체
Access/Refresh Token을 발급한다는 1차 결정과 맞지 않아 선택하지 않았다.

## 영향

### 긍정적 영향

- 실제 소셜 제공자 연동 전에 총총 Token 발급 규칙을 독립적으로 구현하고 검증할 수 있다.
- 발급한 Access Token을 기존 Resource Server 경계로 바로 검증할 수 있다.
- Refresh Token 원문이 데이터베이스에 남지 않는다.
- 사용자당 하나의 활성 Refresh Session을 데이터베이스 제약으로 보장한다.
- 동시 발급에서도 같은 사용자의 Session이 중복 생성되는 것을 막을 수 있다.
- 시간 의존 로직을 실제 대기 없이 테스트할 수 있다.
- 로그인, 재발급과 로그아웃이 공통 Token 발급 서비스를 재사용할 수 있다.

### 부정적 영향과 위험

- 사용자당 하나의 Session만 허용하므로 다른 기기에서 로그인하면 기존 기기의 Refresh Token이 무효화된다.
- 동일 사용자 Token 발급 중에는 사용자 행 잠금 비용이 발생한다.
- 로그아웃이나 Session 교체 직후에도 기존 Access Token은 최대 30분 동안 유효할 수 있다.
- 단일 현재 해시만 저장하므로 교체된 과거 Refresh Token의 계열과 재사용 사건을 식별할 수 없다.
- HMAC 서명 키가 노출되면 공격자가 유효한 Access Token을 만들 수 있으므로 운영 키 보관과 교체 전략이 필요하다.
- JPA `ddl-auto`에 의존하므로 운영 데이터가 생긴 뒤 제약이나 컬럼을 변경할 때 별도의 안전한 DB 변경 전략이 필요하다.

## 미확정 사항

- 웹과 앱에서 Refresh Token을 전달하는 최종 방식
- 웹 Refresh Cookie의 이름, `Secure`, `HttpOnly`, `SameSite`, `Path`, `Domain`과 만료 속성
- Cookie 기반 Refresh 요청의 CSRF 방어 방식
- 재발급 실패의 외부 에러 코드와 상세 메시지
- Token family 또는 이력을 이용한 Refresh Token 재사용 탐지와 대응 범위
- 다중 기기 Session 지원 시 Session 식별과 선택적 로그아웃 정책
- HMAC 서명 키 교체 시 기존 Access Token과의 전환 방식
- 탈퇴·정지 사용자에게 남아 있는 Access Token의 처리 방식

## 후속 작업

- Access/Refresh Token 만료 시간을 설정 값으로 추가한다.
- 발급기와 검증기가 공유할 HMAC `SecretKey` 구성을 분리한다.
- Spring Security `JwtEncoder`와 Access Token 발급기를 구현한다.
- Refresh Token 생성기와 SHA-256 해시 컴포넌트를 구현한다.
- `AuthSession` Entity와 Repository 및 데이터베이스 제약을 구현한다.
- 사용자 행 잠금을 사용하는 Token 쌍 발급 서비스를 구현한다.
- Claim, 만료, 해시, Session 교체와 동시 발급을 테스트한다.
- 발급한 Access Token이 기존 `JwtDecoder`를 통과하는지 검증한다.
- 후속 이슈에서 소셜 로그인과 자동 회원가입을 구현한다.
- 후속 이슈에서 Rotation, 재발급, 로그아웃과 Cookie/CSRF 정책을 구현한다.

## 참고 자료

- [Spring Security OAuth2](https://docs.spring.io/spring-security/reference/7.0/servlet/oauth2/index.html)
- [Spring Security CSRF](https://docs.spring.io/spring-security/reference/7.1-SNAPSHOT/features/exploits/csrf.html)
- [RFC 9700: OAuth 2.0 Security Best Current Practice](https://www.rfc-editor.org/rfc/rfc9700.html)

# 0007. Access Token 인증 경계를 구성한다

- 날짜: 2026-08-18
- 관련 이슈: [#54](https://github.com/woowacourse-teams/2026-ChongChong/issues/54)

## 배경

공지, 과제와 스터디 API는 요청을 보낸 총총 사용자를 식별하고, 해당 사용자의 스터디 가입 여부와 역할을 기준으로
권한을 판단해야 한다. 소셜 로그인과 Refresh Token 생명주기가 아직 구현되지 않았다는 이유로 각 도메인이 임시 사용자
식별자를 사용하면, 이후 인증 도입 시 Controller와 테스트를 반복해서 수정해야 하고 도메인마다 인증 경계가 달라질 수 있다.

도메인 기능을 병렬로 개발하려면 실제 소셜 제공자 연동보다 먼저 다음 공통 기준이 필요하다.

- 총총 Access Token을 어떤 컴포넌트가 검증하는가
- 검증된 토큰에서 현재 사용자 ID를 어떻게 복원하는가
- 인증 정보가 웹 계층과 서비스 계층 사이를 어떻게 통과하는가
- 인증 실패와 인가 실패를 어떤 응답으로 변환하는가
- 스터디별 역할을 Access Token과 데이터베이스 중 어디에서 판단하는가

## 결정

### Spring Security Resource Server 사용

- 총총 API는 `Authorization: Bearer <access-token>` 형식으로 Access Token을 전달받는다.
- Spring Security OAuth2 Resource Server의 JWT 지원을 사용해 토큰을 검증하고 `SecurityContext`를 구성한다.
- 직접 만든 Servlet Filter에서 JWT 문자열을 파싱하거나 검증하지 않는다.
- 서버 세션에 인증 상태를 저장하지 않고 요청마다 Access Token을 검증하는 Stateless 방식을 사용한다.

### Access Token 검증

- Access Token은 총총 백엔드가 발급하는 JWT로 정의한다.
- 1차 인증 기반에서는 HMAC SHA-256 대칭키 서명을 사용한다.
- 서명 키는 최소 256비트 이상의 값을 사용하고 환경변수 또는 외부 설정으로 주입한다. 소스 코드, 테스트 코드와
  저장소 설정 파일에 운영 키를 기록하지 않는다.
- `JwtDecoder`는 서명과 `exp`, `iss`, `aud`를 검증한다.
- 발급자와 대상 API 값은 배포 환경별 설정으로 관리한다.

Access Token에는 다음 표준 Claim을 사용한다.

| Claim | 용도 |
| --- | --- |
| `iss` | 총총 토큰 발급자 |
| `aud` | 토큰을 사용할 총총 API |
| `sub` | 총총 내부 사용자 ID의 문자열 표현 |
| `iat` | 토큰 발급 시각 |
| `exp` | 토큰 만료 시각 |
| `jti` | Access Token 고유 식별자 |

`sub`에는 소셜 제공자의 사용자 ID가 아니라 총총 `users.id`를 넣는다. Access Token에는 스터디별 역할, 닉네임,
프로필 이미지, 이메일, 소셜 제공자 Access Token과 같은 값은 넣지 않는다.

### 현재 사용자 전달

- JWT의 `sub`를 양의 정수인 총총 내부 사용자 ID로 변환한다.
- 웹 계층은 검증된 사용자 ID를 표현하는 인증 전용 타입을 통해 현재 사용자를 전달받는다.
- Controller는 현재 사용자 ID를 서비스 메서드의 명시적인 인자로 전달한다.
- 서비스와 도메인 객체는 `SecurityContext`, `Authentication`과 JWT에 직접 의존하지 않는다.
- `sub`가 없거나 내부 사용자 ID로 변환할 수 없으면 인증 실패로 처리한다.

### 공개 경로와 보호 경로

- `/auth/login`, `/auth/refresh`는 총총 Access Token 없이 접근할 수 있는 공개 경로로 예약한다.
- 공개 경로로 명시하지 않은 API는 기본적으로 인증을 요구한다.
- `/auth/refresh`가 공개 경로라는 것은 아무 자격 증명 없이 토큰을 발급한다는 의미가 아니다. 후속 구현에서 유효한
  Refresh Token을 별도의 자격 증명으로 검증한다.
- 1차 기반은 Bearer Token만 인증 수단으로 사용하므로 CSRF 보호를 비활성화한다. Refresh Token을 Cookie로 전달하는
  시점에 Cookie 범위와 CSRF 방어 방식을 다시 결정한다.

### 인증·인가 실패 응답

- Access Token이 없거나 유효하지 않은 경우 `AuthenticationEntryPoint`에서 `401 Unauthorized`로 변환한다.
- 인증된 사용자가 필요한 권한을 갖지 못한 경우 `AccessDeniedHandler`에서 `403 Forbidden`으로 변환한다.
- 두 응답은 [에러 응답 ADR](0004-standardize-error-response.md)의 `code`, `message` 형식과
  [공통 예외 타입 ADR](0006-design-common-exception-types.md)의 `ErrorCode`, `ErrorResponse`를 사용한다.
- Security Filter Chain에서 발생한 실패는 Controller 진입 전에 발생하므로 `GlobalExceptionHandler`가 아니라
  위 두 컴포넌트가 응답을 작성한다.

### 스터디별 역할 판단

- `MEMBER`, `LEADER`는 전역 권한이 아니라 스터디별 역할이므로 Access Token Claim에 넣지 않는다.
- Access Token은 현재 사용자의 내부 ID만 증명한다.
- 스터디별 인가는 후속 작업에서 현재 사용자 ID와 `studyId`를 사용해 `study_members`를 조회하여 판단한다.
- 공지, 과제와 제출물처럼 경로에 하위 리소스 ID가 함께 있으면 해당 리소스가 요청한 스터디에 속하는지도 검증한다.

### 테스트 지원

- 테스트에서만 사용할 수 있는 JWT 생성 도구를 제공하여 원하는 내부 사용자 ID로 인증 요청을 만들 수 있게 한다.
- 테스트용 서명 키는 테스트 설정에만 두고 운영 프로필에서 개발용 토큰 발급 API나 사용자 ID 우회 기능을 활성화하지
  않는다.
- 정상 토큰, 누락된 토큰, 만료 토큰, 변조 토큰, 잘못된 발급자와 대상 API, 권한 부족 흐름을 검증한다.

### 이번 결정에 포함하지 않는 범위

다음 항목은 인증 경계를 사용하는 후속 작업에서 별도로 결정하고 구현한다.

- Google 등 소셜 제공자와 Authorization Code 교환
- 로그인과 자동 회원가입
- Refresh Token 발급, 저장, Rotation과 재사용 탐지
- `social_accounts`, `auth_sessions` 영속성 모델
- 로그아웃과 회원 탈퇴
- 웹 Refresh Cookie와 CSRF 세부 정책
- `study_members`를 조회하는 스터디별 인가 서비스

## 선택 이유

Spring Security Resource Server를 사용하면 JWT 서명과 표준 Claim 검증, Bearer Token 추출과 인증 컨텍스트 구성을
검증된 프레임워크 컴포넌트에 맡길 수 있다. 커스텀 코드를 현재 사용자 변환과 프로젝트 에러 응답 연결에 제한하면 인증
실수와 중복을 줄일 수 있다.

소셜 로그인보다 Access Token 검증 경계를 먼저 구성하면 도메인 담당자가 실제 인증 사용자 ID를 전제로 Controller와
서비스를 설계할 수 있다. 테스트에서도 같은 인증 경계를 사용하므로 이후 Google 연동과 Refresh Token이 추가되어도
도메인 코드의 변경 범위를 줄일 수 있다.

현재는 하나의 백엔드가 토큰을 발급하고 검증하므로 HMAC SHA-256 방식이 키 생성과 운영 구성이 단순하다. 사용자 ID만
토큰에 넣고 스터디 역할을 요청 시점에 조회하면 역할 변경, 탈퇴와 방출이 Access Token 만료 전에도 권한 판단에 반영된다.

## 검토한 대안

### 직접 만든 JWT Filter 사용

JWT 파싱과 인증 객체 구성을 자유롭게 제어할 수 있지만 Bearer Token 추출, 예외 변환과 SecurityContext 설정을 직접
구현해야 한다. 표준 기능을 반복 구현하면서 검증 누락과 프레임워크 처리 순서 오류가 발생할 가능성이 있어 선택하지 않았다.

### 임시 사용자 ID Header 사용

`X-User-Id` 같은 Header를 신뢰하면 도메인 개발을 빠르게 시작할 수 있지만 클라이언트가 임의의 사용자를 가장할 수 있다.
임시 방식이 운영 설정에 남거나 도메인 테스트가 실제 인증 경계를 검증하지 못할 위험이 있어 선택하지 않았다.

### Access Token에 스터디 역할 포함

인가 시 데이터베이스 조회를 줄일 수 있지만 한 사용자의 역할은 스터디마다 다르고 탈퇴와 방출로 변경될 수 있다. 토큰이
만료될 때까지 오래된 역할이 유지되고 Claim 구조가 복잡해지므로 선택하지 않았다.

### 비대칭키 서명 사용

토큰 발급자와 검증자가 분리되거나 여러 서비스가 공개키로 토큰을 검증할 때 유리하다. 현재는 하나의 백엔드가 발급과
검증을 모두 담당하므로 초기 키 관리 비용이 더 크다. 서비스 분리나 외부 검증 주체가 생기면 새 ADR로 재검토한다.

### 서버 세션 인증 사용

서버에서 세션을 즉시 폐기하기 쉽지만 웹과 이후 모바일 앱이 같은 API를 사용하고 총총 자체 Access/Refresh Token을
발급한다는 요구와 맞지 않아 선택하지 않았다.

## 영향

### 긍정적 영향

- 도메인 API가 공통 방식으로 현재 사용자 ID를 전달받을 수 있다.
- 인증 실패와 인가 실패를 각각 `401`, `403`으로 일관되게 응답할 수 있다.
- 도메인 서비스가 Spring Security와 JWT에 직접 결합되지 않는다.
- 스터디 역할 변경을 토큰 재발급 없이 데이터베이스 기준으로 판단할 수 있다.
- 실제 소셜 로그인 완성 전에도 도메인 인증 흐름과 인수 테스트를 개발할 수 있다.

### 부정적 영향과 위험

- 모든 보호 API 요청에서 JWT 서명과 Claim 검증 비용이 발생한다.
- HMAC 키가 노출되면 공격자가 유효한 토큰을 만들 수 있으므로 키 보관과 교체가 중요하다.
- 실제 로그인 구현 전에는 테스트용 JWT 없이는 보호 API를 호출하기 어렵다.
- Refresh Token을 Cookie로 도입할 때 현재 CSRF 설정을 다시 검토해야 한다.
- 사용자 삭제나 정지 상태를 즉시 반영해야 하는 API는 JWT 검증 외에 사용자 상태 조회가 필요할 수 있다.

## 미확정 사항

- Access Token의 정확한 만료 시간
- 배포 환경별 `iss`, `aud` 값
- HMAC 키 교체 시 기존 토큰을 함께 검증할 수 있는 전환 방식
- 인증 실패와 인가 실패에 사용할 외부 에러 코드와 메시지
- 회원 탈퇴 또는 계정 정지 후 남아 있는 Access Token의 처리 방식
- Refresh Cookie를 도입할 때의 Cookie 속성과 CSRF 방어 방식

## 후속 작업

- Spring Security와 OAuth2 Resource Server 의존성을 추가한다.
- JWT 설정과 `JwtDecoder`를 구성하고 서명 및 표준 Claim 검증 테스트를 작성한다.
- 검증된 `sub`를 현재 사용자 타입으로 변환해 웹 계층에 제공한다.
- `AuthenticationEntryPoint`와 `AccessDeniedHandler`를 기존 에러 응답 규격에 연결한다.
- 도메인 인수 테스트에서 재사용할 테스트 JWT 생성 도구를 제공한다.
- 로그인과 Refresh Token 생명주기, 스터디별 인가는 각각 후속 ADR과 구현 작업에서 구체화한다.

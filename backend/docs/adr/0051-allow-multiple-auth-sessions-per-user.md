# 0051. 사용자당 여러 인증 세션을 허용한다

- 날짜: 2026-10-18
- 관련 이슈: [#526](https://github.com/woowacourse-teams/2026-ChongChong/issues/526)
- 대체하는 ADR: [0008](0008-establish-auth-token-and-session-lifecycle.md)의 사용자당 단일 활성 Session 및 재로그인 시 Session 교체 정책,
  [0020](0020-establish-web-authentication-token-boundary.md)의 단일 활성 Session 전제
- 관련 ADR: [0034](0034-delete-user-account-in-one-transaction.md)의 탈퇴 시 인증 세션 삭제,
  [0028](0028-adopt-spec-first-api-contracts.md)의 명세 우선 API 계약

## 배경

현재는 `auth_sessions.user_id`에 유일 제약을 두고, 같은 사용자가 다시 로그인하면 기존 Session의 Refresh Token
해시와 만료 시각을 교체한다. 휴대폰에서 로그인하면 노트북의 Refresh Token으로는 더 이상 재발급할 수 없다.

웹은 Access Token을 JavaScript 메모리에만 보관하므로 새로고침하면 해당 Token이 사라진다. 노트북을 새로고침한 뒤
Refresh Cookie로 인증을 복구하려 해도 이미 무효화된 Token이므로 로그인 화면으로 이동한다. 노트북과 휴대폰을
함께 사용하는 사용자가 두 브라우저에서 로그인 상태를 유지할 수 있도록 단일 세션 정책을 변경한다.

## 결정

### 로그인마다 독립적인 Session을 생성한다

- 한 사용자가 여러 `AuthSession`을 가질 수 있도록 `User`와 `AuthSession`의 관계를 1:N으로 허용한다.
- 로그인에 성공할 때마다 새로운 Session을 저장한다. 기존 Session을 조회하거나 Refresh Token을 교체하지 않는다.
- Session은 기존의 사용자 참조, Refresh Token 해시, 만료 시각과 감사 시각을 사용한다.
- Session을 식별하는 자격 증명은 브라우저가 보관한 Refresh Token이다. 이번 작업에서 `deviceId`나 기기명을
  추가로 수집하지 않는다.
- 브라우저마다 Cookie 저장 공간이 독립적이면 서로 다른 Session을 사용한다. 같은 브라우저 프로필의 탭은
  Refresh Cookie를 공유하므로 탭별 독립 Session을 보장하지 않는다.
- 같은 브라우저에서 다시 로그인해도 새 Session을 생성한다. 새 Cookie로 덮어쓰기 전의 Session은 만료 시각까지
  DB에 남을 수 있지만, 만료된 Session으로 재발급할 수는 없다.

### DB 제약과 조회를 변경한다

- `auth_sessions.user_id`의 `uk_auth_sessions_user_id` 유일 제약을 제거한다.
- 사용자별 Session 조회 및 사용자 삭제 시 종속 행 탐색을 위해 `user_id`에 일반 인덱스를 둔다.
- `refresh_token_hash`의 유일 제약과 원문 대신 SHA-256 해시를 저장하는 규칙은 유지한다.
- 기존 Flyway 파일을 수정하지 않고 새 마이그레이션으로 제약과 인덱스를 변경한다. 기존 Session 행은 보존한다.
- Entity의 제약 정의도 실제 스키마와 맞춘다. `Optional<AuthSession> findByUserId()`처럼 사용자당 하나의
  Session을 전제한 Repository 메서드는 사용처에 맞게 제거하거나 다건 조회로 변경한다.
- 변경 후 같은 사용자에게 여러 행이 생기면 이전 단일 세션 코드로 단순 복귀할 수 없다. 배포와 복구 계획에서
  이 호환성 경계를 확인하며, 이번 마이그레이션에서 기존 Session을 일괄 삭제하지 않는다.

### 재발급과 로그아웃은 해당 Session에만 적용한다

| 요청 | 적용 범위 |
| --- | --- |
| 로그인 | 새 Session 한 개 생성, 다른 Session 보존 |
| 재발급 | 전달된 Refresh Token의 Session만 회전 |
| 로그아웃 | 전달된 Refresh Token의 Session만 삭제하고 요청 브라우저의 Refresh Cookie 만료 |
| 회원 탈퇴 성공 | 해당 사용자의 모든 Session 삭제 |

- 재발급은 현재 Refresh Token 해시로 Session을 찾고, 만료 여부를 검증한 뒤 Access/Refresh Token을 새로 발급한다.
- 회전된 직전 Refresh Token은 거부한다. 다른 Session의 Refresh Token과 만료 시각은 변경하지 않는다.
- 로그아웃에서 사용자 ID로 모든 Session을 삭제하지 않는다. Cookie나 일치하는 Session이 없어도 기존 멱등
  `204 No Content` 응답과 Cookie 만료 처리를 유지한다.
- 회원 탈퇴는 기존 `users → auth_sessions` 외래 키의 `ON DELETE CASCADE`로 모든 Session을 삭제한다.
- Access Token은 기존 JWT 검증을 사용한다. 로그아웃이나 탈퇴에 따른 Refresh Session 삭제가 이미 발급한
  Access Token을 즉시 폐기하는 것은 아니다.

### 기존 트랜잭션과 동시성 경계를 활용한다

- 로그인 시 사용자 행의 비관적 잠금을 유지한다. 단일 Session 보장에는 더 이상 필요하지 않지만 사용자 삭제와
  Session 생성 사이의 기존 동기화 경계를 이번 정책 변경과 함께 제거하지 않는다.
- 같은 사용자의 로그인 요청이 겹쳐도 성공한 로그인마다 별도의 Session을 저장하고 각각 재발급할 수 있어야 한다.
- 재발급은 해당 Session의 비관적 잠금과 `READ_COMMITTED` 트랜잭션을 유지한다. 동일 Refresh Token의 동시
  재발급은 하나만 성공해야 한다.
- 서로 다른 Session의 재발급은 각자의 행을 변경한다. 하나의 Session에서 회전하거나 로그아웃한 결과가 다른
  Session을 무효화해서는 안 된다.

### HTTP 계약은 Session의 적용 범위를 설명한다

- 기존 로그인·재발급·로그아웃의 경로, 요청·응답 형식, Cookie 속성, CSRF 검증과 오류 코드를 유지한다.
- OpenAPI 설명에는 로그인 시 다른 브라우저의 Session이 유지되고, 재발급과 로그아웃은 요청의 Refresh Cookie에
  해당하는 Session에만 적용됨을 명시한다.
- 회원 탈퇴 설명에는 해당 사용자의 모든 Session이 삭제됨을 명시한다.
- 프론트는 현재의 메모리 Access Token, HttpOnly Refresh Cookie와 재발급 흐름을 사용할 수 있다. 여러 기기의
  로그인 상태 유지를 위해 Access Token을 영구 저장할 필요는 없다.

## 선택 이유

기존 Session 모델과 Refresh Token 해시 조회를 사용하면 기기 식별 API를 추가하지 않고도 브라우저별 인증 상태를
독립적으로 유지할 수 있다. 로그인에서 기존 Session을 덮어쓰는 동작과 사용자별 유일 제약을 제거하면 현재 문제의
원인을 직접 해결한다.

재발급과 로그아웃은 이미 Refresh Token 기준으로 동작하므로 해당 Session의 생명주기를 유지할 수 있다. 회원 탈퇴는
기존 DB CASCADE를 활용해 한 사용자의 Session 개수와 관계없이 모두 삭제한다.

## 검토한 대안

### 사용자당 하나의 Session을 유지한다

저장과 관리가 단순하지만 휴대폰 로그인 후 노트북 인증 복구가 실패하는 현재 문제를 해결하지 못한다.

### deviceId로 Session을 생성하거나 교체한다

같은 설치의 재로그인 Session을 교체하고 로그인 기기 목록을 제공하기에 유리하다. 다만 설치 식별자 생성·보관,
초기화와 중복 처리 정책이 필요하다. 현재 요구인 여러 브라우저의 동시 로그인은 Refresh Token별 Session만으로
충족할 수 있어 이번에는 도입하지 않는다.

### 모든 기기가 하나의 Refresh Token을 공유한다

한 기기의 회전으로 다른 기기가 가진 Token이 오래된 값이 되고, 한 기기만 로그아웃하기도 어렵다. 각 브라우저에
독립적인 자격 증명을 발급하는 방식을 선택한다.

## 영향

### 긍정적 영향

- 휴대폰 로그인 후에도 노트북의 새로고침과 토큰 재발급이 가능하다.
- 각 Session의 회전과 로그아웃이 다른 Session에 영향을 주지 않는다.
- 기존 API 형식과 클라이언트의 Token 보관 방식을 활용한다.
- 기존 Session 데이터와 회원 탈퇴의 전체 Session 삭제 정책을 유지한다.

### 부정적 영향과 위험

- 로그인마다 행이 추가되므로 만료됐거나 더 이상 사용하지 않는 Session 행이 누적된다.
- 일반 로그아웃은 다른 브라우저에 남은 Session을 폐기하지 않는다.
- 기존의 같은 브라우저 다중 탭 Refresh 경쟁은 별도로 남는다. 현재 페이지 내 single-flight만으로 탭 간 경쟁까지
  해결되는 것은 아니다.
- 기기명이나 설치 식별자를 저장하지 않아 사용자에게 기기별 Session 목록을 보여줄 수 없다.
- 이미 발급된 Access Token의 즉시 폐기와 과거 Refresh Token의 계열별 재사용 탐지는 제공하지 않는다.

## 미확정 사항

- 만료 Session의 보관 기간과 정리 주기
- 사용자당 Session 개수 제한, 전체 로그아웃과 기기별 관리가 필요한 시점

## 후속 작업

- OpenAPI에 로그인·재발급·로그아웃·탈퇴의 Session 적용 범위를 먼저 반영한다.
- 새 Flyway 마이그레이션과 Entity 제약을 변경하고 로그인 시 새 Session을 저장한다.
- 기존 단일 Session 전제의 Repository 사용처와 테스트를 수정한다.
- PostgreSQL에서 기존 데이터 보존, 같은 사용자의 다건 저장, Refresh 해시 유일 제약과 탈퇴 CASCADE를 검증한다.
- 두 개의 독립적인 Cookie 저장 공간을 사용하는 인수 테스트로 로그인·재발급·로그아웃의 독립성을 검증한다.
- 같은 사용자의 동시 로그인과 동일 Refresh Token의 중복 재발급 방지를 검증한다.
- 노트북·휴대폰에서 로그인 후 양쪽 새로고침, 한쪽 로그아웃 후 다른 쪽 인증 유지를 수동 검증한다.
- 인증 안내 문서에 다중 Session 정책을 반영한다.

### 별도 학습·고도화 기록

다음 항목은 이번 이슈의 필수 구현에 포함하지 않고 향후 학습 문서에 연결한다.

| 항목 | 나중에 학습할 질문 |
| --- | --- |
| 만료 Session 정리 | 만료 검증과 물리 삭제는 왜 다른가? 배치 삭제의 주기·인덱스·처리량은 어떻게 정하는가? |
| Session 개수 제한 | 동시 로그인 중에도 상한을 지키려면 어떤 기준으로 잠그고 오래된 Session을 선택해야 하는가? |
| 설치 식별과 기기 목록 | Session과 실제 기기·브라우저 설치는 어떻게 다르며 재설치·Cookie 삭제는 어떻게 처리하는가? |
| 전체 로그아웃과 JWT 폐기 | Refresh Session을 모두 지워도 Access JWT가 남는 이유와 즉시 폐기 방법의 비용은 무엇인가? |
| Token family와 재사용 탐지 | 회전된 Token의 재사용을 어떻게 구별하고 어느 범위의 Session을 폐기해야 하는가? |
| 다중 탭 재발급 조정 | 같은 Cookie를 공유하는 탭 사이에서 Refresh 요청을 어떻게 조율하는가? |

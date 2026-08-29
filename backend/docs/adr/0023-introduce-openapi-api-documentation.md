# 0023. OpenAPI 기반 API 문서화를 도입한다

- 날짜: 2026-08-28
- 관련 이슈: [#165](https://github.com/woowacourse-teams/2026-ChongChong/issues/165)
- 관련 ADR: [0004. 에러 응답과 에러 코드 형식을 통일한다](0004-standardize-error-response.md),
  [0007. Access Token 인증 경계를 구성한다](0007-establish-access-token-authentication-boundary.md)

## 배경

기존에는 Notion에 API 명세서를 작성해 프론트엔드와 백엔드가 요청·응답 형식을 확인했다. Notion의 필드명과 실제
구현이 달라지는 경우가 있었고, 프론트엔드는 어느 쪽을 기준으로 연동해야 하는지 판단하기 어려웠다.

백엔드가 DTO나 응답 형식을 수정할 때마다 담당자가 관련 Notion 페이지를 찾아 필드명, 타입과 예시를 직접 수정해야
했다. 코드 변경과 문서 변경이 분리되면서 문서가 늦게 갱신되거나 일부 페이지만 갱신되는 문제가 생겼다.

프론트엔드 연동에는 성공 응답뿐 아니라 요청 검증, 인증·인가, 리소스 부재와 도메인 오류 코드도 필요하다. 기존
Notion 문서는 이 정보를 실제 코드와 같은 형식으로 제공하거나 Swagger UI처럼 API를 직접 확인하고 호출하는 기능을
제공하지 못했다.

## 결정

### 코드에서 OpenAPI 문서를 생성하고 Swagger UI를 제공한다

- Springdoc OpenAPI를 사용해 Spring MVC Controller와 DTO에서 OpenAPI 3 명세를 생성한다.
- Controller에는 `@Tag`, `@Operation`, `@Parameter`, `@ApiResponse`를 사용해 API의 목적, 파라미터와 성공 응답을
  기록한다.
- DTO에는 `@Schema`를 사용해 필드 설명, 타입 제약과 요청·응답 예시를 기록한다.
- `OpenAPI` 설정에서 서비스 정보와 Bearer JWT Security Scheme을 정의한다.
- 개발 환경에서 다음 경로를 제공한다.

  | 목적 | 경로 |
  | --- | --- |
  | Swagger UI | `/swagger-ui/index.html` |
  | OpenAPI JSON | `/api/v3/api-docs` |
  | OpenAPI YAML | `/api/v3/api-docs.yaml` |

- 리더와 멤버처럼 사용자 역할에 따라 응답 필드가 달라지는 API는 Swagger Example에서 역할별 응답을 구분한다.
- 공통 `ErrorResponse`와 API별 도메인 예외는 OpenAPI Customizer로 문서에 추가한다. 예외 매핑은 Study, Notice,
  Assignment, Auth, Notification 도메인별 provider가 관리한다.
- API 계약의 기준은 Controller·DTO·예외 코드와 생성된 OpenAPI 문서로 삼는다. Notion에는 API 필드의 복사본을
  유지하지 않고 Swagger UI 링크와 사용 안내만 남긴다.
- 새로운 API를 추가하거나 요청·응답을 변경할 때는 같은 변경 안에서 관련 OpenAPI 어노테이션과 Example을 갱신한다.

### 운영 환경의 문서 공개는 배포 전에 결정한다

- 개발 중에는 Swagger UI와 OpenAPI endpoint를 활성화한다.
- 운영에서 문서를 공개할지 여부와 공개한다면 인증·네트워크 접근 제한을 적용할지는 배포 전에 결정한다.
- 운영에 공개하지 않기로 하면 `springdoc.api-docs.enabled`와 `springdoc.swagger-ui.enabled`를 비활성화한다.
- 문서 공개 여부는 API 자체의 인증·인가 정책을 대신하지 않는다.

## 선택 이유

OpenAPI를 코드에서 생성하면 DTO의 필드명과 타입 변경이 문서 스키마에 반영된다. Controller와 예외 코드 변경을 같은
커밋에서 관리할 수 있어 Notion과 구현 사이의 차이를 줄인다.

Swagger UI는 프론트엔드가 API 목록, 요청 형식, 성공·실패 응답과 Bearer 인증 방식을 한 화면에서 확인하게 한다.
OpenAPI JSON과 YAML은 문서 화면뿐 아니라 API Client, 코드 생성기와 검증 도구에서도 사용할 수 있다.

Springdoc은 현재 Spring Boot와 Spring MVC 구조에 연결할 수 있고, 기존 `/api` 전역 prefix와 JWT Resource Server를
유지하면서 도입할 수 있다. 역할별 응답 Example과 도메인별 예외 provider를 사용하면 자동 생성만으로 표현하기 어려운
실제 API 계약도 문서에 남길 수 있다.

## 검토한 대안

### Notion 명세서를 계속 수기로 관리한다

팀원이 쉽게 편집하고 논의할 수 있다는 장점이 있다. 하지만 구현 코드와 별도로 필드명, 타입, 예시와 오류 응답을
관리해야 하므로 현재 발생한 문서 드리프트와 반복 수정 문제를 해결하지 못한다.

### Markdown API 문서를 저장소에서 관리한다

Git으로 변경 이력을 남길 수 있지만 코드와 문서가 여전히 별도 계약으로 존재한다. API를 직접 탐색하거나 OpenAPI
기반 도구와 연동하려면 추가 형식과 기능을 관리해야 하므로 선택하지 않았다.

### OpenAPI JSON·YAML만 생성하고 Swagger UI는 제공하지 않는다

도구 연동에는 충분하지만 팀원이 API를 탐색하고 요청을 시험하기 어렵다. 개발 단계의 빠른 확인을 위해 JSON·YAML과
Swagger UI를 함께 제공한다.

## 영향

### 긍정적 영향

- 프론트엔드가 구현 코드와 같은 기준의 필드명, 타입과 예외 응답을 확인할 수 있다.
- API 변경 시 코드와 문서를 같은 변경 단위에서 검토할 수 있다.
- 역할별 응답과 도메인별 오류 코드를 실행 가능한 문서에서 확인할 수 있다.
- OpenAPI JSON·YAML을 API Client와 코드 생성 도구에 재사용할 수 있다.
- Notion 페이지를 찾아 여러 항목을 수기로 수정하는 작업이 줄어든다.

### 부정적 영향과 위험

- Controller와 DTO에 문서화 어노테이션이 추가되어 코드가 길어질 수 있다.
- Example과 설명을 코드 변경과 함께 갱신하지 않으면 문서의 의미가 실제 동작과 다시 달라질 수 있다.
- Swagger UI와 OpenAPI endpoint를 운영에 공개하면 내부 경로와 응답 구조가 외부에 노출될 수 있다.
- 역할별 응답과 도메인 예외가 늘어날수록 provider 매핑을 함께 관리해야 한다.

## 미확정 사항

- 운영 환경에서 Swagger UI와 OpenAPI endpoint를 공개할지 여부
- 운영에서 공개할 경우 적용할 인증 또는 네트워크 접근 제한 방식

## 후속 작업

- 새로운 Controller·DTO·예외 코드를 추가할 때 OpenAPI 어노테이션과 Example을 함께 갱신한다.
- Swagger UI에서 주요 성공·실패 응답과 Bearer 인증 호출 흐름을 확인한다.
- 운영 배포 전에 문서 endpoint의 비활성화 또는 접근 제한 방식을 결정하고 설정에 반영한다.
- 기존 Notion API 명세에는 Swagger UI를 기준 문서로 안내하고 중복된 필드 정의는 제거한다.

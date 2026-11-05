# #643 테스트 ADR 대조와 중복 정리

2026-11-05, `be/chore/643-backend-test-spec-alignment`에서 진행했다.
정비 직전 전체 결과는 테스트 클래스 192개, 실행 사례 1,693개였다. 파라미터별 실행 사례를 포함하며 API 커버리지 검사 1개는 별도다.

## 판단 기준

[ADR-0003](../../backend/docs/adr/0003-define-backend-test-strategy.md)은 같은 규칙을 여러 수준에서 반복 검증하지 않고, 단순 생성자·접근자 및 프레임워크 CRUD 검증을 기본 범위에서 제외한다.
[ADR-0028](../../backend/docs/adr/0028-adopt-spec-first-api-contracts.md)은 실제 HTTP 성공 호출과 명세를 비교하므로 별도 DTO 직렬화 검사와 겹치는 부분을 확인했다.
[ADR-0031](../../backend/docs/adr/0031-run-postgresql-specific-tests-with-testcontainers.md)에 따라 실제 DB 제약·마이그레이션·잠금·동시성 검증은 유지했다.

전체 테스트 파일의 프레임워크 사용과 최근 실행 시간·개수를 먼저 확인했다. 이후 공지 첨부, 인증 DTO, 캘린더 응답 등 중복 후보의 실제 준비 데이터·호출·검증문을 대조했다. 이름이 비슷하거나 실행 시간이 길다는 이유만으로 삭제하지 않았다. 전체 테스트의 모든 검증문에 중복이 없다고 보장하는 감사는 아니다.

## 확인한 문제와 정리

| 정리 대상 | 중복 또는 낮은 가치의 근거 | 남아 있는 검증 |
| --- | --- | --- |
| `ChongChongApplicationTests.contextLoads` | 검증문 없는 Spring 컨텍스트 시작만 수행 | 각 실제 HTTP 테스트의 전체 앱 시작과 요청 처리, `OpenApiMappingContractTest` |
| `StudyCalendarResponseTest` 2개 | 별도 JsonMapper로 확인하는 필드·null 제외·빈 배열 계약을 실제 HTTP가 이미 검사 | `StudyCalendarApiTest.returnsAllNoticesAndOnlyAssignedPersonalAssignments`, `returnsEmptyCalendar`, OpenAPI 응답 검증 |
| `StudyRepositoryTest` 2개 | mock의 findById 반환값을 다시 확인하며 실제 쿼리·매핑·제약을 검증하지 않음 | `StudyAcceptanceTest.getStudyInfoForMissingStudyTest`, `getStudyJoinInfoForMissingStudyTest`와 성공 조회 |
| 인증 요청·응답 변환 테스트 3개 | 필드 복사 결과를 별도로 검사하지만 로그인·갱신 HTTP에서도 같은 값을 사용·검증 | `KakaoSocialLoginAcceptanceTest.loginNewKakaoUserAndAccessProtectedApi`, `SocialLoginAcceptanceTest.completeWebAuthenticationLifecycle` 등 |
| `SocialLoginResultTest.createSocialLoginResult` | 입력값과 접근자 반환값 비교 | 실제 로그인 흐름. 이 클래스의 잘못된 사용자 ID·누락 토큰 검증은 유지 |
| `NoticeRequestValidationTest` 첨부 검증 12개 | 생성·수정 각각 null·빈 배열·양수·null 원소·0·음수를 Validator와 HTTP에서 반복 | `NoticeCreationAttachmentApiTest`의 생성·잘못된 ID 검사, `NoticeUpdateAttachmentApiTest`의 보존·제거·교체·잘못된 ID 검사 |
| 같은 DTO 테스트의 빈 수정·잘못된 수정 예약 2개 | HTTP에서 상태 보존까지 검증하는 사례와 겹침 | `NoticeApiTest.updateNoticeWithoutFieldsTest`, `preserveNoticeWhenReminderIsInvalid` |
| `NoticeApiTest.getNoticeDetailWithoutWriterTest` | 바로 앞 상세 성공 테스트가 작성자 필드 부재까지 동일하게 검사 | `getNoticeDetailTest` 및 OpenAPI additionalProperties 검증 |
| `NoticeCreationPendingAttachmentApiTest.createWithoutAttachments` | 요청에 포함하지 않는 별도 PENDING 파일을 만들고 첨부 없는 생성을 반복 | `NoticeCreationAttachmentApiTest.createWithoutAttachments`. 실제 미완료 첨부 거부는 유지 |
| 생성 첨부 최대 개수·크기 성공 1개 | 정책 단위 경계 및 기존/신규 합산 10개 수정 HTTP와 겹침 | `NoticeAttachmentPolicyTest`의 최대 개수·크기 경계, 생성 대표 성공, 수정의 최종 개수 검증 |
| 생성·수정 각각 단일 미지원 형식 거부 2개 | 같은 endpoint의 일부 이미지 정책 실패 테스트가 같은 코드와 더 강한 상태 보존을 검사 | `rejectWhenOneImagePolicyFails`, `rejectPartialImagePolicyFailure` |
| 생성·수정 HTTP의 0바이트 사례 각 1개 | 정책 단위가 0바이트·상한 초과를 모두 검증. HTTP에서는 같은 오류 코드·변경 보존 경로 반복 | `NoticeAttachmentPolicyTest.rejectImageSizeOutsideBoundaries`; 각 HTTP의 상한 초과 오류 응답·데이터 보존 검사는 유지 |

파라미터를 합쳐 실행 횟수만 줄이거나 검증을 비활성화하지 않았다. 삭제한 규칙은 위의 남겨 둔 테스트가 맡는다. 단일 크기 사례로 줄인 테스트는 일반 `@Test`로 변경했다.

## 유지한 테스트의 이유

- DTO의 토큰·인가 코드 마스킹은 HTTP 응답 계약으로 toString의 비밀값 노출을 검출할 수 없어 유지했다.
- 공지·과제 예약의 생략과 명시적 null은 JSON 역직렬화 이후 실제 보존 결과를 확인하므로 #643에서 추가한 HTTP 사례를 유지했다.
- 공지 길이·생성 예약 DTO 테스트에는 HTTP에 같은 조합이 없는 제약이 있어 남겼다. DTO 테스트라는 이유만으로 전부 삭제하지 않았다.
- `FileMetaDataRepositoryTest`는 이름과 달리 단순 저장·조회가 아니라 직접 작성한 잠금 조회의 ID 정렬과 잠금 모드를 확인한다. 실제 동시성 테스트도 별도로 유지했다.
- 인증 서비스의 Spring 테스트에는 고정 시각·DB에 저장한 토큰 해시·세션 독립성·트랜잭션 경계를 보는 사례가 있다. `SocialLoginFacadeTest`는 외부 인증 시점의 트랜잭션 상태를 실제로 확인하므로 mock 테스트로 단순 대체하지 않았다.
- 이전 리더가 올린 첨부의 유지·일부 제거·전체 제거는 일반 수정 테스트(현재 리더 파일)와 소유자가 달라 유지했다.
- 공지 생성/수정 실패, 파일 사용처 교차 검증, 삭제와 재사용의 동시 요청은 준비 데이터나 실패 시점이 다르므로 유지했다.
- PostgreSQL 마이그레이션·고유 제약·잠금·upsert·동시성, API별 인증·인가·외부 스터디 접근 거부는 유지했다.

## 검증 결과

- 실행 사례 **1,693 → 1,663개(30개 감소)**, 테스트 클래스 **192 → 189개**. 파라미터별 실행을 포함한 JUnit XML 집계다.
- 관련 공지·인증·스터디 테스트를 먼저 실행해 통과한 뒤, 최종 파일 상태에서 `backend/`의 `./gradlew test bootJar --build-cache --profile -Pci`를 실행했다.
- 전체 **1,663개 통과**, 실패·오류·건너뜀 **0개**. PostgreSQL Testcontainers 포함, `bootJar` 성공.
- 별도 `verifyOpenApiCoverage` **1개 통과**. 기존 63개 API의 성공 HTTP 커버리지 조건을 유지했다.
- `git diff --check` 통과. 애플리케이션 코드 변경 없음. 기존 PRD/API 정비와 예약 보존 회귀 테스트는 유지했다.
- 최종 실행 로그: `/tmp/chongchong-643-cleanup-final.log`. JUnit 결과: `backend/build/test-results/test/`, `backend/build/test-results/verifyOpenApiCoverage/`.
- 실행 시간은 2분 21초였지만 반복 비교 실험을 하지 않았으므로 성능 개선으로 해석하지 않는다. 외부 S3·카카오·운영 환경은 이번 테스트 정리에서 검증하지 않았다.

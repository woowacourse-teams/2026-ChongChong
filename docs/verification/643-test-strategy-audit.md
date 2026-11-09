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
| 인증 요청·응답 변환 테스트 3개 | 필드 복사 결과를 별도로 검사하지만 로그인·갱신 HTTP에서도 같은 값을 사용·검증 | `KakaoSocialLoginAcceptanceTest.loginNewKakaoUserAndAccessProtectedApi`, `SocialLoginAcceptanceTest.completeWebAuthenticationLifecycle` 등. 갱신 응답의 accessTokenExpiresAt은 반환된 JWT의 만료 시각과 직접 비교한다. |
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

## 추가 대조: 단순 조회 래퍼와 동일 분기

첫 PR 생성 후 남은 Repository·DTO·페이지 응답·사용자 생성 테스트를 추가 대조했다. 이번에는 다음 7개 사례를 정리했다.

| 제거한 사례 | 남긴 검증과 판단 근거 |
| --- | --- |
| `NoticeRepositoryTest.getByIdOrThrowTest` | 단순 findById 성공 후 ID를 다시 비교한다. 공지 저장·조회는 같은 클래스의 커서·스터디 조건 조회와 HTTP 상세 조회에서 검증한다. 알림 서비스에서 쓰는 getByIdOrThrow의 예외 분기는 별개이므로 유지했다. |
| `StudyMemberRepositoryTest.getByStudyIdAndUserIdOrThrowTest` | 같은 클래스의 `findByStudyIdAndUserIdTest`가 두 스터디·두 사용자 조건을 더 구체적으로 검증한다. 실제 래퍼를 호출하는 멤버 목록 HTTP 성공도 유지한다. |
| `StudyMemberRepositoryTest`의 두 `OrThrowNotFoundTest` | `StudyMemberAcceptanceTest.getStudyMembersByNonMemberTest`와 `expelMissingStudyMemberTest`가 각각 동일 래퍼를 거쳐 403 STUDY_ACCESS_DENIED, 404 STUDY_MEMBER_NOT_FOUND를 검증한다. 다른 스터디 접근 차단도 유지한다. |
| `AssignmentRequestValidationTest.validateEmptyUpdateRequestTest` | `AssignmentApiTest.preserveSubmissionMethodTest`의 `{}` 요청이 실제 역직렬화·검증·수정 이후 기존 설명 보존까지 검사한다. |
| `CursorPageResponseTest.createLastPageWhenResultSizeIsLessThanRequestedSize` | 같은 클래스의 `omitCursorOnLastPage`도 요청 2건에 결과 1건이며 내용·커서·hasNext를 동일하게 검증한다. 추가로 커서 추출 함수를 호출하지 않는지도 확인한다. 정확히 같은 크기·초과·빈 결과·불변 스냅샷 검증은 유지한다. |
| `UserTest.createUserTest` | 단순 필드 저장·접근자 검사다. 카카오 로그인 HTTP의 사용자 이름·URL 저장 검사와 사용자 이미지 서비스의 파일 미연결 검증이 남는다. null 이미지 허용과 이름·URL 제약, 이미지 상태 변경 검증은 유지한다. |

Web Push 저장 테스트는 이름과 달리 지연 로딩과 감사 시각을 확인하므로 유지했다. 인증·인가 테스트도 API 경로별 접근 제어를 검증하므로 공통 상태 코드라는 이유만으로 삭제하지 않았다. 조회 조건·집계·잠금 테스트와 트랜잭션 통합 테스트는 단순 mock으로 바꾸지 않았다.

추가 정리 결과는 **1,663 → 1,656개**이며 최초 정비 전 대비 총 37개가 감소했다. 관련 테스트 140개를 먼저 실행해 통과한 뒤, `./gradlew test bootJar --build-cache --profile -Pci`로 전체 1,656개 통과(실패·오류·건너뜀 0개), 별도 API 커버리지 검사 통과와 JAR 빌드 성공을 확인했다. `git diff --check`도 통과했다. 로그는 `/tmp/chongchong-643-second-full.log`에 남겼다. 애플리케이션 코드는 변경하지 않았고 외부 운영 환경이나 실행 속도 개선은 검증하지 않았다.

## 서비스 분기를 Spring 없이 검증하도록 분리

Spring을 사용하는 서비스 테스트 9개 클래스를 확인했다. 잠금·동시성 2개, 소셜 로그인 롤백, 알림 삭제 트랜잭션, 외부 인증 시점의 트랜잭션 부재, 세션 저장·회전·독립성 검증은 실제 인프라가 필요하므로 유지했다. 이 중 소셜 로그인과 로그아웃 클래스에서 서비스 분기 4개만 단위 테스트로 이동했다.

| 이동한 사례 | 단위 테스트 책임 | 남아 있는 통합 검증 |
| --- | --- | --- |
| 신규 소셜 사용자 가입 | 저장할 User 프로필·SocialAccount 제공자/식별자/사용자 연결, 저장 후 사용자 ID로 토큰 발급, 결과 전달 | `KakaoSocialLoginAcceptanceTest.loginNewKakaoUserAndAccessProtectedApi`, `AuthTokenServiceTest.issueTokenPairAndStoreAuthSession`의 실제 저장·JWT·해시 검증 |
| 기존 소셜 사용자 재사용 | 추가 사용자 저장 없이 연결된 사용자 선택, 기존 이름·이미지 보존, 토큰 발급 | 카카오 재로그인 HTTP, `SocialLoginServiceTest`의 재로그인 세션 보존 및 제공자 구분 |
| 일치하는 세션 로그아웃 | 토큰 해시를 사용한 잠금 조회와 조회된 세션 삭제 위임 | `AuthTokenServiceLogoutTest`의 동일 사용자·다른 사용자 세션 보존, 실제 삭제 및 삭제된 토큰 재사용 거부 |
| 세션 없는 로그아웃 | 반복 호출해도 삭제나 토큰 발급 없이 정상 종료 | 동일 사용자 세션 보존 통합 테스트에서 실제 세션 삭제 후 같은 토큰으로 재로그아웃 |

기존 `SocialLoginServiceValidationTest`, `AuthTokenServiceValidationTest`의 검증은 유지하고 이름을 각각 `SocialLoginServiceUnitTest`, `AuthTokenServiceUnitTest`로 바꿨다. MockitoExtension을 사용하며 Spring 컨텍스트·DB 청소·JWT 해석 없이 서비스 분기를 검증한다. 새 테스트의 ReflectionTestUtils는 엔티티 ID 준비에만 사용하며 컨텍스트를 띄우지 않는다.

테스트 사례 수는 이동 전후 동일하다. 통합 검증을 남겼으므로 Spring 컨텍스트 개수 감소나 전체 실행 시간 개선을 주장하지 않는다. 단위 테스트만 선택해 빠르게 피드백받을 수 있도록 책임을 분리했다.

서비스 단위 테스트 선택 실행은 성공했다. 최종 `./gradlew test bootJar --build-cache --profile -Pci`에서도 전체 1,656개(전환한 단위 테스트 클래스의 10개 포함), 실패·오류·건너뜀 0개를 확인했다. API 커버리지 검사와 JAR 빌드, `git diff --check`도 통과했다. 로그: `/tmp/chongchong-643-unit-only.log`, `/tmp/chongchong-643-service-unit-full.log`. 애플리케이션 코드 변경은 없다.

## HTTP 입력값 조합과 공통 오류 검증 추가 정리

대표 HTTP 호출은 남기고, 같은 분기의 값 조합은 이미 존재하는 단위 테스트에 맡겼다. 테스트 메서드 개수만 줄이기 위해 사례를 한 메서드로 합치거나 검증을 끄지 않았다.

| 정리한 HTTP 반복 | 남긴 검증 |
| --- | --- |
| 댓글 본문의 공백 한 칸·개행/탭 2회 | HTTP는 빈 본문과 길이 초과를 각각 거부하고 댓글·알림 미저장을 검사한다. `CommentTest.rejectBlankContent`는 빈 값·공백·개행/탭·유니코드 공백을 모두 검증한다. 정규화 후 길이 검증 HTTP도 유지한다. |
| 댓글 cursor=-1 1회 | 같은 `@Positive` 경로의 cursor=0 오류 응답을 유지한다. `CursorPageRequestTest.rejectNonPositiveCursorTest`는 0·-1을 모두 검사한다. 문자열 형식 오류와 Long 범위 초과는 변환 경로가 달라 유지한다. |
| 홈 업로드 이미지 조회의 JPEG·WebP 2회 | PNG 대표 HTTP가 서명 URL 반환·외부 URL 우선순위·DB 미변경을 검사한다. `FileUrlGeneratorTest.generatesImageUrlOnlyForSupportedTypes`가 JPEG·PNG·WebP 각각 실제 presigner의 서명·응답 Content-Type을 검증한다. 이미지 정책 테스트도 세 형식 허용을 유지한다. |
| 홈 이미지 수정 fileId=-1 1회 | `@Positive`의 fileId=0 HTTP 오류·기존 상태 보존 검사를 유지한다. 생략·null 요청도 유지한다. 정책 및 서비스 단위 테스트가 0·-1을 모두 검증한다. |
| 공지 목록 size=Integer.MAX_VALUE HTTP 요청 1회 | 동일 int 범위의 `@Max` 거부는 size=101에서 오류 코드와 size 필드를 확인한다. 100 성공과 0 실패는 유지한다. `CursorPageRequestTest.rejectOutOfRangePageSizeTest`가 Integer.MAX_VALUE까지 검증한다. |

다른 스터디 공지 조회는 일반 멤버와 리더로 요청하는 사례가 각각 있어 합치지 않았다. 잘못된 JSON, 숫자 변환 오류, 인증·인가, 소유권, 파일 상태와 트랜잭션 실패도 처리 경로가 달라 유지했다.

관련 테스트 175개 통과 후 `./gradlew test bootJar --build-cache --profile -Pci`로 전체 **1,650개 통과**, 실패·오류·건너뜀 0개를 확인했다. API 커버리지 검사와 JAR 빌드, `git diff --check`도 통과했다. 직전 1,656개에서 6개 감소했으며 메서드 안의 중복 HTTP 요청도 1회 제거했다. 최초 1,693개 대비 누적 43개 감소다. 로그: `/tmp/chongchong-643-http-focused.log`, `/tmp/chongchong-643-http-full.log`. 외부 운영 환경과 속도 개선은 검증하지 않았다.

## 서비스 성공 흐름과 인수 테스트의 책임 통합

스터디·스터디 멤버·공지·과제·제출 서비스 테스트의 성공 흐름을 인수 테스트와 대조했다. 저장소에 준비한 값을 다시 DTO에서 비교하는 성공 사례 22개 메서드(파라미터 포함 23회)를 제거했다. 5개 파일에서 준비 코드·중복 검증·불필요한 import 537줄을 줄였다. 전체 테스트 클래스 삭제를 목표로 삼지 않고 서비스 판단이 남아 있는 클래스는 유지했다.

| 서비스에서 제거한 묶음 | 실제 HTTP 검증을 맡는 테스트 |
| --- | --- |
| `StudyServiceTest`: 공개 참여 정보·정원 도달 정보 조회 | `StudyAcceptanceTest.getStudyJoinInfoForPublicStudyTest`, `hideFullStudyButAllowJoinInfoTest`. 인원수·이미지 유무를 조회하고 정원 50에서 목록 제외·조회 허용·가입 거절까지 확인한다. 기존 mock에서 51명을 반환하던 비정상 정원 fixture의 단순 값 전달 반복은 제거했다. |
| `StudyServiceTest`: 생성·이미지 생성·수정·이미지 교체·기본 이미지 초기화 | `StudyAcceptanceTest.createStudyTest`, `updateStudyTest`, `StudyImageAcceptanceTest.createsStudyWithImage`, `replacesAndRetainsImage`, `resetsImageToDefault`. 실제 사용자 연결·리더 등록·저장 상태·재조회까지 검증한다. |
| `StudyServiceTest`: 정보 조회 2개·리더/멤버 상세 2개·초대 링크 조회 | `StudyAcceptanceTest.getStudyInfoTest`, `getStudyInfoWithImageTest`, `getStudyDetailForLeaderTest`, `getStudyDetailForMemberTest`, `getInviteLinkTest`. 응답 필드·집계·역할별 형태·이미지 URL·초대 링크를 실제 HTTP에서 확인한다. |
| `StudyMemberServiceTest`: 초대 토큰 가입·공개 스터디 가입 | `StudyMemberAcceptanceTest.joinStudyTest`, `joinPublicStudyByIdTest`의 실제 멤버 저장과 응답 검증. 토큰 불일치·비공개·정원·가입 한도 및 실패 전 후속 호출 차단 단위 테스트는 유지한다. |
| `NoticeServiceTest`: 기본 상세 조회 | `NoticeApiTest.getNoticeDetailTest`의 상세 필드 및 빈 첨부 응답. 첨부별 URL/null 해석과 접근 정책 단위 테스트는 유지한다. |
| `AssignmentServiceTest`: 상세 조회·수정 성공 | `AssignmentApiTest`의 상세 조회 및 `updateAssignmentTest`가 실제 필드·마감·예약 저장을 확인한다. 조회 배치·정책 거부 및 후속 저장 차단은 유지한다. |
| `AssignmentSubmissionServiceTest`: 내 제출 상태 3개·상세·목록 | `AssignmentApiTest.getMySubmissionDetailTest`, `getMySubmissionDetailBeforeSubmitTest`, `getMySubmissionDetailByLeaderTest`, `getSubmissionDetailTest` 및 제출 목록 HTTP. `AssignmentHomeProfileImageApiTest`가 현황·목록·상세의 현재 홈 이미지 반영도 확인한다. 최초/재제출 알림 결정과 정책 거부 테스트는 유지한다. |

서비스 단위 테스트에 남긴 책임은 정책 거부 시 조회·저장·URL 발급 차단, 여러 ID의 일괄 조회, 이미지 생략 시 검증 생략, 잠금·삭제 순서, 알림 발생 조건이다. `getMyStudiesTest`의 역할별 집계는 현재 HTTP 테스트가 동일 수치까지 검증하지 않아 유지했다. 즉, 성공 테스트라는 이유만으로 일괄 삭제하지 않았다.

관련 테스트 295개 통과 후 `./gradlew test bootJar --build-cache --profile -Pci`로 전체 **1,627개 통과**, 실패·오류·건너뜀 0개를 확인했다. API 커버리지 검사·JAR 빌드·`git diff --check`도 통과했다. 직전 1,650개에서 23개, 최초 1,693개 대비 누적 66개 감소했다. 로그: `/tmp/chongchong-643-service-dedup-focused.log`, `/tmp/chongchong-643-service-dedup-full.log`. 애플리케이션 코드는 변경하지 않았으며, 전체 실행 시간 개선이나 외부 운영 환경 검증 결과를 주장하지 않는다.

## 2026-11-09 최신 dev 통합

통합 기준은 `origin/dev`의 `70a09284`다. #655의 리더 공지 7일 정책, #658의 댓글 응답 메타데이터·등록 전 멤버십 확인, #652 캘린더 및 #653 과제 대상 선택 웹 연동을 대조했다.

- `StudyServiceTest`의 충돌은 기존에 제거한 상세 응답 복사 테스트를 다시 추가하지 않는 방향으로 해결했다. 새 Clock 주입과 리더 공지 집계의 7일 기준 인자는 유지한다. `LeaderNoticeExpiryApiTest`가 경계 전·정확히 경계·경계 후, 읽음 여부, 수정·재알림 후 기간 유지와 멤버/전체 조회 보존을 실제 HTTP로 검증한다.
- `scope.md`는 최신 기준으로 통합하고 리더 7일 정책과 이미 연결된 웹 기능을 함께 반영했다.
- 신규 댓글 테스트의 전체 개수·본인 여부·작성자 역할 검증은 API별 연결 계약을 맡으므로 유지한다. 서비스의 잠금 전 거부·잠금 후 멤버십 재확인도 단순 HTTP 권한 검사와 다른 책임이므로 유지한다.
- `NoticeCommentApiTest.rejectUnauthorizedAndMixedScope`의 비회원 POST 1회는 새 `rejectNonMemberRegardlessOfNoticeExistence(false)`와 동일해 제거했다. 새 테스트가 오류 코드와 댓글·알림·발송 내역 미저장까지 확인한다. 비회원 GET과 다른 스터디·원문의 접근 거부는 유지한다.
- PRD/API 설명에 댓글 `totalCount`, `isMine`, `author.role`과 웹 캘린더·제출 대상 선택 연결을 반영했다. 삭제된 `AssignmentForm.tsx` 근거를 `AssignmentFormFunnel.tsx`로 갱신했다. 마감 후 closeAt 제약, 웹 제출 방법 필수, assigned=false 수정 제한 미연동은 남겨 두었다.
- 프론트 파일은 직접 수정하지 않았다. `git diff origin/dev -- frontend`가 비어 있는 것으로 최신 dev와 동일함을 확인했다. 기존 원본 작업 디렉터리의 사용자 변경도 건드리지 않았다.

`./gradlew test bootJar --build-cache --profile -Pci`로 전체 **1,638개 통과**, 실패·오류·건너뜀 0개를 확인했다. 별도 OpenAPI 커버리지 검사 1개와 JAR 빌드도 통과했다. 최초 실행은 Docker가 꺼져 PostgreSQL 테스트가 건너뛰어지고 성공 호출 기록이 부족해 커버리지 검사가 실패했다. Docker 실행 후 전체를 재실행한 최종 결과다. 로그: `/tmp/chongchong-643-dev-sync-full-docker.log`.

API 문서 테스트 13개, lint·bundle·build와 63개 API 메타데이터 검증이 통과했다. 기존 lint 경고 3개(license, CSRF·VAPID의 4xx 응답)와 Redoc nullable/oneOf 경고 2개는 남아 있다. 로그: `/tmp/chongchong-643-dev-sync-docs.log`. `git diff --check` 및 프론트가 origin/dev와 동일함을 확인했다. 실제 브라우저·배포 환경은 검증하지 않았다.

## 2026-11-09 API 문서 경고 정리

- CSRF·VAPID 공개 조회는 인증 없이 허용되지만 잘못된 Bearer 토큰을 전달하면 보안 필터가 `AUTHENTICATION_REQUIRED` 401을 반환한다. 공통 오류 응답을 명세에 추가하고 `WebCsrfSecurityTest`의 매개변수 테스트로 두 경로의 상태·오류 코드·WWW-Authenticate 헤더를 검증했다.
- 과제 생성의 `anyOf`는 필드 존재와 null이 아님만 검사하도록 표현했다. 배열·문자열 타입과 최소 인원·enum 제약은 공통 properties에 그대로 두었다. 따라서 두 필드를 모두 보내는 요청과 한 필드만 유효한 요청은 기존처럼 허용하고, 둘 다 생략하거나 null이면 거부한다. Redoc에서 nullable 타입과 분기의 타입을 병합할 때 발생하던 경고를 제거했다.
- 저장소에 지정된 라이선스가 없어 임의로 라이선스를 선언하지 않았다. `redocly.yaml`에서 `info-license` 권고만 제외하고 나머지 recommended 규칙은 유지한다.

관련 백엔드 테스트 86개(과제 API·공개 조회 인증·OpenAPI 필터)가 실패·오류·스킵 없이 통과했다. 문서 테스트 13개, lint·63개 API 메타데이터 검사·문서 빌드도 통과했고 기존 문서 경고 5개는 더 이상 출력되지 않는다. 전체 백엔드 재실행·브라우저 화면 확인은 이번 문서 변경에서 수행하지 않았다. 프론트와 애플리케이션 코드는 변경하지 않았다. 로그: `/tmp/643-doc-warning-tests.log`, `/tmp/643-doc-warning-node-tests.log`, `/tmp/643-doc-warning.log`.

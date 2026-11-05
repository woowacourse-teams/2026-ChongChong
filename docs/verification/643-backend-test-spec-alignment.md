# #643 백엔드 테스트·PRD/API 정비

기준: 2026-11-05, 통합 개발 브랜치 `f21a30c1`.
작업 브랜치: `be/chore/643-backend-test-spec-alignment`.
기존 작업 폴더와 분리한 worktree에서 진행했으며 애플리케이션 구현과 DB 스키마는 변경하지 않았다.

## 확인 범위와 정비 결과

| 범위 | 확인한 차이와 반영 | 근거 |
| --- | --- | --- |
| 공지·과제 예약 | 웹 입력을 미구현으로 안내하던 내용을 수정. 생략·null 유지, 빈 배열 제거, 잘못된 배열 거부 계약과 예시 보완 | `NoticeApiTest`, `AssignmentApiTest`, `NoticeForm.tsx`, `AssignmentForm.tsx` |
| 첨부·다운로드 | 공통 업로드·완료 호출, 첨부 표시와 다운로드 연결 반영. 업로드 용도와 대상별 파일 정책 구분 | `shared/modules/files/api.ts`, `shared/widgets/AttachmentList.tsx`, 각 도메인 첨부 정책 |
| 제출 입력·응답 | 웹의 링크·첨부만 제출 가능 및 상세 createdAt·content의 null 허용 반영 | `AssignmentSubmissionForm.tsx`, `assignment/responseSchemas.ts` |
| 스터디 | 공개 탐색·참여 조건, 대표 이미지의 생략/명시적 null 차이 추가 | `StudyService`, `StudyMemberService`, `StudyUpdateRequest`, OpenAPI studies 경로 |
| 홈 프로필 | 홈 이미지 변경·초기화와 모든 참여 스터디에 반영되는 서버 정책 추가 | `UserProfileImageService`, `UserProfileImageUrlResolver`, 프로필 HTTP 테스트 |
| 제출 대상·캘린더 | 병합 상태 반영. 현재 지정 여부·보존 제출물·월 경계·현재 원본 반영 정책 명시 | #638·#640, `StudyCalendarApiTest`, 과제 HTTP 테스트 |
| 댓글 | 권한·작성자 마스킹·알림·페이징의 서버 범위를 PRD에 추가 | `CommentAccessPolicy`, `CommentTargetResolver`, `CommentService`, 댓글 HTTP 테스트 |
| API 목록·상태 | 63개 operation과 목차를 대조해 누락한 완료·프로필·댓글 API 추가. 병합·테스트가 확인된 review를 done으로 갱신 | `openapi.yaml`, `OpenApiMappingContractTest`, `OpenApiCoverageTest` |

## 추가 회귀 테스트

공지·과제 각각 다음 네 경우를 실제 로컬 HTTP 요청으로 검증한다. 기존 추가·개별 삭제·전체 삭제 왕복 테스트와 함께 부분 수정 계약을 확인한다.

- 제목만 수정하고 remindAts 생략: 기존 예약 유지.
- 제목 수정과 remindAts=null: 기존 예약 유지.
- remindAts=[null]: 400 INVALID_INPUT_VALUE, 기존 제목·예약 보존.
- 지난 시각을 포함한 remindAts: 400 INVALID_INPUT_VALUE, 기존 제목·예약 보존.

공지·과제 HTTP 테스트 113개(추가 8개 포함)가 실패·오류·건너뜀 없이 통과했다.

## 검증

아래 결과는 최초 정비 완료 시점이다. 이후 테스트 ADR에 따른 중복 정리와 최종 검증은 [테스트 전략 대조 기록](643-test-strategy-audit.md)을 참고한다.

| 실행 | 결과 |
| --- | --- |
| `bash .github/scripts/validate-branch-policy.sh be/chore/643-backend-test-spec-alignment dev` | 통과 |
| `./gradlew test --tests withoutc.chongchong.notice.NoticeApiTest --tests withoutc.chongchong.assignment.AssignmentApiTest` | 113개 통과, 실패·오류·건너뜀 0 |
| `./gradlew test bootJar --build-cache --profile -Pci` | 1,693개 통과, 실패·오류·건너뜀 0. PostgreSQL Testcontainers 포함. 실행 JAR 빌드 성공 |
| 위 명령의 `verifyOpenApiCoverage` | 별도 테스트 1개 통과. 63개 API 성공 HTTP 호출 확인 |
| API 문서 디렉터리의 `npm test` | 13개 통과 |
| `npm run lint`, `npm run bundle`, `npm run build` | 성공. 아래 기존 경고 유지 |
| API 목차와 번들 대조 | 63개 operation 모두 목차에 존재 |
| 변경 문서의 상대 링크와 x-frontend 근거 경로 | 누락 없음 |
| `git diff --check` | 통과 |

Gradle 명령은 `backend/`, npm 명령은 `backend/docs/openapi/`에서 실행했다.
상세 결과는 로컬 `backend/build/reports/tests/test/index.html`, `backend/build/reports/tests/verifyOpenApiCoverage/index.html`과 `backend/build/test-results/`에 생성된다. 자동 생성 보고서·문서 dist는 커밋 대상에 포함하지 않는다.

## 남은 범위

- 홈 프로필 이미지 변경·초기화 UI, 과제 대상 개별·전체 선택 및 assigned=false에 따른 편집 제한, 웹 캘린더 연결은 남아 있다.
- 과제 제출 방법은 서버 선택 값과 웹 필수 입력의 차이가 유지된다.
- 실제 브라우저, S3 업로드·다운로드, 카카오 로그인, 운영 푸시 수신 및 배포 환경 Postman 검증은 실행하지 않았다. 로컬 HTTP 테스트는 테스트 인증과 S3 대역을 사용하며 외부 서비스 성공을 보장하지 않는다.
- 문서 lint의 기존 경고 3개는 라이선스 미지정과 공개 CSRF·VAPID API의 4xx 응답 미기재다. 확인되지 않은 라이선스·오류 계약을 추가하지 않았다.
- 정적 문서 빌드는 기존 AssignmentCreateRequest의 nullable/oneOf 조합에 관한 Redoc 사전 렌더링 경고 2개를 출력한다. 빌드는 성공하며 배포 문서는 기존 client-only 렌더링을 사용한다. 이번 작업에서는 요청 계약을 변경하지 않았다.

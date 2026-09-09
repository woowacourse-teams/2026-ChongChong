# API 명세

명세 원본은 `openapi.yaml`(목차), `paths/`(URL별 API), `components/`(공유 정의)로 나눈다.
API를 수정할 때는 아래 표에서 파일을 찾아 설명·파라미터·응답·예시를 수정한다. 같은 URL의 여러 HTTP 메서드는 한 파일에서 관리한다.
요청·응답 필드가 참조 스키마에 있으면 해당 `$ref` 파일도 함께 수정한다. API를 추가할 때는 Path Item 파일을 만들고
`openapi.yaml`의 `paths`에 연결한 뒤 아래 표를 갱신한다.

## API별 수정 파일

| API | 메서드와 URL | 수정 파일 |
| --- | --- | --- |
| CSRF 토큰 조회 | `GET /api/auth/csrf` | [paths/auth/csrf.yaml](paths/auth/csrf.yaml) |
| 소셜 로그인 | `POST /api/auth/login` | [paths/auth/login.yaml](paths/auth/login.yaml) |
| 액세스 토큰 갱신 | `POST /api/auth/refresh` | [paths/auth/refresh.yaml](paths/auth/refresh.yaml) |
| 로그아웃 | `POST /api/auth/logout` | [paths/auth/logout.yaml](paths/auth/logout.yaml) |
| 스터디 생성 | `POST /api/studies` | [paths/studies/collection.yaml](paths/studies/collection.yaml) |
| 스터디 상세 조회 | `GET /api/studies/{studyId}` | [paths/studies/detail.yaml](paths/studies/detail.yaml) |
| 스터디 수정 | `PATCH /api/studies/{studyId}` | [paths/studies/detail.yaml](paths/studies/detail.yaml) |
| 스터디 삭제 | `DELETE /api/studies/{studyId}` | [paths/studies/detail.yaml](paths/studies/detail.yaml) |
| 스터디 기본 정보 조회 | `GET /api/studies/{studyId}/info` | [paths/studies/info.yaml](paths/studies/info.yaml) |
| 내 스터디 목록 조회 | `GET /api/studies/me` | [paths/studies/my-studies.yaml](paths/studies/my-studies.yaml) |
| 스터디 초대 링크 조회 | `GET /api/studies/{studyId}/invite-link` | [paths/studies/invite-link.yaml](paths/studies/invite-link.yaml) |
| 스터디 참여 | `POST /api/studies/join` | [paths/studies/join.yaml](paths/studies/join.yaml) |
| 스터디 멤버 목록 조회 | `GET /api/studies/{studyId}/members` | [paths/study-members/collection.yaml](paths/study-members/collection.yaml) |
| 스터디 멤버 방출 | `DELETE /api/studies/{studyId}/members/{memberId}` | [paths/study-members/detail.yaml](paths/study-members/detail.yaml) |
| 스터디 탈퇴 | `DELETE /api/studies/{studyId}/members/me` | [paths/study-members/leave.yaml](paths/study-members/leave.yaml) |
| 공지 생성 | `POST /api/studies/{studyId}/notices` | [paths/notices/collection.yaml](paths/notices/collection.yaml) |
| 공지 목록 조회 | `GET /api/studies/{studyId}/notices` | [paths/notices/collection.yaml](paths/notices/collection.yaml) |
| 공지 상세 조회 | `GET /api/studies/{studyId}/notices/{noticeId}` | [paths/notices/detail.yaml](paths/notices/detail.yaml) |
| 공지 수정 | `PATCH /api/studies/{studyId}/notices/{noticeId}` | [paths/notices/detail.yaml](paths/notices/detail.yaml) |
| 공지 삭제 | `DELETE /api/studies/{studyId}/notices/{noticeId}` | [paths/notices/detail.yaml](paths/notices/detail.yaml) |
| 공지 읽음 현황 조회 | `GET /api/studies/{studyId}/notices/{noticeId}/status` | [paths/notices/status.yaml](paths/notices/status.yaml) |
| 공지 읽음 처리 | `PATCH /api/studies/{studyId}/notices/{noticeId}/read` | [paths/notices/mark-as-read.yaml](paths/notices/mark-as-read.yaml) |
| 내 공지 읽음 상태 조회 | `GET /api/studies/{studyId}/notices/{noticeId}/status/me` | [paths/notices/my-status.yaml](paths/notices/my-status.yaml) |
| 과제 생성 | `POST /api/studies/{studyId}/assignments` | [paths/assignments/collection.yaml](paths/assignments/collection.yaml) |
| 과제 목록 조회 | `GET /api/studies/{studyId}/assignments` | [paths/assignments/collection.yaml](paths/assignments/collection.yaml) |
| 과제 상세 조회 | `GET /api/studies/{studyId}/assignments/{assignmentId}` | [paths/assignments/detail.yaml](paths/assignments/detail.yaml) |
| 과제 수정 | `PATCH /api/studies/{studyId}/assignments/{assignmentId}` | [paths/assignments/detail.yaml](paths/assignments/detail.yaml) |
| 과제 삭제 | `DELETE /api/studies/{studyId}/assignments/{assignmentId}` | [paths/assignments/detail.yaml](paths/assignments/detail.yaml) |
| 과제 제출 현황 조회 | `GET /api/studies/{studyId}/assignments/{assignmentId}/status` | [paths/assignments/status.yaml](paths/assignments/status.yaml) |
| 과제 제출 | `POST /api/studies/{studyId}/assignments/{assignmentId}/submissions` | [paths/assignment-submissions/collection.yaml](paths/assignment-submissions/collection.yaml) |
| 과제 제출 목록 조회 | `GET /api/studies/{studyId}/assignments/{assignmentId}/submissions` | [paths/assignment-submissions/collection.yaml](paths/assignment-submissions/collection.yaml) |
| 내 과제 제출 조회 | `GET /api/studies/{studyId}/assignments/{assignmentId}/submissions/my` | [paths/assignment-submissions/my-submission.yaml](paths/assignment-submissions/my-submission.yaml) |
| 과제 제출 상세 조회 | `GET /api/studies/{studyId}/assignments/{assignmentId}/submissions/{submissionId}` | [paths/assignment-submissions/detail.yaml](paths/assignment-submissions/detail.yaml) |
| 과제 제출 수정 | `PATCH /api/studies/{studyId}/assignments/{assignmentId}/submissions/{submissionId}` | [paths/assignment-submissions/detail.yaml](paths/assignment-submissions/detail.yaml) |
| 푸시 토큰 등록 | `POST /api/push-tokens` | [paths/push-tokens/collection.yaml](paths/push-tokens/collection.yaml) |
| 푸시 토큰 비활성화 | `DELETE /api/push-tokens/{installationId}` | [paths/push-tokens/detail.yaml](paths/push-tokens/detail.yaml) |

## 공통 정의 수정 위치

- [인증](components/security.yaml): Bearer JWT 정의
- [파라미터](components/parameters.yaml): 경로 식별자와 페이지네이션
- [공통 오류 응답](components/responses.yaml): 상태별 오류 조합과 예시
- [오류 스키마](components/schemas/errors.yaml): 오류 코드·메시지·필드 오류 구조
- [공통 타입](components/schemas/common.yaml): 서버 날짜·시간
- 도메인별 요청·응답 스키마: [인증](components/schemas/auth.yaml), [스터디·멤버](components/schemas/studies.yaml), [공지](components/schemas/notices.yaml), [과제](components/schemas/assignments.yaml), [제출물](components/schemas/assignment-submissions.yaml), [푸시 토큰](components/schemas/push-tokens.yaml)

공통 정의를 바꾸면 이를 참조하는 여러 API에 적용된다. 특정 API만 달라져야 한다면 해당 API에 맞는 정의를 분리한다.
`$ref` 경로는 작성 중인 파일 기준 상대경로이며, 아래 검증 명령으로 참조가 정상인지 확인한다.
`dist/openapi.yaml`과 `dist/index.html`은 자동 생성 결과이므로 직접 수정하지 않는다.

## 로컬 검증과 미리 보기

Node.js 24에서 다음 명령을 실행한다.

```bash
cd backend/docs/openapi
npm ci
npm test
npm run lint
npm run bundle
npm run build
python3 -m http.server 8080 --directory dist
```

브라우저에서 `http://localhost:8080`을 열어 정적 문서를 확인한다. `dist/`는 빌드 산출물이며 저장소에 커밋하지
않는다. `bundle`은 도구 소비용 단일 OpenAPI YAML을, `build`는 Redoc 정적 HTML을 생성한다.
현재 고정한 Redocly CLI의 서버 렌더링 결과는 CDN Redoc의 hydration과 충돌할 수 있어, `build` 뒤에 클라이언트 렌더링으로 전환한다.
Redocly CLI를 업데이트할 때는 이 변환의 회귀 테스트와 실제 브라우저 콘솔을 다시 확인한다.

## CI와 GitHub Pages

Backend CI는 모든 `main`, `dev`, `prod` 대상 PR과 push에서 두 job을 병렬로 실행한다. `API documentation` job은 명세의
렌더링 회귀 테스트, lint·bundle·정적 문서 build를 실행하고, `build` job은 backend Gradle `test`로 실제 HTTP 응답과
OpenAPI 계약의 일치 및 operation coverage를 확인한 뒤 `bootJar`를 만든다.

`dev`에 명세 변경이 병합되면 `Deploy API documentation` 워크플로가 명세 검증과 backend Gradle `test`, `bootJar`를
모두 통과한 `dist/`만 GitHub Pages에 배포한다. 2026-09-06 확인 시 이 저장소는 Pages 사이트가 설정되어 있지 않았다. 첫 배포 전 저장소 관리자에게
**Settings → Pages → Build and deployment → Source: GitHub Actions**를 설정해 달라고 요청한다. 저장소가 공개이므로
Pages 문서도 공개된다. 배포 URL은 워크플로의 `github-pages` 환경과 GitHub Pages 설정에서 확인한다.

현재 `dev`에는 활성 ruleset `Protect dev`가 있으나 required status check 규칙은 없다(2026-09-06 GitHub API 조회). 이
워크플로를 병합 조건으로 사용하려면 저장소 관리자가 해당 ruleset에 Backend CI 워크플로의 `API documentation`과 `build` 검사를
required status check로 추가해야 한다. 이 설정은 워크플로 YAML만으로 자동 적용되지 않는다.

## 자동 검증의 범위

CI는 명세 문법·참조·문서 생성 가능 여부, Spring MVC의 업무 endpoint와 명세 operation 집합, 기존 인수 테스트가
호출한 정상 요청과 성공 응답의 스키마 일치를 검사한다. 잘못된 요청을 의도하는 실패 시나리오에서는 요청 스키마 검증을 생략하고 실제 오류 응답을 검증한다.
OpenAPI의 표준 nullable JSON과 달리 실제 빈 본문을 반환하는 경우는 `getMySubmissionDetail`의 200 응답만 예외로 둔다.
해당 response 객체의 `x-allow-empty-body: true`는 리더 응답의 본문 없음을 명시하며, 멤버가 미제출이면 `submitted=false` DTO를
반환한다. 계약 검증은 이 응답의 본문 누락만 허용하고 요청 및 나머지 응답 검증은 그대로 수행한다.
개별 `./gradlew test --tests ...` 실행은 빠른 개발 확인용이며 전체 operation coverage 게이트를 생략한다. PR과 배포 전에는
반드시 전체 `./gradlew test`를 실행한다.
따라서 명세의 설명 문장만으로 서버 시간 기록, 권한 판단, 저장 동작 같은 업무 의미가 자동으로 증명되지는 않는다.
그런 동작을 바꿀 때는 해당 HTTP 시나리오 테스트도 같은 PR에서 수정하거나 추가한다.

## 백엔드 개발 진행 정보

각 operation(`get`, `post` 등)에 `x-backend`를 작성한다. 같은 URL에서도 메서드마다 담당자와 상태가 다를 수 있다.

```yaml
post:
  operationId: createStudy
  summary: 스터디 생성
  description: 인증된 사용자가 새 스터디를 생성한다.
  x-backend:
    status: in-progress
    owner: frombunny
    description: |
      신규 응답 필드를 구현하고 있다.
      구버전 응답 호환성 테스트가 남아 있다.
```

위 진행 정보는 작성 예시이며 실제 API 상태를 뜻하지 않는다.

| 필드 | 작성 규칙 |
| --- | --- |
| `status` | `null`(미확인), `todo`(대기), `in-progress`(진행 중), `review`(리뷰 중), `done`(완료) |
| `owner` | `@`를 제외한 GitHub 아이디. 미배정이면 `null`. 진행 중·리뷰 중·완료에는 담당자 필수 |
| `description` | 현재 작업, 남은 작업, 막힌 이유를 적는 문자열. 전달사항이 없으면 `""`. 여러 줄은 `|` 사용 |

세 필드는 모두 작성하며, 필드 이름 오타나 잘못된 값은 `npm run lint`와 `npm run build`에서 실패한다.
상태나 담당자를 확인할 근거가 없으면 `status: null`, `owner: null`로 둔다.
담당자는 관련 구현 이력과 이슈 배정을 확인한 뒤 갱신한다.

- `todo`: 작업 범위가 정해졌고 착수 전이다.
- `in-progress`: 담당자가 구현 또는 관련 검증을 진행한다.
- `review`: 구현과 관련 검증을 마치고 코드 리뷰를 기다리거나 받고 있다.
- `done`: 구현이 병합되었고 관련 검증이 통과했다. 배포 여부를 뜻하지 않는다.

상태는 자동 동기화되지 않는다. 작업 변경 시 명세도 갱신하고, 완료 표시는 병합·검증 결과를 확인한 뒤 반영한다.
작업이 막혀도 현재 상태를 유지하고 `description`에 원인과 필요한 조치를 적는다.
기존 전체 operation 매핑·HTTP 호출 검증은 상태와 관계없이 유지한다. 미구현 API를 명세에 추가하면 현재 계약 검증은
실패하므로, 기획 단계의 API는 이슈에서 관리하고 구현과 함께 명세에 추가한다.

### 2026-09-09 초기 현황 입력 근거

현재 명세의 36개 API는 병합된 구현과 전체 계약 테스트를 기준으로 `done`으로 입력했다.
기준 코드는 `fb87aa0f03d4ccd077faff7f1e453586a673b2ba`이며, 상태는 **현재 명세의 구현 범위**에 해당한다.
열린 후속 이슈가 모두 완료되었다는 의미는 아니다. 후속 작업은 개발 설명에 이슈 번호와 함께 별도로 적었다.
착수 여부는 열린 이슈만으로 판단하지 않았다.

초기 `owner`는 핵심 API 구현 PR 작성자를 기준으로 입력했다. 팀의 공식 유지보수 담당 배정을 뜻하지 않으며,
향후 실제 작업 담당자가 달라지면 변경한다. 문서화·공통 리팩터링 작성자를 API 담당자로 일괄 지정하지 않는다.

| API 범위 | 초기 담당자 | 구현 근거 |
| --- | --- | --- |
| 인증 4개 | `JYL35` | [웹 인증·세션 #112](https://github.com/woowacourse-teams/2026-ChongChong/pull/112), [카카오 연동 #115](https://github.com/woowacourse-teams/2026-ChongChong/pull/115) |
| 스터디·참여 8개 | `wontop02` | [생성·초대 #49](https://github.com/woowacourse-teams/2026-ChongChong/pull/49), [핵심 API #73](https://github.com/woowacourse-teams/2026-ChongChong/pull/73), [수정 #253](https://github.com/woowacourse-teams/2026-ChongChong/pull/253) |
| 멤버 목록·방출·탈퇴 3개 | `JYL35` | [멤버 API #167](https://github.com/woowacourse-teams/2026-ChongChong/pull/167) |
| 공지 8개 | `frombunny` | [CRUD #81](https://github.com/woowacourse-teams/2026-ChongChong/pull/81), [읽음 상태 #107](https://github.com/woowacourse-teams/2026-ChongChong/pull/107) |
| 과제·제출 11개 | `frombunny` | [CRUD #121](https://github.com/woowacourse-teams/2026-ChongChong/pull/121), [제출·현황 #137](https://github.com/woowacourse-teams/2026-ChongChong/pull/137), [내 제출 #221](https://github.com/woowacourse-teams/2026-ChongChong/pull/221) |
| 푸시 토큰 2개 | `wontop02` | [등록 #157](https://github.com/woowacourse-teams/2026-ChongChong/pull/157), [재등록·비활성화 #288](https://github.com/woowacourse-teams/2026-ChongChong/pull/288) |

개발 설명에는 API 동작과 남은 작업을 적고, 개인별 기여 이력은 적지 않는다.

후속 작업은 [공지 책임 분리 #256](https://github.com/woowacourse-teams/2026-ChongChong/issues/256),
[전체 초대 링크 요청 #274](https://github.com/woowacourse-teams/2026-ChongChong/issues/274),
[이미지·제출 형식 #275](https://github.com/woowacourse-teams/2026-ChongChong/issues/275),
[리드 제출 #276](https://github.com/woowacourse-teams/2026-ChongChong/issues/276),
[신규 멤버 노출 #277](https://github.com/woowacourse-teams/2026-ChongChong/issues/277),
[제출물 공개 #278](https://github.com/woowacourse-teams/2026-ChongChong/issues/278)를 확인했다.

### API 설명과 개발 설명의 구분

- operation의 표준 `description`: API 동작, 호환성 유지 이유, API 사용 시 주의사항. Redoc 상세 문서에 표시한다.
- 응답 스키마 필드의 표준 `description`: 필드의 의미와 사용 기준. 공유 스키마라면 `components/schemas/`에서 작성한다.
- `x-backend.description`: 개발 진행을 위한 메모. 보드에서 일반 텍스트로 표시하며 Markdown·HTML은 렌더링하지 않는다.

다음은 구버전 호환을 위한 가상의 필드 설명 예시다. 실제 API에서 확인되지 않은 호환성 정책을 복사하지 않는다.

```yaml
properties:
  memberName:
    type: string
    deprecated: true
    description: |
      구버전 클라이언트 호환용 이름이다.
      신규 개발에서는 member.name을 사용한다.
      제거 일정은 아직 정해지지 않았다.
```

`deprecated: true`는 제공 중이지만 신규 사용을 권장하지 않는다는 의미다. 대체하기로 결정한 필드에만 표시한다.
두 필드의 관계와 제거 조건은 확인된 정책에 따라 API 전체 `description`에도 적는다.

### 보드 생성과 확인

`npm run build`는 최신 명세를 JSON으로 번들하고 메타데이터를 검증한 뒤 `dist/index.html`과
`dist/board.html`, `dist/board.css`를 생성한다. 추가 런타임 서버나 의존성은 없다.
기존 문서 빌드·Pages 업로드 흐름에서 보드도 함께 생성되며 `dist/`는 직접 수정하거나 커밋하지 않는다.

로컬 미리 보기 서버에서 `/board.html`을 열거나 API 문서 소개의 **백엔드 개발 보드** 링크를 선택한다.
메서드·이름·URI·상태·담당자를 열별로 비교하고, 개발 설명은 표에서 바로 읽는다.
API 이름을 선택하면 `operationId`에 해당하는 상세 문서로 이동한다. 좁은 화면에서는 API별 카드로 재배치하여 상태·담당자·개발 설명을 함께 확인한다.
JavaScript를 사용할 수 없어도 전체 API 표와 상세 링크는 제공된다.

보드는 생성 시점의 명세 정보를 보여준다. 기존 배포 설정에서는 API 문서와 보드가 함께 공개될 수 있다.

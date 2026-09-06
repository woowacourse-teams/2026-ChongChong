# 0028. 명세 우선 API 계약을 채택한다

- 날짜: 2026-09-06
- 상태: 채택
- 관련 이슈: [#279](https://github.com/woowacourse-teams/2026-ChongChong/issues/279)
- 대체 ADR: [0023. OpenAPI 기반 API 문서화를 도입한다](0023-introduce-openapi-api-documentation.md)

## 배경

Notion 명세와 구현이 별도로 바뀌면 API 계약이 늦게 갱신될 수 있다. 기존 방식은 Controller와 DTO의 OpenAPI
어노테이션에서 Swagger 문서를 생성하므로, 구현 코드와 문서 정보가 섞이고 명세를 먼저 검토하기 어렵다.

## 결정

`backend/docs/openapi/openapi.yaml`과 `backend/docs/openapi/components.yaml`을 API 계약의 원본으로 둔다. 구현 전에 경로,
인증, 요청, 응답, 오류 코드, 예시와 전달사항을 두 파일에 작성한다. OpenAPI의 Markdown 설명을 사용해 요청 body가 없는 이유나 서버가 시간을
기록하는 규칙 같은 전달사항도 함께 기록한다.

API 계약을 변경하는 기능 PR은 구현과 명세를 함께 수정한다. PR의 Backend CI는 Redocly로 명세를 lint·bundle하고
정적 문서를 빌드한다. backend 테스트는 Spring MVC 업무 endpoint와 명세 operation의 누락을 양방향으로 확인하고,
기존 HTTP 인수 테스트가 호출한 정상 요청과 응답을 명세 스키마와 비교한다. 잘못된 요청을 의도하는 실패 시나리오에서는
요청 스키마 검증을 생략하고 실제 오류 응답을 검증한다.

OpenAPI의 nullable JSON으로 표현할 수 없는 실제 빈 본문은 `getMySubmissionDetail`의 200 응답에만
`x-allow-empty-body: true`를 둔다. 이는 리더의 본문 없는 응답을 나타내며, 멤버가 미제출이면 `submitted=false` DTO를
반환한다. 계약 검증은 이 response의 본문 누락만 제외하고 요청과 다른 응답의 검증을 유지한다.

정적 문서는 `dev` 병합 후 동일 검증을 통과한 결과만 GitHub Pages에 배포한다. Pages의 활성화와 공개 범위 설정은
저장소 관리자가 GitHub 설정에서 관리한다.

## 선택 이유

구현 전에 API 계약을 검토하고 개발을 시작하는 흐름이 필요하다. Notion 유지가 필수 조건은 아니며, 전달사항, Bearer JWT
인증, 필수 Path Variable, 상태별 응답 JSON 예시와 오류 발생 조건을 담을 수 있으면 저장소 명세와 웹 보기 방식을
사용할 수 있다.

현재 코드에서 문서를 생성하는 운영 방식은 구현에 앞서 독립 명세를 검토하려는 요구를 충족하지 못했다. 문서 설명과
예시가 Controller·DTO에 섞여 코드와 문서의 변경 이유를 함께 관리해야 하는 점도 불편하다. 독립 OpenAPI 파일은
기능 단위 PR에서 구현 변경과 계약 변경을 함께 리뷰하고, Redoc 정적 문서는 같은 원본을 사람이 읽기 쉬운 형태로
제공한다.

명세 파일이 수정됐는지만 확인하는 CI는 구현과 계약의 실제 차이를 발견하지 못한다. 구조 lint·bundle·문서 빌드는
명세의 형식·참조·표시 가능 여부를 확인하고, endpoint 목록 비교와 실제 HTTP 계약 검증은 구현의 경로와 테스트가
호출한 응답 구조를 확인한다. 이미 존재하는 Rest Assured HTTP 인수 테스트를 재사용하면 별도의 중복 테스트 체계를
늘리지 않고 이 확인을 수행할 수 있다. 계약 검증은 구조 차이를, HTTP 시나리오 테스트는 권한·오류 조건·서버 시각
기록 같은 업무 의미를 각각 검증한다.

## 검토한 대안

### Notion 명세를 계속 원본으로 관리

자유로운 설명과 논의에는 편리하지만, 구현과 분리된 문서를 기능 PR에서 자동으로 비교하기 어렵다. 이미 발생한 갱신
누락을 해결하지 못하므로 원본으로 유지하지 않는다.

### Controller·DTO의 OpenAPI 어노테이션에서 Swagger 문서를 생성

구현의 타입 변경을 문서에 반영할 수 있지만, 문서 정보가 코드와 섞이고 구현 전에 독립 명세를 검토하려는 현재 요구를
충족하지 못한다. 이 방식은 ADR 0023에서 채택했으나 명세 우선 개발 흐름을 위해 대체한다.

### 명세 파일의 수정 여부만 CI에서 확인

기능 PR에 명세 파일 변경을 요구할 수는 있지만, 계약 변경이 없는 내부 구현 변경까지 수정을 강제할 수 있다. 파일이
수정돼도 실제 HTTP 응답과 일치하는지는 확인할 수 없으므로, 명세 구조 검사와 HTTP 계약 검증을 함께 둔다.

## 결과

- API를 구현하기 전에 리뷰 가능한 계약을 저장소에서 변경 이력과 함께 관리한다.
- Swagger 문서용 어노테이션과 런타임 문서 생성은 계약 원본이 아니다.
- lint와 문서 빌드는 명세 파일의 형식·참조·표시 가능 여부를 확인한다.
- endpoint 목록과 성공 응답 검증은 명세 누락 및 테스트된 응답의 구조 차이를 발견한다.
- 계약 검증 통과만으로 권한·오류 조건·저장 같은 업무 동작이 맞다고 판단하지 않는다.

## 한계

명세의 자연어 설명은 자동으로 실행되지 않는다. 실제로 호출하지 않은 오류·인가 경계, 서버 시간 기록, 데이터 저장과
도메인 규칙은 계약 검사만으로 증명할 수 없다. 해당 의미를 바꾸는 PR은 의도한 HTTP 시나리오 테스트를 함께 둔다.

개별 `./gradlew test --tests ...` 실행은 전체 operation coverage 게이트를 생략하므로 개발 중 빠른 확인에만 사용한다.
PR과 배포 검증은 전체 `./gradlew test`로 수행한다.

GitHub Pages는 현재 설정되어 있지 않으며, 공개 저장소의 Pages 사이트는 공개된다. 배포 전에 Pages source를
GitHub Actions로 설정하고 공개 문서 범위를 확인해야 한다.

2026-09-06 GitHub API 조회에서 `dev`에는 활성 ruleset `Protect dev`가 있지만 required status check 규칙은 없다.
워크플로를 병합 조건으로 강제하려면 저장소 관리자가 Backend CI 워크플로의 `API documentation`과 `build` 검사를
required status check로 ruleset에 추가해야 한다.

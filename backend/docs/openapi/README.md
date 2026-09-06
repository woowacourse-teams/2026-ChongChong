# API 명세

`openapi.yaml`과 `components.yaml`은 ChongChong HTTP API의 계약 원본이다. API를 추가하거나 요청·응답·인증·상태 코드·오류 코드를
바꾸는 PR에는 해당 operation과 재사용 component 변경을 함께 포함한다.

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

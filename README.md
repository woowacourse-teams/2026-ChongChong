# 총총 · ChongChong

총총은 스터디의 공지 확인과 과제 제출 현황을 함께 관리하는 서비스입니다.
리더는 누가 공지를 읽고 과제를 제출했는지 확인하고, 스터디원은 해야 할 활동을 확인하고 완료합니다.

## 👥 Team

| 안톨리니 | 디움 | 바니 | 이든 | 피즈 |
| :---: | :---: | :---: | :---: | :---: |
| [<img src="https://avatars.githubusercontent.com/u/100985243?v=4&amp;s=240" width="120" height="120" alt="안톨리니 프로필" />](https://github.com/Antoliny0919) | [<img src="https://avatars.githubusercontent.com/u/86216821?v=4&amp;s=240" width="120" height="120" alt="디움 프로필" />](https://github.com/DongEun02) | [<img src="https://avatars.githubusercontent.com/u/156324288?v=4&amp;s=240" width="120" height="120" alt="바니 프로필" />](https://github.com/frombunny) | [<img src="https://avatars.githubusercontent.com/u/154980427?v=4&amp;s=240" width="120" height="120" alt="이든 프로필" />](https://github.com/JYL35) | [<img src="https://avatars.githubusercontent.com/u/169251789?v=4&amp;s=240" width="120" height="120" alt="피즈 프로필" />](https://github.com/wontop02) |
| [@Antoliny0919](https://github.com/Antoliny0919) | [@DongEun02](https://github.com/DongEun02) | [@frombunny](https://github.com/frombunny) | [@JYL35](https://github.com/JYL35) | [@wontop02](https://github.com/wontop02) |
| FE | FE | BE | BE | BE |

## 📚 Docs

서비스 기능부터 기술 선택의 근거, 협업 규칙까지 필요한 문서를 찾아보세요.

| 알고 싶은 내용 | 문서 |
| --- | --- |
| 서비스 기능과 구현 범위 | [PRD](docs/common/v2/README.md) · [구현 범위](docs/common/v2/scope.md) |
| 프론트엔드 기술과 개발 기록 | [프론트엔드 기술 문서](https://github.com/DongEun02/ChongChong-Our-Way) |
| 백엔드 기술 선택과 근거 | [기술 의사결정 기록 (ADR)](backend/docs/adr/README.md) |
| API 요청과 응답 | [API 명세서](https://woowacourse-teams.github.io/2026-ChongChong/board) · [명세 작성 가이드](backend/docs/openapi/README.md) |
| LLM 위키 | [NotebookLM](https://notebook.google.com/notebook/da2c5052-dd72-4b47-93ee-48208b46ab78) |
| 브랜치와 PR 규칙 | [기여 가이드](docs/CONTRIBUTING.md) |
| 커밋 메시지와 PR 제목 | [커밋 컨벤션](docs/COMMIT_CONVENTION.md) |
| 프론트엔드 실행과 개발 | [프론트엔드 README](frontend/README.md) |
| 배포와 운영 | [배포 가이드](deploy/README.md) |
| 무중단 배포와 롤백 검증 | [prod 측정 결과와 실제 롤백 기록](deploy/docs/prod-deployment-verification.md) |
| 과거 기능 기록 | [v1 PRD](docs/common/v1/README.md) |

## 🛠️ Tech Stack

### 🎨 Frontend

**Core** &nbsp; | &nbsp; ![React 19](https://img.shields.io/badge/React_19-20232A?style=flat-square&logo=react&logoColor=61DAFB) ![TypeScript 6](https://img.shields.io/badge/TypeScript_6-3178C6?style=flat-square&logo=typescript&logoColor=white) ![Webpack 5](https://img.shields.io/badge/Webpack_5-2B3A42?style=flat-square&logo=webpack&logoColor=8DD6F9) ![pnpm 11](https://img.shields.io/badge/pnpm_11-F69220?style=flat-square&logo=pnpm&logoColor=black)

**Libraries** &nbsp; | &nbsp; ![TanStack Query](https://img.shields.io/badge/TanStack_Query-FF4154?style=flat-square&logo=reactquery&logoColor=white) ![React Router](https://img.shields.io/badge/React_Router-CA4245?style=flat-square&logo=reactrouter&logoColor=white) ![Emotion](https://img.shields.io/badge/Emotion-D36AC2?style=flat-square) ![Zod](https://img.shields.io/badge/Zod-3E67B1?style=flat-square&logo=zod&logoColor=white) ![ky](https://img.shields.io/badge/ky-20232A?style=flat-square)

**Testing** &nbsp; | &nbsp; ![Jest](https://img.shields.io/badge/Jest-C21325?style=flat-square&logo=jest&logoColor=white) ![Testing Library](https://img.shields.io/badge/Testing_Library-E33332?style=flat-square&logo=testinglibrary&logoColor=white) ![MSW](https://img.shields.io/badge/MSW-FF6A33?style=flat-square&logo=mockserviceworker&logoColor=white)

**Monitoring & Analytics** &nbsp; | &nbsp; ![Sentry](https://img.shields.io/badge/Sentry-362D59?style=flat-square&logo=sentry&logoColor=white) ![PostHog](https://img.shields.io/badge/PostHog-000000?style=flat-square&logo=posthog&logoColor=white)

### ⚙️ Backend

**Core** &nbsp; | &nbsp; ![Java 25](https://img.shields.io/badge/Java_25-ED8B00?style=flat-square) ![Spring Boot 4](https://img.shields.io/badge/Spring_Boot_4-6DB33F?style=flat-square&logo=springboot&logoColor=white) ![Gradle](https://img.shields.io/badge/Gradle-02303A?style=flat-square&logo=gradle&logoColor=white)

**Data & Security** &nbsp; | &nbsp; ![Spring Data JPA](https://img.shields.io/badge/Spring_Data_JPA-6DB33F?style=flat-square&logo=spring&logoColor=white) ![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?style=flat-square&logo=postgresql&logoColor=white) ![Flyway](https://img.shields.io/badge/Flyway-CC0200?style=flat-square&logo=flyway&logoColor=white) ![Spring Security](https://img.shields.io/badge/Spring_Security-6DB33F?style=flat-square&logo=springsecurity&logoColor=white)

**Testing** &nbsp; | &nbsp; ![JUnit 5](https://img.shields.io/badge/JUnit_5-25A162?style=flat-square&logo=junit5&logoColor=white) ![REST Assured](https://img.shields.io/badge/REST_Assured-3B7C3A?style=flat-square) ![Testcontainers](https://img.shields.io/badge/Testcontainers-2496ED?style=flat-square&logo=testcontainers&logoColor=white)

**Monitoring** &nbsp; | &nbsp; ![Spring Boot Actuator](https://img.shields.io/badge/Spring_Boot_Actuator-6DB33F?style=flat-square&logo=springboot&logoColor=white) ![Micrometer](https://img.shields.io/badge/Micrometer-117A65?style=flat-square)

## 🗂️ Project Structure

| 폴더 | 내용 |
| --- | --- |
| [frontend](frontend/) | React 애플리케이션 |
| [backend](backend/) | Spring Boot 애플리케이션 |
| [docs](docs/) | 기능 문서와 협업 가이드 |
| [deploy](deploy/) | 배포 설정과 운영 가이드 |
| [.github](.github/) | GitHub Actions와 협업 템플릿 |

## 🚀 Getting Started

실행할 영역을 펼쳐 로컬 실행 명령어를 확인하세요.

<details>
<summary>Frontend 실행하기</summary>

```bash
cd frontend
corepack enable
pnpm install
pnpm start
```

</details>

<details>
<summary>Backend 실행하기</summary>

```bash
cd backend
./gradlew bootRun
```

</details>

## ✅ Checks

변경한 영역에 맞춰 아래 검증 명령어를 실행하세요.

<details>
<summary>Frontend 타입 검사 · 린트 · 빌드</summary>

```bash
cd frontend
pnpm type-check
pnpm lint
pnpm build
```

</details>

<details>
<summary>Backend 테스트</summary>

```bash
cd backend
./gradlew test
```

</details>

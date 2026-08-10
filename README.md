# ChongChong

우아한테크코스 2026 팀 프로젝트 ChongChong의 모노레포입니다.

## 기술 구성

| 영역     | 기술                                       |
| -------- | ------------------------------------------ |
| Frontend | React 19, TypeScript 6, Webpack 5, pnpm 11 |
| Backend  | Java 25, Spring Boot 4, Gradle             |

## 저장소 구조

```text
.
├── backend/     # Spring Boot 애플리케이션
├── frontend/    # React 애플리케이션
├── docs/        # 프로젝트 문서
└── .github/     # GitHub Actions와 협업 템플릿
```

## 로컬 실행

### Frontend

```bash
cd frontend
corepack enable
pnpm install
pnpm start
```

### Backend

```bash
cd backend
./gradlew bootRun
```

## 검증

```bash
cd frontend
pnpm type-check
pnpm lint
pnpm build
```

```bash
cd backend
./gradlew test
```

## 브랜치 흐름

```text
fe/*      ─┐
be/*      ─┼→ dev → prod
common/*  ─┘
```

## 협업 문서

- 브랜치 이름, PR 타깃, 머지 방식과 보호 규칙: [기여 가이드](docs/CONTRIBUTING.md)
- 커밋 메시지와 PR 제목 형식: [커밋 컨벤션](docs/COMMIT_CONVENTION.md)

# 총총 Frontend

## 이미지 최적화

배포 빌드(`build:development`, `build:production`)에서 사용하는 PNG는 무손실 압축하고,
SVG는 벡터와 `viewBox`를 유지하며 최적화합니다. SVG 내부의 Base64 PNG도
기존 포맷을 유지하며 무손실 압축합니다. 원본 파일은 수정하지 않습니다.

WebP 변환이나 크기 조절이 필요하면 import에 크기를 지정합니다. 로컬 서버에서도 동작합니다.

```tsx
// 40px로 표시할 아이콘을 3배 해상도로 생성합니다.
import logo from './header-icon.svg?width=120&as=webp';
import photo from './photo.png?width=600&as=webp';
```

생성 파일은 Webpack이 빌드 결과에 포함하므로 직접 만들거나 저장소에 추가하지 않습니다.
`width`는 실제 표시 너비의 2~3배로 지정하고, 같은 원본도 화면별로 다른 크기를 사용할 수 있습니다.
일반 SVG import는 벡터로 유지됩니다. 로컬에서는 일반 PNG/SVG 압축을 생략하여 개발 속도를 유지합니다.

`pnpm test:images`로 픽셀 보존, 투명도, 크기별 생성과 개발·배포 빌드 호환성을 확인합니다.

## 릴리스 절차

프론트엔드 배포 담당자를 위한 릴리스 절차입니다.
버전은 FE 변경을 기준으로 결정합니다.
아래는 `1.0.0`에서 버그 수정 버전 `1.0.1`을 배포하는 예시입니다.

1. [CHANGELOG.md](./CHANGELOG.md)의 `[Unreleased]` 내용을 `[1.0.1] - YYYY-MM-DD` 항목으로 확정합니다.
2. 변경 기록 맨 위에 새로운 `[Unreleased]` 섹션을 추가합니다.
3. [package.json](./package.json)의 `version`을 `1.0.1`로 변경하고, 위 변경 사항을 포함한 PR을 작성해 `dev`에 병합합니다.
4. `dev → prod` PR을 병합하고 배포합니다. **배포 성공과 주요 기능의 정상 동작을 확인합니다.**
5. 배포 시스템에서 **실제로 배포한 커밋 SHA**를 확인하고, 해당 커밋에 `frontend-v1.0.1` 태그를 생성합니다.
6. GitHub Release에서 해당 태그를 선택하고 제목을 `Frontend v1.0.1`로 작성합니다. CHANGELOG의 해당 버전 내용만 복사하고, 운영 배포 시각과 커밋 해시를 함께 기록해 게시합니다.

릴리스 준비 이후 FE 변경이 추가되었다면, `prod` 병합 전에 버전과 CHANGELOG를 다시 확인합니다.

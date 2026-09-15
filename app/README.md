# 총총 앱

이슈: https://github.com/woowacourse-teams/2026-ChongChong/issues/311

React Native + Expo SDK 57 기반 네이티브 앱의 공통 UI와 내비게이션 골격입니다.
앱 진입·내 스터디·계정 UI는 #317에서 목 상태로 구현했습니다.
스터디 내부 홈·공지·과제·멤버 기능 화면은 후속 이슈에서 구현합니다.
API·인증·푸시·업로드와 연결하지 않습니다.

## 실행

Node.js와 pnpm 11.19.0을 준비한 뒤 이 디렉터리에서 실행합니다.

```sh
pnpm install --frozen-lockfile
pnpm start
pnpm ios
pnpm android
pnpm web
```

`ios`는 Xcode와 iOS Simulator, `android`는 Android SDK와 에뮬레이터가 필요합니다.
실기기는 SDK 57과 호환되는 Expo Go에서 개발 서버 QR로 실행합니다.
웹은 공통 컴포넌트의 보조 검증용이며 네이티브 실행을 대체하지 않습니다.

## 검증

```sh
pnpm type-check
pnpm lint
pnpm exec expo install --check
pnpm peers check
pnpm export:all
```

`export:all`은 iOS·Android·웹 JavaScript/에셋 번들을 생성합니다.
앱 바이너리 빌드, 스토어 배포 또는 실기기 실행 검증은 아닙니다.
현재 Biome는 `rules.preset: "none"`이므로 `pnpm lint` 성공은 포맷·구문 검사 결과이며 실질적인 린트 규칙 검증 통과를 뜻하지 않습니다.

## 구조

- `src/app`: Expo Router 경로. 시작 화면, 공통 UI 확인, 스터디 탭 골격.
- `src/ui`: 디자인 토큰·공통 UI·Safe Area 및 스크롤 처리.
- `src/mocks`: 리더/스터디원 시나리오와 공유 fixture.
- `assets/fonts`: 로컬 Pretendard 글꼴과 OFL 라이선스.
- [docs/DESIGN.md](docs/DESIGN.md): Computer Use로 확인한 Figma 변수와 구현 기준.

웹 프런트엔드와 의존성을 분리한 독립 패키지입니다. SDK 호환 버전을 유지하려면
네이티브 패키지는 `pnpm exec expo install <패키지>`로 추가합니다.

## 확인 시나리오

1. 시작 화면 → 소셜 진입 → 알림 안내 → 내 스터디로 이동한다.
   `/scenarios`에서 리더·스터디원·빈 상태를 선택할 수 있다.
2. 홈·공지·과제·멤버 탭을 이동하고 같은 역할이 표시되는지 확인한다.
3. 뒤로 가기로 복귀해 역할 상태가 유지되는지 확인한다.
4. 공통 UI에서 빈 입력/10자 초과 시 저장 비활성화, 정상 입력 시 안내 표시를 확인한다.
5. 모달의 취소·확인과 토스트를 눌러 닫기를 확인한다.

10자 제한은 공통 UI 시연 전용이며 실제 프로필 정책이 아닙니다.
시나리오는 메모리 상태로만 유지되며 앱 재실행/웹 새로고침 시 초기화됩니다.

## 검증 기록

2026-09-14: 타입 검사·Biome·peer dependency 검사 통과.
iOS·Android·웹 번들 export 성공. 웹에서 역할 전환, 4개 탭 이동, 뒤로 가기,
입력 오류/정상 입력, 모달과 안내 동작을 직접 확인했습니다.
후속 재검증에서 발견한 탭 이동 시 역할 초기화는 공식 `PlatformPressable`로 웹 링크 탐색 처리를 복원해 수정했습니다. 수정 후 스터디원 상태로 홈·공지·과제·멤버를 이동하고 뒤로 돌아와도 역할이 유지되는 것을 확인했습니다.

최신 공통 UI와 원본 빈 상태 이미지를 포함한 iOS·Android·웹 번들 export를 다시 수행해 성공했습니다.
이 결과는 JavaScript/에셋 번들 생성 검증이며 실제 네이티브 앱 실행 성공을 뜻하지 않습니다.

현재 Xcode 26.6(17F113)은 설치되어 있습니다. 기본 개발 도구 경로는
`/Library/Developer/CommandLineTools`이므로 Xcode 조회 시
`DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer`를 명시했습니다.
`xcrun simctl list devices available`은 기기 없이 `== Devices ==`만 반환했고,
`xcrun devicectl list devices`는 `No devices found.`를 반환했습니다.
Android SDK 기본 경로와 `adb`, Android Studio도 확인되지 않았습니다.

따라서 iOS·Android 실제 실행, OS 키보드, 화면 읽기, 실제 Safe Area 및
Android 시스템 뒤로가기는 미검증입니다. 사용 가능한 시뮬레이터/실기기와
Android SDK를 준비한 뒤 위 확인 시나리오를 각 플랫폼에서 수행해야 합니다.

공통 UI의 Figma 대응과 웹 측정 결과는 [docs/UI-VERIFICATION.md](docs/UI-VERIFICATION.md)에 정리했습니다.

## 참고

- https://docs.expo.dev/more/create-expo/
- https://github.com/orioncactus/pretendard
- https://www.figma.com/design/sT7K2tOQl8JtyHzuwT0nnl?node-id=1702-18637

## 앱 진입 및 내 스터디 (#317)

목록·생성·참여·마이페이지·알림 안내를 로컬 상태로 확인할 수 있습니다.
프로필 이름 제한은 8자입니다. 실행 경로와 Figma 대조·네이티브 미검증 사항은
[docs/ISSUE-317.md](docs/ISSUE-317.md)에 기록했습니다.

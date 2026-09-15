> 보관 안내: 아래 이미지 파일명은 당시 검증 이력이다. 검증 이미지는 사용자 요청으로 제거했으며, 현재 열 수 있는 첨부 파일이 아니다.

# #321 홈·알림 시각 검토 (Pass B 재검토)

## recommendation

**APPROVE (PASS)**

확인된 기본 홈(리더·스터디원)과 알림 목록은 라이브 React Native 컴포넌트·SVG·이미지 자산으로 렌더링되며, 이번 범위에서 확인된 비의도적 배치·타이포그래피·아이콘 불일치가 없다.

## evidence

- 참조 캡처: `figma-member.jpg`, `figma-leader.jpg`, `figma-notifications.jpg`
- 실제 기본 화면: `member.jpg`, `leader.jpg`, `notifications.jpg`
- 반응형 화면: `member-320.jpg`
- DOM 측정: `member-measurements.json`
- 구현: `src/app/study/index.tsx`, `src/app/notifications.tsx`, `src/features/home/{ActivityRow,ActivityIcon,StudyHeader,layout}.tsx|ts`, `src/ui/tokens.ts`
- 기준 문서: `app/docs/STUDY_HOME_UI.md`

`file`로 실제 및 참조 캡처가 모두 JPEG임을 확인했고, 확장자도 `.jpg`로 일치한다. 리더·알림 기본 캡처는 관련 구현보다 늦은 시각에 생성됐다. 멤버의 구현 뒤 변경은 검토한 결과 스타일·값 변경이 없는 Biome 줄바꿈 정리이며, DOM 측정값과 현재 선언이 일치한다.

## 확인 결과

- 홈은 350×120 배너, 64px 헤더, 350×66 행, 12px radius와 1px `rgba(15,23,42,.08)` 테두리로 구현됐다. 멤버 행 시작은 y=266/392, 제목은 Pretendard 14/20, 상태는 12/18이다.
- 리더 현황 카드는 169×106 두 열 구조이며, 새 390×763 캡처에서 오른쪽 잘림 없이 배너·카드·행·하단 탭이 모두 보인다.
- 알림은 행 시작 y=84/187/290, 95px 행 높이, 첫 항목의 36px 브랜드 원과 18px 흰 벨, 일반 항목의 배경 없는 18px 공지 아이콘, 16/24·13/18·12/18 텍스트 계층을 갖는다.
- 홈·알림 모두 스크린샷이나 배경 이미지가 UI를 대체하지 않는다. 배너의 360×360 RGBA PNG만 `Image` 요소로 사용하고, 텍스트·상태·아이콘·행은 라이브 UI다.
- 320px 멤버 화면에서 한국어는 단어 경계에서만 줄바꿈되며, 잘림·겹침·가로 넘침이 보이지 않는다.
- 일반 색상·타이포그래피·간격·radius는 `ui/tokens.ts`를, Figma 화면 고유 치수는 `features/home/layout.ts`를 사용한다. 화면별 측정값을 컴포넌트 소유 값으로 분리한 방식은 이번 범위의 재사용성과 추적 가능성에 충분하다.

## findings

### CRITICAL

없음.

### HIGH

없음.

### MEDIUM

없음.

### LOW

없음.

## 미검증 및 한계

- Figma 증거는 61% 축소된 편집기 전체 1249×675이고 실제는 OS 영역을 제외한 390×763이다. 동일 프레임·배율이 아니므로 픽셀 diff 점수는 산정하지 않았다.
- iOS/Android 네이티브 기기 렌더링은 확인하지 않았으며, 이번 판정은 웹 캡처와 제공된 실행 증거에 한정한다.

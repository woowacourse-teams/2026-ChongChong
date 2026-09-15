> 보관 안내: 아래 이미지 파일명은 당시 검증 이력이다. 검증 이미지는 사용자 요청으로 제거했으며, 현재 열 수 있는 첨부 파일이 아니다.

# #323 공지 UI — Visual QA Pass B (재검토, 읽기 전용)

## 판정: APPROVE

현재 구현과 최신 증거에서 차단할 시각·구조·토큰 결함은 발견하지 못했다.

## 검토 근거

- 원본 해상도로 직접 확인한 기준/actual 이미지: `figma-summary-web.png`, `leader-summary.jpg`, `leader-list.jpg`, `delete.jpg`, `member-single-320.jpg`, `member-nontarget.jpg`, `member-list.jpg`, `member-reading.jpg`, `member-read.jpg`, `member-toast.jpg`, `leader-detail.jpg`, `editor.jpg`, `calendar.jpg`, `time.jpg`, `empty.jpg`, `figma-delete.jpg`.
- 이미지 시그니처·뷰포트는 정상이다. Figma 요약 원본은 PNG 390×763이고 actual은 네이티브 JPEG 390×763(단일 멤버 상세 320×763)다. JPEG의 색공간/압축 차이는 색상 불일치 근거로 쓰지 않았다. 이 환경의 PNG 전용 `visual-qa.mjs image-diff`도 JPEG actual을 입력받지 않는다.
- 코드: `app/src/features/notices/{layout.ts,NoticeCard.tsx,NoticeSummary.tsx,NoticeReader.tsx,NoticeIcon.tsx,ImageAttachments.tsx,ReminderSheet.tsx}`, `app/src/app/notices/*`, `app/src/app/study/notices.tsx`, 공통 primitives/tokens.

## GOOD

- 화면을 스크린샷·raster background로 대체하지 않았다. 목록(`NoticeCard`), 리더 요약(`NoticeSummary`), 상세/읽음(`NoticeReader`), 작성(`ImageAttachments`, `ReminderSheet`)이 실제 React Native 컴포넌트 트리다. 이미지 자산은 실제 첨부 이미지/썸네일에만 쓰인다.
- 공통 색상·간격·반경·타이포는 `tokens.ts`와 공통 primitive를 재사용한다. 공지 전용의 Figma 프레임 치수는 전역 변수로 위장하지 않고 `layout.ts:1-35`에 이름·노드 출처와 함께 화면 계약으로 분리했다. 이전의 HIGH 토큰 지적은 해소됐다.
- `NoticeIcon.tsx:27-42`는 XML path 자체는 보존하되 런타임에 `t.color.brand/text/tertiary/placeholder`를 주입한다. 이전 raw 색상 토큰 우회 지적도 해소됐다.
- `ImageAttachments.tsx:80-81`은 `t.size.control`과 `t.space.controlVertical`을 참조하고, 이전 미사용 원시 `image` 스타일도 제거됐다. 이미지 업로드 행 역시 공통 토큰 경로를 따른다.
- 새 `member-nontarget.jpg`(03:44:39)는 `NoticeReader.tsx`(03:42:26)보다 새롭다. 하단 읽음 UI가 없고 수평 이미지 갤러리가 잘리는 비대상 상태가 실제 `!leader && self` 분기(`NoticeReader.tsx:110-129`)와 맞는다.
- 새 멤버 목록/읽는 중/읽음/toast 캡처(03:43:46~03:44:17)는 각각 현재 목록·상세 코드보다 새롭다. 직접 대조한 결과, 읽지 않음/미대상 배지, 끝까지 읽기 전 footer, 완료 footer, toast의 계층·문구·사진 비율은 코드 상태와 일치한다.
- 03:47:14~03:48:03에 갱신된 리더 목록·요약·상세·삭제, 작성, 날짜, 시간 actual을 원본 해상도로 다시 직접 확인했다. 모두 마지막 관련 소스보다 새롭다. 목록의 20px 좌우 여백·350px 폭·168px 카드·80px 썸네일, 요약의 제목·날짜·2분할 탭·진행 막대·77px 대상자 카드, 삭제의 310px급 중앙 레이어·스크림·2분할 액션, 작성의 52px 입력/파일 행, 날짜 선택의 44px·반경 12 선택 행, 시간 선택의 계층은 목표 구조와 일치한다.
- 새 `empty.jpg`(03:50:46)는 `study/notices.tsx`(03:43:14)보다 새롭다. 직접 확인한 토끼 150px 일러스트와 문구의 수직 위치는 문서의 웹 390×763 좌표 계약과 맞고, 빈 상태에 리더 작성 버튼이 노출되지 않는다.
- 날짜 루트 `1702:18848`과 그 하위 선택 표시 레이어 `1702:18896`은 서로 다른 근거다. 문서의 루트 표와 선택 표시의 세부 적용값은 충돌로 보지 않았다.

## 심각도별 결과

- CRITICAL: 없음
- HIGH: 없음
- MEDIUM: 없음
- LOW: 없음

## 추천

- recommendation: **APPROVE**
- blockers: 없음

브라우저·네이티브 앱 조작은 하지 않았다. 실제 이미지 선택, 작성/수정/삭제 네비게이션 및 스크롤 이벤트의 런타임 동작은 UI-only mock 범위 밖이라 실행 검증하지 않았다.

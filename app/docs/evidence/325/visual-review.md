# Visual QA pass B 최종

VERDICT: PASS
CONFIDENCE: HIGH (제공된 캡처와 소스 범위)

## 결론
두 건의 CJK 줄바꿈 문제를 수정 후 캡처로 직접 확인했다. 320px 목록과 1280×900 목록·상세에서 추가 차단 결함은 관찰되지 않았다.

## 해결된 문제
- `submission-wrap-fixed.jpg`: 동일 제출 내용의 마지막 `올려주세요.`가 한 어절로 유지된다. 기존 `요.` 단독 행이 사라졌다. `styles.ts:92-98`의 웹 keep-all/anywhere 적용 확인.
- `list-320-fixed.jpg`: 첫 카드 설명이 `됩니다....`로 표시되어 기존 `다....` 단독 음절 행이 사라졌다. `AssignmentCard.tsx:122-127`의 description 웹 keep-all/anywhere 적용 확인. 목록의 2행 제한은 유지된다.

## 시각 검증
- `list-320-fixed.jpg`: 카드·배지·버튼·헤더·하단 4개 탭이 수평으로 잘리거나 겹치지 않는다.
- 교체된 `list-desktop.jpg`: 1280×900에서 카드와 버튼이 중앙의 제한된 콘텐츠 폭을 유지한다. 헤더 정렬과 하단 탭도 정상이다.
- `detail-desktop.jpg`: 제목·요약/상세 탭·내용/방법 블록·내 제출·제출 수정 버튼이 중앙 콘텐츠 폭을 따르며 한글 잘림이 없다.
- 기존 직접 검토한 390px 캡처의 카드 350×184, radius16, border1, 좌우20 여백과 공통 Pretendard/primary #172033/brand #00C471/border rgba(15,23,42,0.08) 판정 유지.
- 공지→과제 문구와 과제 탭 활성화는 의도된 교정이다.

## 근거와 한계
기존 이미지 상세 검토는 /tmp/assignment325-visual.md에 기록했다. 이번에는 list-320-fixed.jpg, 교체된 list-desktop.jpg, detail-desktop.jpg를 직접 열었다. 제출 입력란은 앞선 재검토에서 submission-wrap-fixed.jpg를 직접 열었다. 수정 전 submission-file.jpg와 list-320.jpg는 이력 증거다.

Figma 캡처는 브라우저 UI가 포함된 축소 캔버스이므로 실제 렌더와 정확한 픽셀 점수를 산정하지 않았다. 유효한 image-diff JSON/hotspot 자료는 없고 JPEG로 알파 채널을 검증하지 않았다. 이 PASS는 시각적 충실도와 CJK 검토이며 실제 상호작용·스크롤 및 타입/린트 검증은 루트/별도 pass A의 증거를 사용해야 한다. 검토자는 소스를 변경하지 않았다.

BLOCKING: 없음.

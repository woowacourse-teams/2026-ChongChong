# 공지

[← v2 개요](README.md) · [v1 대비 변경](changes.md)

## 디자인에 있는 기능

- 리더가 공지를 작성·수정·삭제한다.
- 리더는 ‘요약’에서 확인 현황을, ‘상세’에서 본문을 확인한다.
- 공지에 이미지를 추가하고 첨부한 항목을 제거할 수 있다.
- 리마인드 날짜·시간을 선택하고 여러 예약을 추가·제거한다.
- 스터디원은 공지 본문과 첨부 이미지를 조회한다.
- 읽는 중에는 진행률을, 읽음 완료 시에는 완료 상태와 토스트를 표시한다.
- 확인 대상이 아닌 스터디원도 본문을 열람하는 별도 상태가 있다.

## 현재 디자인의 정책

- 작성 후 가입한 스터디원에게 이전 글을 보여주되 확인 대상으로 할당하지 않는 방향이 메모에 명시되어 있다.
- 리마인드 안내는 설정 시각까지 읽지 않은 스터디원을 대상으로 한다.
- 대상이 아닌 사용자의 열람을 읽음 현황에 포함하지 않도록 조회와 확인 대상을 분리한다.

> [!IMPORTANT]
> 읽는 중 화면은 ‘끝까지 읽으면 읽음으로 표시돼요’라고 안내하지만, 일부 본문·작성 도움말에는 읽음 버튼 표현이 남아 있다.
> 자동 읽음 처리와 버튼 확인 중 최종 동작을 확정해야 한다. v1은 스크롤 완료 시 자동 처리한다.

## v1에서 추가·변경할 기능

- 이미지 업로드·연결·조회·제거와 작성 화면을 추가한다.
- 예약 처리·알림 생성·Web Push 발송은 서버에 구현되어 있다. 웹 날짜·시간 예약 입력은 남아 있다.
- 과거 공지 목록·상세 열람과 `NOT_ASSIGNED` 처리는 서버와 웹에 반영됐다. 대상이 아닌 사용자는 읽음 요청을 보내지 않는다.
- 리더 요약·상세 탭, 읽음 진행률·자동 완료·토스트는 웹에 반영됐다. 이미지 첨부 화면은 남아 있다.

현재 구현 근거: [공지 서비스](../../../backend/src/main/java/withoutc/chongchong/notice/service/NoticeService.java), [멤버 상세](../../../frontend/src/features/notice/components/MemberNoticeDetailContent.tsx).

> [!NOTE]
> 이미지 형식·용량·개수와 확대 방식은 미확정이다. 서버의 예약 시각은 미래여야 하며 중복 시각은 하나로 합친다. 수정 시 생략·null은 유지하고 빈 배열은 대기 예약을 제거한다.

## Figma 근거

- [03-02-01 리더 요약](https://www.figma.com/design/sT7K2tOQl8JtyHzuwT0nnl?node-id=1702-20772)
- [03-03-01 공지 작성·수정](https://www.figma.com/design/sT7K2tOQl8JtyHzuwT0nnl?node-id=1702-18804)
- [03-03-02 날짜 선택](https://www.figma.com/design/sT7K2tOQl8JtyHzuwT0nnl?node-id=1702-18848)
- [03-03-03 시간 선택](https://www.figma.com/design/sT7K2tOQl8JtyHzuwT0nnl?node-id=1702-18987)
- [03-02-05 읽는 중](https://www.figma.com/design/sT7K2tOQl8JtyHzuwT0nnl?node-id=1702-19370)
- [03-02-06 대상 아님](https://www.figma.com/design/sT7K2tOQl8JtyHzuwT0nnl?node-id=1702-19358)
- [03-02-07 단일 이미지](https://www.figma.com/design/sT7K2tOQl8JtyHzuwT0nnl?node-id=1702-19298)
- [03-02-08 읽음 완료 토스트](https://www.figma.com/design/sT7K2tOQl8JtyHzuwT0nnl?node-id=1702-19336)

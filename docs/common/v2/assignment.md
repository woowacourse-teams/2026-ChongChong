# 과제

[← v2 개요](README.md) · [v1 대비 변경](changes.md)

## 디자인에 있는 기능

- 리더가 제목·내용·제출 방법·마감 시각을 입력하고 과제를 작성·수정·삭제한다.
- 과제 이미지와 리마인드 예약을 추가·제거한다.
- 과제별로 제출물 ‘공개·비공개’를 선택한다.
- ‘리드도 과제를 제출할게요’ 선택으로 리더의 제출 참여를 설정한다.
- 요약 화면은 제출 현황과 제출물 목록, 상세 화면은 과제 안내와 내 제출을 보여준다.
- 제출 화면에서 내용·링크·파일을 입력하고 첨부 파일을 제거할 수 있다.
- 제출 완료 후 내 제출물을 조회하고 수정하는 진입점을 제공한다.

## 역할·상태별 표시

| 상태 | 디자인에 표시된 내용 |
| --- | --- |
| 리더·제출 참여 | 내 제출과 제출 완료·미완료 현황 |
| 리더·제출 대상 아님 | 운영용 과제 상세 상태 |
| 스터디원·미제출 | 미제출 상태와 제출 진입 |
| 스터디원·비공개 | 내 제출만 표시 |
| 스터디원·공개 | 내 제출, 제출 현황, 완료·미완료 멤버, 제출물 상세 보기 |
| 스터디원·제출 완료 | 내용·링크와 제출물 수정 진입 |

## 현재 구현과 남은 작업 (2026-09-30)

- 리더 참여 선택·내 제출·편집 화면과 서버의 대상·집계·리마인드 처리는 구현되어 있다.
- 공개 범위는 현재 브랜치의 서버에 구현되어 있다. 공개 과제의 일반 멤버는 제출 완료 목록·상세를 조회할 수 있지만 제출·미제출 현황 API는 리더 전용이다. 디자인의 일반 멤버 현황 화면과는 차이가 있다.
- 웹은 `submissionVisibility` 선택·전송·응답 처리가 없어 공개 기능 연결이 남아 있다. 특히 생성 API의 필수 값이므로 현재 웹 생성 요청은 이 브랜치 서버 계약과 맞지 않는다.
- 과제 이미지·제출 파일 업로드와 조회·수정·접근 권한을 추가한다.
- 가입 전 과제 조회와 `NOT_ASSIGNED` 응답·웹 표시를 구현했다. 해당 사용자는 제출 대상으로 자동 할당하지 않는다.
- 리더 요약·상세 탭과 내 제출·편집 흐름은 연결되어 있다. 첨부와 일반 멤버의 공개 제출물 화면은 남아 있다.

근거: [과제 모델](../../../backend/src/main/java/withoutc/chongchong/assignment/entity/Assignment.java), [조회 권한](../../../backend/src/main/java/withoutc/chongchong/assignment/policy/AssignmentAccessPolicy.java), [웹 과제 폼](../../../frontend/src/features/assignment/components/AssignmentForm.tsx), [리더 상세](../../../frontend/src/features/assignment/components/LeaderAssignmentDetailContent.tsx).

> [!IMPORTANT]
> 디자인 메모에는 ‘과제 제출 형식 추가’가 있지만 작성 화면의 ‘제출 방법’은 안내문 입력으로 보인다.
> 파일 첨부 지원과 허용 제출 형식을 선택·강제하는 정책을 구분한다. 형식 선택 UI·단일/복수 선택 여부는 미확정이다.

> [!NOTE]
> 공개 범위 변경은 이후 조회에 반영된다. 미제출·미할당 멤버도 공개 과제의 타인 제출 완료 내용을 조회할 수 있다.
> 리더 미포함으로 변경하면 기존 리더 제출물도 삭제된다. 파일 제한과 리더 양도 시 대상 승계는 추가 결정이 필요하다.
> 마감 후 제출 차단은 디자인에서 확인되지 않았으며 v1은 차단하지 않는다.
> ‘제출물 수정’ 프레임에 미제출 안내가 남아 있어 최종 편집 화면도 확인한다.

## Figma 근거

- [04-03-01 과제 작성·수정](https://www.figma.com/design/sT7K2tOQl8JtyHzuwT0nnl?node-id=1702-19050)
- [04-04-01 과제 제출](https://www.figma.com/design/sT7K2tOQl8JtyHzuwT0nnl?node-id=1702-21247)
- [04-04-02 제출물 수정](https://www.figma.com/design/sT7K2tOQl8JtyHzuwT0nnl?node-id=1702-20953)
- [04-02-08 내 제출만 표시](https://www.figma.com/design/sT7K2tOQl8JtyHzuwT0nnl?node-id=1702-21202)
- [04-02-09 전체 제출물 공개](https://www.figma.com/design/sT7K2tOQl8JtyHzuwT0nnl?node-id=1702-21149)
- [04-02-05 리더·대상 아님](https://www.figma.com/design/sT7K2tOQl8JtyHzuwT0nnl?node-id=1702-20993)
- [04-02-06 리더·제출 완료](https://www.figma.com/design/sT7K2tOQl8JtyHzuwT0nnl?node-id=1702-20843)
- [기능 추가 메모](https://www.figma.com/design/sT7K2tOQl8JtyHzuwT0nnl?node-id=1586-5861)

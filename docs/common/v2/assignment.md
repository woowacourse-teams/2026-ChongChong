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
| 스터디원·미제출 | 마감 전 미제출과 마감 후 미제출을 구분하고 모두 제출 진입 허용 |
| 스터디원·비공개 | 내 제출만 표시 |
| 스터디원·공개 | 내 제출, 제출 현황, 완료·미완료 멤버, 제출물 상세 보기 |
| 스터디원·제출 완료 | 정상 제출과 지각 제출을 구분하고 내용·링크와 제출물 수정 진입 제공 |

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
> 마감 후에도 제출을 허용하며 정상 제출·지각 제출·마감 후 미제출을 구분한다. 과제 조회의 상태 표시는 2026-10-03 사용자 확인에 따라 Figma에도 반영된 확정 정책이다.
> ‘제출물 수정’ 프레임에 미제출 안내가 남아 있어 최종 편집 화면도 확인한다.

## 2026-10-03 백엔드 구현 반영

과제 조회의 제출 상태 구분은 2026-10-03 사용자 확인에 따라 Figma에도 반영된 확정 정책이다. 아래는 해당 정책과 백엔드 구현·테스트를 기준으로 한 API 연동 기준이다. Figma 반영과 별개로 프론트엔드 구현·배포 완료 여부는 이번 작업에서 검증하지 않았다.

### 제출 상태와 마감 정책

| API 상태 | 판정 기준 | 화면 연동 기준 |
| --- | --- | --- |
| `NOT_ASSIGNED` | 제출 대상이 아님 | 과제는 열람하되 제출 폼과 요청을 비활성화 |
| `NOT_SUBMITTED` | 제출 대상이며 미제출, 현재 시각이 마감 시각 이하 | 미제출 표시와 제출 진입 |
| `MISSING` | 제출 대상이며 미제출, 현재 시각이 마감 시각을 지남 | 마감 후 미제출 표시, 제출은 허용 |
| `SUBMITTED` | 최초 제출 시각이 마감 시각 이하 | 정상 제출 표시와 제출물 조회·수정 |
| `LATE_SUBMITTED` | 최초 제출 시각이 마감 시각을 지남 | 지각 제출 표시와 제출물 조회·수정 |

- 마감 후에도 제출과 수정이 가능하다. 마감 시각과 같은 시각에 제출하면 정상 제출이다.
- 재제출은 내용·링크를 덮어쓰지만 최초 제출 시각을 유지한다. 내용 수정도 최초 제출 시각을 바꾸지 않는다.
- 마감 시각을 변경하면 변경된 마감 시각과 최초 제출 시각으로 정상·지각 제출을 다시 판정한다.
- 과제 목록과 내 제출 조회는 위 5개 상태를 반환한다. 제출물 목록은 제출된 항목만 반환하며 각 항목에 `SUBMITTED` 또는 `LATE_SUBMITTED`를 포함한다.
- 리더의 완료 인원과 전체 완료 여부는 정상·지각 제출을 모두 완료로 집계한다. 마감 후 미제출자는 미완료에 포함한다.

### 최초 제출 알림

- 최초 제출 시 제출자를 제외한 스터디 리더들에게 알림을 생성한다. 제출 대상인 리더가 제출해도 같은 규칙을 적용한다.
- 재제출과 내용 수정에는 제출 알림을 추가하지 않는다.
- 같은 제출에 동시 요청이 들어와도 최초 제출 판단과 알림 생성이 중복되지 않도록 잠금 조회 후 같은 트랜잭션에서 처리한다.
- 리더 제출 시 다른 리더에게 알림을 보낼지에 대한 최종 정책은 코드 TODO로 남아 있다. 위 내용은 현재 구현 동작이다.

### 검증과 남은 연동

관련 테스트 155개가 통과했다. 프론트엔드의 5개 상태 표시와 제출 후 상태 갱신은 이번 작업에서 검증하지 않았다.

[제출 상태 모델](../../../backend/src/main/java/withoutc/chongchong/assignment/entity/AssignmentSubmission.java) · [제출 서비스](../../../backend/src/main/java/withoutc/chongchong/assignment/service/AssignmentSubmissionService.java) · [API 상태 명세](../../../backend/docs/openapi/components/schemas/assignment-submissions.yaml) · [상태 경계 테스트](../../../backend/src/test/java/withoutc/chongchong/assignment/entity/AssignmentSubmissionTest.java)

## Figma 근거

- [04-03-01 과제 작성·수정](https://www.figma.com/design/sT7K2tOQl8JtyHzuwT0nnl?node-id=1702-19050)
- [04-04-01 과제 제출](https://www.figma.com/design/sT7K2tOQl8JtyHzuwT0nnl?node-id=1702-21247)
- [04-04-02 제출물 수정](https://www.figma.com/design/sT7K2tOQl8JtyHzuwT0nnl?node-id=1702-20953)
- [04-02-08 내 제출만 표시](https://www.figma.com/design/sT7K2tOQl8JtyHzuwT0nnl?node-id=1702-21202)
- [04-02-09 전체 제출물 공개](https://www.figma.com/design/sT7K2tOQl8JtyHzuwT0nnl?node-id=1702-21149)
- [04-02-05 리더·대상 아님](https://www.figma.com/design/sT7K2tOQl8JtyHzuwT0nnl?node-id=1702-20993)
- [04-02-06 리더·제출 완료](https://www.figma.com/design/sT7K2tOQl8JtyHzuwT0nnl?node-id=1702-20843)
- [기능 추가 메모](https://www.figma.com/design/sT7K2tOQl8JtyHzuwT0nnl?node-id=1586-5861)

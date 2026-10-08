# 역할과 활동 대상

[← 현재 PRD](README.md) · [구현 범위](scope.md)

각 스터디에는 리더(`LEADER`)와 일반 멤버(`MEMBER`)가 있다. 역할에 따라 할 수 있는 일이 다르다.

| 행동 | 리더 | 일반 멤버 |
| --- | --- | --- |
| 공지·과제 작성·수정·삭제 | 허용 | 거부 |
| 공지·과제 열람 | 허용 | 현재 소속이면 가입 전 글도 허용 |
| 공지 읽음 | 확인 대상 제외 | 공지 작성 당시 확인 대상인 멤버만 가능 |
| 과제 제출 | 리더 참여를 선택해 제출 대상이 된 경우 | 제출 대상인 경우 |
| 읽음·제출 현황 전체 조회 | 허용 | 거부 |
| 제출 완료 목록 조회 | 허용 | 공개 과제만 허용 |
| 제출물 상세 조회 | 허용 | 본인 또는 공개 과제의 제출 완료 항목 |
| 제출물 수정 | 본인 제출물만 | 본인 제출물만 |
| 멤버 방출·스터디 수정·삭제 | 허용 | 거부 |
| 스터디 탈퇴 | 거부 | 허용 |
| 회원탈퇴 | 리더인 스터디가 하나라도 있으면 거부 | 허용 |

공지 확인 대상과 과제 제출 대상은 글을 작성할 때 정해진다. 공지는 당시의 일반 멤버가 확인 대상이 되고, 과제는 리더가 선택한 참여 범위에 따라 제출 대상이 정해진다.

나중에 가입한 멤버도 이전 글을 읽을 수 있지만, 그 공지를 확인하거나 과제를 제출해야 하는 대상에는 추가되지 않는다. 서버는 이 상태를 `NOT_ASSIGNED`로 표시한다.
과제가 공개되어 있다면 제출 대상이 아니거나 아직 제출하지 않은 멤버도 다른 멤버의 제출물을 볼 수 있다.
현재는 리더 권한을 다른 멤버에게 넘기는 기능이 없다.

근거: [공지 권한](../../../backend/src/main/java/withoutc/chongchong/notice/policy/NoticeAccessPolicy.java), [과제 권한](../../../backend/src/main/java/withoutc/chongchong/assignment/policy/AssignmentAccessPolicy.java), [멤버 서비스](../../../backend/src/main/java/withoutc/chongchong/study/service/StudyMemberService.java), [계정 서비스](../../../backend/src/main/java/withoutc/chongchong/user/service/UserService.java).

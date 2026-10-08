# 멤버 관리

[← 현재 PRD](README.md) · [구현 범위](scope.md)

## 조회와 초대

스터디에 참여한 멤버는 멤버 목록과 초대 링크를 확인할 수 있다. 목록에서는 리더와 일반 멤버를 구분하고, 최대 50명까지 참여할 수 있음을 안내한다.
초대 링크에 담긴 토큰은 발급 후 3일 동안 유효하다.

## 방출과 탈퇴

리더는 같은 스터디의 일반 멤버를 방출할 수 있다. 리더를 방출할 수는 없다.
일반 멤버는 스터디 관리 화면에서 탈퇴할 수 있다. 리더는 스터디를 탈퇴할 수 없다.
멤버가 방출되거나 탈퇴하면 스터디 소속 정보와 그 멤버의 스터디 알림을 삭제한다.
방출된 멤버를 따로 차단하지는 않으므로, 초대 링크로 다시 참여하는 것을 영구적으로 막지는 않는다.

현재는 리더 권한을 다른 멤버에게 넘기는 기능이 없다.

근거: [멤버 서비스](../../../backend/src/main/java/withoutc/chongchong/study/service/StudyMemberService.java), [웹 멤버 목록](../../../frontend/src/features/member/pages/MemberListPage.tsx), [관리 메뉴](../../../frontend/src/features/study/components/ManagementList.tsx), [초대 토큰](../../../backend/src/main/java/withoutc/chongchong/study/token/StudyInviteTokenProvider.java).

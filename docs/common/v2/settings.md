# 스터디 관리

[← 현재 PRD](README.md) · [구현 범위](scope.md)

스터디 관리 화면에서 스터디 이름과 설명을 확인할 수 있다. 리더와 일반 멤버에게는 서로 다른 메뉴를 보여준다.

| 사용자 | 제공 메뉴 |
| --- | --- |
| 리더 | 스터디 정보 수정, 스터디 삭제 |
| 일반 멤버 | 스터디 탈퇴 |

리더는 스터디 이름을 최대 15자, 설명을 최대 30자까지 수정할 수 있다.
수정 API에 `null`로 보낸 항목은 기존 값을 유지한다. 설명을 지우려면 빈 문자열을 보낸다.

스터디 삭제를 선택하면 먼저 확인 창을 띄운다. 서버는 요청한 사용자가 리더인지 확인하고 관련 알림을 정리한 뒤 스터디를 삭제한다.
일반 멤버가 탈퇴할 때도 확인 창을 띄운다. 리더는 스터디를 탈퇴할 수 없다.

스터디별 프로필 수정 메뉴는 사용할 수 없다.
이름을 바꾸려면 [마이페이지](account.md)에서 계정 이름을 수정해야 한다. 변경한 이름은 참여 중인 모든 스터디에 함께 반영된다.

근거: [관리 화면](../../../frontend/src/features/study/pages/StudyManagementPage.tsx), [역할별 메뉴](../../../frontend/src/features/study/components/ManagementList.tsx), [수정 화면](../../../frontend/src/features/study/pages/StudyEditPage.tsx), [스터디 모델](../../../backend/src/main/java/withoutc/chongchong/study/entity/Study.java), [서비스](../../../backend/src/main/java/withoutc/chongchong/study/service/StudyService.java).

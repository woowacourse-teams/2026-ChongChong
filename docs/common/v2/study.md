# 스터디 생성·참여·홈

[← 현재 PRD](README.md) · [구현 범위](scope.md)

## 사용자 흐름

1. 내 스터디 목록에서 기존 스터디를 선택하거나 이름·설명을 입력해 새로 만든다.
2. 생성자는 리더가 된다. 초대 링크로 참여한 사용자는 일반 멤버가 된다.
3. 리더는 홈에서 아직 완료되지 않은 공지·과제와 완료한 인원을 확인한다. 일반 멤버는 자신이 아직 읽지 않은 공지와 제출하지 않은 과제를 확인한다.
4. 스터디 내부에서 공지·과제·멤버·관리 화면으로 이동한다.

## 입력과 참여 제한

| 항목 | 현재 정책 |
| --- | --- |
| 스터디 이름 | 필수, 공백만 입력 불가, 최대 15자 |
| 설명 | 선택, 최대 30자 |
| 사용자별 소속 수 | 생성·참여 합계 최대 50개 |
| 스터디 인원 | 리더 포함 최대 50명 |
| 초대 링크 | 소속 멤버가 조회 가능, 토큰 발급 시점부터 3일 유효 |
| 중복 참여 | 거부 |

초대 링크가 만료되었거나 올바르지 않으면 참여할 수 없다.
서버는 초대 토큰을 확인하고, 스터디가 존재하는지, 이미 참여한 사용자인지, 인원 제한을 넘는지 확인한다.
새로 가입한 멤버도 이전 공지와 과제를 읽을 수 있다. 다만 이전 공지의 확인 대상이나 과제의 제출 대상이 되지는 않는다.

## 목록과 홈에 표시하는 정보

내 스터디 목록에는 최근에 참여한 스터디부터 표시한다. 각 스터디의 인원과 남은 공지·과제 수도 함께 보여준다.
리더에게는 대상 멤버가 모두 완료했는지를 기준으로 남은 활동 수를 보여준다. 일반 멤버에게는 본인이 해야 하는 활동 중 아직 완료하지 않은 수를 보여준다.
과제는 마감 전에 제출했든 늦게 제출했든 완료한 것으로 센다.

근거: [스터디 서비스](../../../backend/src/main/java/withoutc/chongchong/study/service/StudyService.java), [참여 서비스](../../../backend/src/main/java/withoutc/chongchong/study/service/StudyMemberService.java), [스터디 모델](../../../backend/src/main/java/withoutc/chongchong/study/entity/Study.java), [초대 토큰](../../../backend/src/main/java/withoutc/chongchong/study/token/StudyInviteTokenProvider.java), [웹 입력 폼](../../../frontend/src/features/study/components/StudyForm.tsx), [홈](../../../frontend/src/features/study/pages/StudyDetailPage.tsx).

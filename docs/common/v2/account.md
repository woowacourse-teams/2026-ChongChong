# 마이페이지

[← 현재 PRD](README.md) · [구현 범위](scope.md)

## 조회와 이름 변경

마이페이지에서는 내 이름과 프로필 이미지를 확인하고 이름을 바꿀 수 있다.
새 이름은 최대 8자까지 입력할 수 있다. 웹은 앞뒤 공백을 제거한 뒤 서버에 보내며, 비어 있거나 공백만 있는 이름은 저장할 수 없다.
소셜 계정으로 처음 가입할 때 가져오는 이름은 최대 255자까지 저장하지만, 사용자가 직접 수정할 때는 8자 제한을 적용한다.

이름을 바꾸면 계정 이름과 참여 중인 모든 스터디의 멤버 이름이 함께 바뀐다. 서버는 이 변경을 하나의 트랜잭션으로 처리한다.
스터디마다 다른 이름을 쓰거나 프로필 이미지를 바꾸는 화면은 없다.

## 계정 메뉴

- 푸시 스위치로 현재 브라우저의 알림을 켜거나 끈다. 같은 계정으로 이용하는 다른 브라우저의 설정까지 바뀌지는 않는다.
- 로그아웃을 선택하면 현재 브라우저의 푸시 구독을 해제한 뒤 서버에 로그아웃을 요청한다.
- 개인정보처리방침 링크와 이메일 지원 링크를 제공한다.
- 회원탈퇴를 선택하면 확인 창을 띄운다. 리더를 맡은 스터디가 하나라도 있으면 탈퇴할 수 없다.
- 회원탈퇴가 완료되면 서버에서 사용자 알림과 사용자 정보를 삭제한다. 연결된 데이터는 데이터베이스에 설정된 관계에 따라 삭제한다.

근거: [계정 서비스](../../../backend/src/main/java/withoutc/chongchong/user/service/UserService.java), [이름 검증](../../../backend/src/main/java/withoutc/chongchong/user/entity/User.java), [웹 API](../../../frontend/src/features/user/api.ts), [이름 입력](../../../frontend/src/features/user/components/ProfileNameForm.tsx), [계정 메뉴](../../../frontend/src/features/user/components/AccountMenuSection.tsx).

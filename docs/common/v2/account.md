# 마이페이지

[← 현재 PRD](README.md) · [구현 범위](scope.md)

## 조회와 이름 변경

마이페이지에서는 내 이름과 프로필 이미지를 확인하고 이름을 바꿀 수 있다.
새 이름은 최대 8자까지 입력할 수 있다. 웹은 앞뒤 공백을 제거한 뒤 서버에 보내며, 비어 있거나 공백만 있는 이름은 저장할 수 없다.
소셜 계정으로 처음 가입할 때 가져오는 이름은 최대 255자까지 저장하지만, 사용자가 직접 수정할 때는 8자 제한을 적용한다.

이름을 바꾸면 계정 이름과 참여 중인 모든 스터디의 멤버 이름이 함께 바뀐다. 서버는 이 변경을 하나의 트랜잭션으로 처리한다.
스터디마다 다른 이름을 쓰거나 프로필 이미지를 바꾸는 화면은 없다.

## 홈 프로필 이미지 API

서버는 `PATCH /api/users/me/profile-image`에 업로드 완료된 본인 소유 파일의 `fileId`를 받아 홈 이미지를 변경한다. 기본 정책은 JPEG·PNG·WebP 한 장, 0바이트 초과·최대 5MiB이며 다른 게시물·스터디·프로필에 사용 중인 새 파일은 거부한다.
`DELETE /api/users/me/profile-image`는 업로드 연결과 소셜 이미지 URL을 함께 해제하고 본문 없는 204를 반환한다. 이후 프로필 조회의 `profileImageUrl`은 null이며, 소셜 이미지로 복원하는 기능은 아니다.
홈 이미지 변경·초기화는 현재 참여 중인 모든 스터디의 해당 사용자 이미지에 반영된다. 신규 스터디 생성·참여 후에도 현재 홈 이미지를 사용한다. 스터디별로 다른 사용자 이미지를 설정하는 기능은 없다. 스터디 자체의 대표 이미지와는 별개다.
파일은 연결만 해제하며 즉시 S3에서 삭제하지 않는다. 업로드 이미지 조회 URL이 만료되면 해당 조회 API를 다시 호출한다. 웹의 홈 이미지 변경·초기화 화면은 미연동이다.

근거: [프로필 이미지 서비스](../../../backend/src/main/java/withoutc/chongchong/user/service/UserProfileImageService.java), [이미지 조회](../../../backend/src/main/java/withoutc/chongchong/user/service/UserProfileImageUrlResolver.java), [API 계약](../../../backend/docs/openapi/paths/users/profile-image.yaml).

## 계정 메뉴

- 푸시 스위치로 현재 브라우저의 알림을 켜거나 끈다. 같은 계정으로 이용하는 다른 브라우저의 설정까지 바뀌지는 않는다.
- 로그아웃을 선택하면 현재 브라우저의 푸시 구독을 해제한 뒤 서버에 로그아웃을 요청한다.
- 개인정보처리방침 링크와 이메일 지원 링크를 제공한다.
- 회원탈퇴를 선택하면 확인 창을 띄운다. 리더를 맡은 스터디가 하나라도 있으면 탈퇴할 수 없다.
- 회원탈퇴가 완료되면 서버에서 사용자 알림과 사용자 정보를 삭제한다. 연결된 데이터는 데이터베이스에 설정된 관계에 따라 삭제한다.

근거: [계정 서비스](../../../backend/src/main/java/withoutc/chongchong/user/service/UserService.java), [이름 검증](../../../backend/src/main/java/withoutc/chongchong/user/entity/User.java), [웹 API](../../../frontend/src/features/user/api.ts), [이름 입력](../../../frontend/src/features/user/components/ProfileNameForm.tsx), [계정 메뉴](../../../frontend/src/features/user/components/AccountMenuSection.tsx).

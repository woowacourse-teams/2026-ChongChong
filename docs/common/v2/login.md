# 로그인

[← 현재 PRD](README.md) · [구현 범위](scope.md)

## 로그인과 로그아웃

사용자는 카카오 로그인 버튼을 눌러 로그인한다. 처음 이용하는 사용자라면 로그인 과정에서 계정을 만든다.
카카오 인증이 끝나면 웹이 인가 코드를 서버에 보내고, 서버가 발급한 액세스 토큰으로 로그인 상태를 유지한다. 토큰을 갱신할 때는 Refresh 쿠키를 사용한다.

로그아웃을 선택하면 현재 브라우저의 푸시 알림 구독을 해제한 뒤 서버에 로그아웃을 요청한다.

## 정책과 예외

- 현재 로그인할 수 있는 소셜 계정은 카카오다. Google·Apple 로그인은 제공하지 않는다.
- 웹은 로그인·인증 갱신·로그아웃을 요청할 때 요청 위조를 막기 위한 CSRF 토큰을 함께 보낸다.
- 인증 갱신에 실패하면 웹에 보관한 액세스 토큰을 지운다. 사용자는 다시 로그인해야 한다.
- 서버는 로그아웃할 때 인증 세션을 처리하고 Refresh 쿠키를 만료시킨다.
- 로그인해도 브라우저 푸시 알림이 자동으로 허용되지는 않는다. 푸시 알림은 [계정 메뉴](account.md)에서 관리한다.

근거: [웹 인증 요청](../../../frontend/src/features/login/api.ts), [로그인 화면](../../../frontend/src/features/login/pages/LoginPage.tsx), [인증 컨트롤러](../../../backend/src/main/java/withoutc/chongchong/auth/controller/AuthController.java), [소셜 로그인 서비스](../../../backend/src/main/java/withoutc/chongchong/auth/service/SocialLoginService.java), [카카오 연동](../../../backend/src/main/java/withoutc/chongchong/auth/social/kakao/KakaoSocialLoginClient.java).

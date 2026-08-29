package withoutc.chongchong.auth.social;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import withoutc.chongchong.auth.exception.AuthErrorCode;
import withoutc.chongchong.auth.exception.AuthException;
import withoutc.chongchong.auth.support.FakeSocialLoginClient;

class SocialLoginClientsTest {

    @Test
    @DisplayName("요청한 제공자에 해당하는 Client로 사용자를 인증한다")
    void authenticateWithRequestedProviderClient() {
        String credential = "same-credential";
        FakeSocialLoginClient googleClient = new FakeSocialLoginClient(SocialProvider.GOOGLE);
        FakeSocialLoginClient kakaoClient = new FakeSocialLoginClient(SocialProvider.KAKAO);
        googleClient.willSucceed(credential, createSocialUserInfo(SocialProvider.GOOGLE, "google-id"));
        SocialUserInfo expected = createSocialUserInfo(SocialProvider.KAKAO, "kakao-id");
        kakaoClient.willSucceed(credential, expected);
        SocialLoginClients clients = new SocialLoginClients(List.of(googleClient, kakaoClient));

        SocialUserInfo actual = clients.authenticate(new SocialLoginCommand(
                SocialProvider.KAKAO,
                credential
        ));

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    @DisplayName("등록되지 않은 제공자는 공통 Auth 오류로 거부한다")
    void rejectUnsupportedProvider() {
        SocialLoginClients clients = new SocialLoginClients(List.of(
                new FakeSocialLoginClient(SocialProvider.GOOGLE)
        ));

        AuthException exception = catchThrowableOfType(
                () -> clients.authenticate(new SocialLoginCommand(
                        SocialProvider.APPLE,
                        "credential"
                )),
                AuthException.class
        );

        assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.UNSUPPORTED_SOCIAL_PROVIDER);
    }

    @Test
    @DisplayName("Provider 인증 실패를 공통 Auth 오류로 반환한다")
    void rejectInvalidProviderAuthentication() {
        FakeSocialLoginClient googleClient = new FakeSocialLoginClient(SocialProvider.GOOGLE);
        googleClient.willFail("invalid-credential");
        SocialLoginClients clients = new SocialLoginClients(List.of(googleClient));

        AuthException exception = catchThrowableOfType(
                () -> clients.authenticate(new SocialLoginCommand(
                        SocialProvider.GOOGLE,
                        "invalid-credential"
                )),
                AuthException.class
        );

        assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.SOCIAL_AUTHENTICATION_FAILED);
    }

    @Test
    @DisplayName("같은 제공자의 Client를 중복 등록할 수 없다")
    void rejectDuplicateProviderClients() {
        FakeSocialLoginClient firstClient = new FakeSocialLoginClient(SocialProvider.GOOGLE);
        FakeSocialLoginClient secondClient = new FakeSocialLoginClient(SocialProvider.GOOGLE);

        assertThatThrownBy(() -> new SocialLoginClients(List.of(firstClient, secondClient)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("같은 제공자의 소셜 로그인 Client를 중복 등록할 수 없습니다.");
    }

    @Test
    @DisplayName("Client가 요청과 다른 제공자의 결과를 반환하면 실패한다")
    void rejectMismatchedProviderResult() {
        String credential = "credential";
        FakeSocialLoginClient googleClient = new FakeSocialLoginClient(SocialProvider.GOOGLE);
        googleClient.willSucceed(
                credential,
                createSocialUserInfo(SocialProvider.KAKAO, "kakao-id")
        );
        SocialLoginClients clients = new SocialLoginClients(List.of(googleClient));

        assertThatThrownBy(() -> clients.authenticate(new SocialLoginCommand(
                SocialProvider.GOOGLE,
                credential
        )))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("소셜 로그인 Client가 요청한 제공자와 다른 결과를 반환했습니다.");
    }

    @Test
    @DisplayName("소셜 로그인 요청이 없으면 인증할 수 없다")
    void rejectNullCommand() {
        SocialLoginClients clients = new SocialLoginClients(List.of());

        assertThatThrownBy(() -> clients.authenticate(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("소셜 로그인 요청은 필수입니다.");
    }

    @Test
    @DisplayName("Client 목록이 없더라도 Registry를 생성할 수 있다")
    void createWithEmptyClients() {
        SocialLoginClients clients = new SocialLoginClients(List.of());

        AuthException exception = catchThrowableOfType(
                () -> clients.authenticate(new SocialLoginCommand(
                        SocialProvider.GOOGLE,
                        "credential"
                )),
                AuthException.class
        );

        assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.UNSUPPORTED_SOCIAL_PROVIDER);
    }

    private SocialUserInfo createSocialUserInfo(
            SocialProvider provider,
            String providerUserId
    ) {
        return new SocialUserInfo(provider, providerUserId, "총총이", null);
    }
}

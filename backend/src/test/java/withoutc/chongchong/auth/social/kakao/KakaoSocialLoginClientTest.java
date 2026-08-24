package withoutc.chongchong.auth.social.kakao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import withoutc.chongchong.auth.exception.AuthErrorCode;
import withoutc.chongchong.auth.exception.AuthException;
import withoutc.chongchong.auth.social.SocialLoginCommand;
import withoutc.chongchong.auth.social.SocialProvider;
import withoutc.chongchong.auth.social.SocialUserInfo;
import withoutc.chongchong.auth.social.kakao.dto.KakaoUserInfoResponse;
import withoutc.chongchong.auth.social.kakao.dto.KakaoUserInfoResponse.KakaoAccount;
import withoutc.chongchong.auth.social.kakao.dto.KakaoUserInfoResponse.KakaoProfile;

class KakaoSocialLoginClientTest {

    private static final String AUTHORIZATION_CODE = "sensitive-authorization-code";
    private static final KakaoAccessToken ACCESS_TOKEN = new KakaoAccessToken("sensitive-kakao-access-token");

    private KakaoTokenClient tokenClient;
    private KakaoUserInfoClient userInfoClient;
    private KakaoSocialLoginClient socialLoginClient;

    @BeforeEach
    void setUp() {
        tokenClient = mock(KakaoTokenClient.class);
        userInfoClient = mock(KakaoUserInfoClient.class);
        socialLoginClient = new KakaoSocialLoginClient(tokenClient, userInfoClient);
    }

    @Test
    @DisplayName("KAKAO 제공자를 지원한다")
    void supportKakaoProvider() {
        assertThat(socialLoginClient.provider()).isEqualTo(SocialProvider.KAKAO);
    }

    @Test
    @DisplayName("Kakao 회원번호와 프로필을 SocialUserInfo로 변환한다")
    void authenticateKakaoUser() {
        KakaoUserInfoResponse response = userInfo(
                123456789L,
                "총총이",
                "https://example.com/profile.png"
        );
        givenKakaoUserInfo(response);

        SocialUserInfo socialUserInfo = socialLoginClient.authenticate(command());

        assertThat(socialUserInfo.provider()).isEqualTo(SocialProvider.KAKAO);
        assertThat(socialUserInfo.providerUserId()).isEqualTo("123456789");
        assertThat(socialUserInfo.displayName()).isEqualTo("총총이");
        assertThat(socialUserInfo.profileImageUrl()).isEqualTo("https://example.com/profile.png");
        verify(tokenClient).exchange(AUTHORIZATION_CODE);
        verify(userInfoClient).fetch(ACCESS_TOKEN);
    }

    @Test
    @DisplayName("Kakao 프로필 이미지가 없으면 null로 변환한다")
    void allowMissingProfileImage() {
        givenKakaoUserInfo(userInfo(123456789L, "총총이", null));

        SocialUserInfo socialUserInfo = socialLoginClient.authenticate(command());

        assertThat(socialUserInfo.profileImageUrl()).isNull();
    }

    @ParameterizedTest
    @MethodSource("invalidUserInfoResponses")
    @DisplayName("필수 Kakao 사용자 정보가 잘못되면 공통 소셜 인증 실패로 변환한다")
    void rejectInvalidUserInfo(KakaoUserInfoResponse response) {
        givenKakaoUserInfo(response);

        assertAuthenticationFailed(() -> socialLoginClient.authenticate(command()));
    }

    @Test
    @DisplayName("KAKAO가 아닌 요청은 Provider를 호출하지 않고 거부한다")
    void rejectDifferentProvider() {
        SocialLoginCommand command = new SocialLoginCommand(SocialProvider.GOOGLE, AUTHORIZATION_CODE);

        assertAuthenticationFailed(() -> socialLoginClient.authenticate(command));

        verifyNoInteractions(tokenClient, userInfoClient);
    }

    @Test
    @DisplayName("Kakao 사용자 정보 문자열에 회원번호와 프로필을 노출하지 않는다")
    void redactUserInfoFromString() {
        KakaoUserInfoResponse response = userInfo(
                123456789L,
                "sensitive-nickname",
                "https://example.com/sensitive-profile.png"
        );

        assertThat(response.toString())
                .isEqualTo("KakaoUserInfoResponse[REDACTED]")
                .doesNotContain("123456789", "sensitive-nickname", "sensitive-profile");
        assertThat(response.kakaoAccount().toString()).isEqualTo("KakaoAccount[REDACTED]");
        assertThat(response.kakaoAccount().profile().toString()).isEqualTo("KakaoProfile[REDACTED]");
    }

    private void givenKakaoUserInfo(KakaoUserInfoResponse response) {
        when(tokenClient.exchange(AUTHORIZATION_CODE)).thenReturn(ACCESS_TOKEN);
        when(userInfoClient.fetch(ACCESS_TOKEN)).thenReturn(response);
    }

    private SocialLoginCommand command() {
        return new SocialLoginCommand(SocialProvider.KAKAO, AUTHORIZATION_CODE);
    }

    private static Stream<KakaoUserInfoResponse> invalidUserInfoResponses() {
        return Stream.of(
                null,
                new KakaoUserInfoResponse(null, new KakaoAccount(new KakaoProfile("총총이", null))),
                userInfo(0L, "총총이", null),
                new KakaoUserInfoResponse(123456789L, null),
                new KakaoUserInfoResponse(123456789L, new KakaoAccount(null)),
                userInfo(123456789L, null, null),
                userInfo(123456789L, " ", null),
                userInfo(123456789L, "가".repeat(256), null),
                userInfo(123456789L, "총총이", " "),
                userInfo(123456789L, "총총이", "a".repeat(2049))
        );
    }

    private static KakaoUserInfoResponse userInfo(
            Long id,
            String nickname,
            String profileImageUrl
    ) {
        return new KakaoUserInfoResponse(
                id,
                new KakaoAccount(new KakaoProfile(nickname, profileImageUrl))
        );
    }

    private void assertAuthenticationFailed(ThrowingCall call) {
        Throwable exception = catchThrowable(call::execute);

        assertThat(exception)
                .isInstanceOf(AuthException.class)
                .extracting(thrown -> ((AuthException) thrown).getErrorCode())
                .isEqualTo(AuthErrorCode.SOCIAL_AUTHENTICATION_FAILED);
        assertThat(exception).hasMessage(AuthErrorCode.SOCIAL_AUTHENTICATION_FAILED.getMessage());
        assertThat(exception.getMessage())
                .doesNotContain(AUTHORIZATION_CODE, ACCESS_TOKEN.value());
    }

    @FunctionalInterface
    private interface ThrowingCall {

        void execute();
    }
}

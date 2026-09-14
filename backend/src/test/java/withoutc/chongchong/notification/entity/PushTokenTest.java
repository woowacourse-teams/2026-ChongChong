package withoutc.chongchong.notification.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import withoutc.chongchong.notification.exception.PushTokenErrorCode;
import withoutc.chongchong.notification.exception.PushTokenException;
import withoutc.chongchong.user.entity.User;

class PushTokenTest {

    private static final User USER = mock(User.class);
    private static final String INSTALLATION_ID = "installation-1";
    private static final String TOKEN_VALUE = "push-token";

    @Test
    @DisplayName("푸시 토큰을 생성하면 활성 상태로 생성된다")
    void createPushToken() {
        PushToken pushToken = PushToken.create(
                USER,
                INSTALLATION_ID,
                TokenProvider.EXPO,
                TOKEN_VALUE,
                DevicePlatform.ANDROID
        );

        assertThat(pushToken.getUser()).isSameAs(USER);
        assertThat(pushToken.getInstallationId()).isEqualTo(INSTALLATION_ID);
        assertThat(pushToken.getProvider()).isEqualTo(TokenProvider.EXPO);
        assertThat(pushToken.getTokenValue()).isEqualTo(TOKEN_VALUE);
        assertThat(pushToken.getPlatform()).isEqualTo(DevicePlatform.ANDROID);
        assertThat(pushToken.isActive()).isTrue();

        pushToken.deactivate();

        assertThat(pushToken.isActive()).isFalse();
    }

    @Test
    @DisplayName("푸시 토큰의 필수 도메인 값이 없으면 생성할 수 없다")
    void rejectMissingRequiredValues() {
        assertInvalidPushToken(null, TokenProvider.EXPO, DevicePlatform.ANDROID);
        assertInvalidPushToken(USER, null, DevicePlatform.ANDROID);
        assertInvalidPushToken(USER, TokenProvider.EXPO, null);
    }

    @Test
    @DisplayName("설치 식별자는 공백이 아니고 255자 이내여야 한다")
    void validateInstallationId() {
        assertInvalidInstallationId(null);
        assertInvalidInstallationId(" ");
        assertInvalidInstallationId("a".repeat(256));

        assertThatCode(() -> PushToken.create(
                USER,
                "a".repeat(255),
                TokenProvider.EXPO,
                TOKEN_VALUE,
                DevicePlatform.ANDROID
        )).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("푸시 토큰 값은 공백이 아니고 255자 이내여야 한다")
    void validateTokenValue() {
        assertInvalidTokenValue(null);
        assertInvalidTokenValue(" ");
        assertInvalidTokenValue("a".repeat(256));

        assertThatCode(() -> PushToken.create(
                USER,
                INSTALLATION_ID,
                TokenProvider.EXPO,
                "a".repeat(255),
                DevicePlatform.ANDROID
        )).doesNotThrowAnyException();
    }

    private void assertInvalidPushToken(User user, TokenProvider provider, DevicePlatform platform) {
        assertThatThrownBy(() -> PushToken.create(
                user,
                INSTALLATION_ID,
                provider,
                TOKEN_VALUE,
                platform
        ))
                .isInstanceOf(PushTokenException.class)
                .extracting(exception -> ((PushTokenException) exception).getErrorCode())
                .isEqualTo(PushTokenErrorCode.INVALID_PUSH_TOKEN);
    }

    private void assertInvalidInstallationId(String installationId) {
        assertThatThrownBy(() -> PushToken.create(
                USER,
                installationId,
                TokenProvider.EXPO,
                TOKEN_VALUE,
                DevicePlatform.ANDROID
        ))
                .isInstanceOf(PushTokenException.class)
                .extracting(exception -> ((PushTokenException) exception).getErrorCode())
                .isEqualTo(PushTokenErrorCode.INVALID_INSTALLATION_ID);
    }

    private void assertInvalidTokenValue(String tokenValue) {
        assertThatThrownBy(() -> PushToken.create(
                USER,
                INSTALLATION_ID,
                TokenProvider.EXPO,
                tokenValue,
                DevicePlatform.ANDROID
        ))
                .isInstanceOf(PushTokenException.class)
                .extracting(exception -> ((PushTokenException) exception).getErrorCode())
                .isEqualTo(PushTokenErrorCode.INVALID_TOKEN_VALUE);
    }
}

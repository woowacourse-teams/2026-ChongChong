package withoutc.chongchong.notification.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import withoutc.chongchong.notification.exception.WebPushErrorCode;
import withoutc.chongchong.notification.exception.WebPushException;
import withoutc.chongchong.user.entity.User;

class WebPushSubscriptionTest {

    private static final User USER = mock(User.class);
    private static final String INSTALLATION_ID = "4c2f0b3f-0a57-4a37-bb15-8ad7f4f3c2a4";
    private static final String ENDPOINT = "https://push.example.com/subscription";
    private static final String P256DH = "p256dh-key";
    private static final String AUTH = "auth-secret";

    @Test
    @DisplayName("웹 푸시 구독을 생성하면 활성 상태로 생성된다")
    void createWebPushSubscription() {
        WebPushSubscription subscription = WebPushSubscription.create(
                USER, INSTALLATION_ID, ENDPOINT, P256DH, AUTH
        );

        assertThat(subscription.getUser()).isSameAs(USER);
        assertThat(subscription.getInstallationId()).isEqualTo(INSTALLATION_ID);
        assertThat(subscription.getEndpoint()).isEqualTo(ENDPOINT);
        assertThat(subscription.getP256dh()).isEqualTo(P256DH);
        assertThat(subscription.getAuth()).isEqualTo(AUTH);
        assertThat(subscription.isActive()).isTrue();

        subscription.deactivate();

        assertThat(subscription.isActive()).isFalse();
    }

    @Test
    @DisplayName("웹 푸시 구독의 필수 값이 없으면 생성할 수 없다")
    void rejectMissingRequiredValues() {
        assertInvalidSubscription(null, INSTALLATION_ID, ENDPOINT, P256DH, AUTH);
        assertInvalidSubscription(USER, null, ENDPOINT, P256DH, AUTH);
        assertInvalidSubscription(USER, INSTALLATION_ID, null, P256DH, AUTH);
        assertInvalidSubscription(USER, INSTALLATION_ID, ENDPOINT, " ", AUTH);
        assertInvalidSubscription(USER, INSTALLATION_ID, ENDPOINT, P256DH, null);
    }

    private void assertInvalidSubscription(
            User user,
            String installationId,
            String endpoint,
            String p256dh,
            String auth
    ) {
        assertThatThrownBy(() -> WebPushSubscription.create(user, installationId, endpoint, p256dh, auth))
                .isInstanceOf(WebPushException.class)
                .extracting(exception -> ((WebPushException) exception).getErrorCode())
                .isEqualTo(WebPushErrorCode.INVALID_WEB_PUSH_SUBSCRIPTION);
    }
}

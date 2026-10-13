package withoutc.chongchong.notification.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import withoutc.chongchong.notification.exception.WebPushException;

class WebPushNotificationClickTokenProviderTest {

    private static final byte[] SECRET_BYTES = new byte[32];
    private static final String SECRET = Base64.getEncoder().encodeToString(SECRET_BYTES);
    private static final Instant ISSUED_AT = Instant.parse("2026-10-13T00:00:00Z");
    private static final long DELIVERY_ID = 42L;

    @Test
    @DisplayName("발급한 클릭 토큰은 해당 발송 내역 ID에 대해 검증된다")
    void verifyTokenForDeliveryId() {
        WebPushNotificationClickTokenProvider provider = providerAt(ISSUED_AT);
        String token = provider.createClickToken(DELIVERY_ID);

        assertThat(Base64.getUrlDecoder().decode(token)).hasSize(40);
        assertThat(token).hasSize(54);
        provider.verifyToken(DELIVERY_ID, token);
    }

    @Test
    @DisplayName("다른 발송 내역 ID에 대해 클릭 토큰을 사용할 수 없다")
    void rejectTokenForDifferentDeliveryId() {
        WebPushNotificationClickTokenProvider provider = providerAt(ISSUED_AT);
        String token = provider.createClickToken(DELIVERY_ID);

        assertThatThrownBy(() -> provider.verifyToken(DELIVERY_ID + 1, token))
                .isInstanceOf(WebPushException.class);
    }

    @Test
    @DisplayName("서명이 변조된 클릭 토큰을 거부한다")
    void rejectTamperedToken() {
        WebPushNotificationClickTokenProvider provider = providerAt(ISSUED_AT);
        String token = provider.createClickToken(DELIVERY_ID);
        char replacement = token.charAt(0) == 'A' ? 'B' : 'A';
        String tamperedToken = replacement + token.substring(1);

        assertThatThrownBy(() -> provider.verifyToken(DELIVERY_ID, tamperedToken))
                .isInstanceOf(WebPushException.class);
    }

    @Test
    @DisplayName("만료된 클릭 토큰을 거부한다")
    void rejectExpiredToken() {
        WebPushNotificationClickTokenProvider issuer = providerAt(ISSUED_AT);
        WebPushNotificationClickTokenProvider verifier = providerAt(ISSUED_AT.plus(Duration.ofDays(8)));
        String token = issuer.createClickToken(DELIVERY_ID);

        assertThatThrownBy(() -> verifier.verifyToken(DELIVERY_ID, token))
                .isInstanceOf(WebPushException.class);
    }

    @Test
    @DisplayName("올바른 Base64URL 형식이 아닌 클릭 토큰을 거부한다")
    void rejectMalformedToken() {
        WebPushNotificationClickTokenProvider provider = providerAt(ISSUED_AT);

        assertThatThrownBy(() -> provider.verifyToken(DELIVERY_ID, "!".repeat(54)))
                .isInstanceOfSatisfying(WebPushException.class, exception ->
                        assertThat(exception.getErrorCode().getCode()).isEqualTo("INVALID_CLICK_TOKEN"));
    }

    private WebPushNotificationClickTokenProvider providerAt(Instant instant) {
        return new WebPushNotificationClickTokenProvider(
                Clock.fixed(instant, ZoneOffset.UTC),
                SECRET
        );
    }
}

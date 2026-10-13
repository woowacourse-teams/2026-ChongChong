package withoutc.chongchong.notification.token;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import withoutc.chongchong.notification.exception.WebPushErrorCode;
import withoutc.chongchong.notification.exception.WebPushException;

@Component
public class WebPushNotificationClickTokenProvider {

    private static final Duration CLICK_TOKEN_TTL = Duration.ofDays(7);
    private static final int SIGNATURE_LENGTH = 32;
    private static final int TOKEN_BYTE_LENGTH = SIGNATURE_LENGTH + Long.BYTES;
    private static final int TOKEN_ENCODED_LENGTH = 54;
    private static final String SIGNATURE_ALGORITHM = "HmacSHA256";
    private static final String SIGN_VALUE_PREFIX = "push-click-v1:";

    private final byte[] clickTokenSecret;
    private final Clock clock;

    public WebPushNotificationClickTokenProvider(
            Clock clock,
            @Value("${web-push.notification.click-token-secret}") String secret
    ) {
        this.clock = clock;
        this.clickTokenSecret = Base64.getDecoder().decode(secret);
    }

    public String createClickToken(long deliveryId) {
        long expiresAt = clock.instant()
                .plus(CLICK_TOKEN_TTL)
                .getEpochSecond();
        byte[] signature = sign(deliveryId, expiresAt);

        byte[] tokenBytes = ByteBuffer.allocate(TOKEN_BYTE_LENGTH)
                .put(signature)
                .putLong(expiresAt)
                .array();

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(tokenBytes);
    }

    public void verifyToken(long deliveryId, String token) {
        if (deliveryId <= 0 || token == null || token.length() != TOKEN_ENCODED_LENGTH) {
            throw invalidClickToken();
        }

        try {
            byte[] tokenBytes = Base64.getUrlDecoder().decode(token);

            validateCanonicalEncoding(tokenBytes, token);

            ByteBuffer tokenBuffer = ByteBuffer.wrap(tokenBytes);
            byte[] actualSignature = new byte[SIGNATURE_LENGTH];
            // tokenBytes에서 서명 길이만큼 읽어 actualSignature에 복사
            tokenBuffer.get(actualSignature);
            long expiresAt = tokenBuffer.getLong();

            validateNotExpired(expiresAt);

            byte[] expectedSignature = sign(deliveryId, expiresAt);
            validateSignature(actualSignature, expectedSignature);
        } catch (IllegalArgumentException exception) {
            throw invalidClickToken();
        }
    }

    private byte[] sign(long deliveryId, long expiresAt) {
        try {
            Mac mac = Mac.getInstance(SIGNATURE_ALGORITHM);
            mac.init(new SecretKeySpec(clickTokenSecret, SIGNATURE_ALGORITHM));
            String message = SIGN_VALUE_PREFIX + deliveryId + ":" + expiresAt;
            return mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException exception) {
            throw new WebPushException(WebPushErrorCode.CLICK_TOKEN_SIGN_FAILED);
        }
    }

    private void validateCanonicalEncoding(byte[] tokenBytes, String token) {
        String reEncodedToken = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(tokenBytes);
        if (!reEncodedToken.equals(token)) {
            throw invalidClickToken();
        }
    }

    private void validateNotExpired(long expiresAt) {
        if (clock.instant().getEpochSecond() >= expiresAt) {
            throw invalidClickToken();
        }
    }

    private void validateSignature(byte[] actualSignature, byte[] expectedSignature) {
        if (!MessageDigest.isEqual(actualSignature, expectedSignature)) {
            throw invalidClickToken();
        }
    }

    private WebPushException invalidClickToken() {
        return new WebPushException(WebPushErrorCode.INVALID_CLICK_TOKEN);
    }
}

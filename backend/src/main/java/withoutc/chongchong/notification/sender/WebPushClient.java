package withoutc.chongchong.notification.sender;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Urgency;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.jose4j.lang.JoseException;
import withoutc.chongchong.notification.config.WebPushVapidProperties;
import withoutc.chongchong.notification.exception.WebPushErrorCode;
import withoutc.chongchong.notification.exception.WebPushException;

public class WebPushClient {

    private static final int MAX_RESPONSE_BODY_LENGTH = 2_000;

    private final PushService pushService;
    private final Duration sendTimeout;

    public WebPushClient(WebPushVapidProperties vapidProperties, Duration sendTimeout) {
        try {
            this.pushService = new PushService(
                    vapidProperties.publicKey(),
                    vapidProperties.privateKey(),
                    vapidProperties.subject()
            );
        } catch (GeneralSecurityException exception) {
            throw new WebPushException(WebPushErrorCode.INVALID_WEB_PUSH_CONFIG);
        }
        validateSendTimeout(sendTimeout);
        this.sendTimeout = sendTimeout;
    }

    WebPushClient(PushService pushService, Duration sendTimeout) {
        validateSendTimeout(sendTimeout);
        this.pushService = pushService;
        this.sendTimeout = sendTimeout;
    }

    public WebPushResponse send(String endpoint, String p256dh, String auth, String payload) {
        Future<HttpResponse> responseFuture = null;
        try {
            Notification notification = new Notification(endpoint, p256dh, auth, payload, Urgency.HIGH);
            responseFuture = pushService.sendAsync(notification);
            HttpResponse response = responseFuture.get(sendTimeout.toMillis(), TimeUnit.MILLISECONDS);
            int status = response.getStatusLine().getStatusCode();
            String responseBody = readResponseBody(response);
            return new WebPushResponse(status, responseBody);
        } catch (JoseException | GeneralSecurityException exception) {
            throw new WebPushException(WebPushErrorCode.WEB_PUSH_CRYPTO_FAILED);
        } catch (IOException | ExecutionException exception) {
            throw new WebPushException(WebPushErrorCode.WEB_PUSH_TRANSPORT_FAILED);
        } catch (TimeoutException exception) {
            cancel(responseFuture);
            throw new WebPushException(WebPushErrorCode.WEB_PUSH_TRANSPORT_FAILED);
        } catch (InterruptedException exception) {
            cancel(responseFuture);
            Thread.currentThread().interrupt();
            throw new WebPushException(WebPushErrorCode.WEB_PUSH_INTERRUPTED);
        }
    }

    private void validateSendTimeout(Duration sendTimeout) {
        if (sendTimeout == null || sendTimeout.isNegative() || sendTimeout.isZero()
                || sendTimeout.toMillis() < 1) {
            throw new IllegalArgumentException("푸시 발송 제한 시간은 1밀리초 이상이어야 합니다.");
        }
    }

    private void cancel(Future<?> future) {
        if (future != null) {
            future.cancel(true);
        }
    }

    private String readResponseBody(HttpResponse response) throws IOException {
        if (response.getEntity() == null) {
            return "";
        }
        String responseBody = EntityUtils.toString(response.getEntity());
        if (responseBody.length() <= MAX_RESPONSE_BODY_LENGTH) {
            return responseBody;
        }
        return responseBody.substring(0, MAX_RESPONSE_BODY_LENGTH);
    }

    public record WebPushResponse(int status, String responseBody) {
    }
}

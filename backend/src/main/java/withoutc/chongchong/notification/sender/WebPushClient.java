package withoutc.chongchong.notification.sender;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.concurrent.ExecutionException;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.jose4j.lang.JoseException;
import org.springframework.stereotype.Component;
import withoutc.chongchong.notification.config.WebPushVapidProperties;
import withoutc.chongchong.notification.exception.WebPushErrorCode;
import withoutc.chongchong.notification.exception.WebPushException;

@Component
public class WebPushClient {

    private static final int MAX_RESPONSE_BODY_LENGTH = 2_000;

    private final PushService pushService;

    public WebPushClient(WebPushVapidProperties vapidProperties) {
        try {
            this.pushService = new PushService(
                    vapidProperties.publicKey(),
                    vapidProperties.privateKey(),
                    vapidProperties.subject()
            );
        } catch (GeneralSecurityException exception) {
            throw new WebPushException(WebPushErrorCode.INVALID_WEB_PUSH_CONFIG);
        }
    }

    public WebPushResponse send(String endpoint, String p256dh, String auth, String payload) {
        try {
            Notification notification = new Notification(endpoint, p256dh, auth, payload);
            HttpResponse response = pushService.send(notification);
            int status = response.getStatusLine().getStatusCode();
            String responseBody = readResponseBody(response);
            return new WebPushResponse(status, responseBody);
        } catch (JoseException | GeneralSecurityException exception) {
            throw new WebPushException(WebPushErrorCode.WEB_PUSH_CRYPTO_FAILED);
        } catch (IOException | ExecutionException exception) {
            throw new WebPushException(WebPushErrorCode.WEB_PUSH_TRANSPORT_FAILED);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new WebPushException(WebPushErrorCode.WEB_PUSH_INTERRUPTED);
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

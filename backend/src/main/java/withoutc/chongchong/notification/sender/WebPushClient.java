package withoutc.chongchong.notification.sender;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.concurrent.ExecutionException;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.apache.http.HttpResponse;
import org.jose4j.lang.JoseException;
import org.springframework.stereotype.Component;
import withoutc.chongchong.notification.config.WebPushVapidProperties;
import withoutc.chongchong.notification.exception.WebPushErrorCode;
import withoutc.chongchong.notification.exception.WebPushException;

@Component
public class WebPushClient {

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

    public int send(String endpoint, String p256dh, String auth, String payload) {
        try {
            Notification notification = new Notification(endpoint, p256dh, auth, payload);
            HttpResponse response = pushService.send(notification);
            return response.getStatusLine().getStatusCode();
        } catch (JoseException | GeneralSecurityException exception) {
            throw new WebPushException(WebPushErrorCode.WEB_PUSH_CRYPTO_FAILED);
        } catch (IOException | ExecutionException exception) {
            throw new WebPushException(WebPushErrorCode.WEB_PUSH_TRANSPORT_FAILED);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new WebPushException(WebPushErrorCode.WEB_PUSH_INTERRUPTED);
        }
    }
}

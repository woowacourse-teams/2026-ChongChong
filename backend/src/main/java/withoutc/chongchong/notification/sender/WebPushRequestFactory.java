package withoutc.chongchong.notification.sender;

import java.io.IOException;
import java.security.GeneralSecurityException;
import nl.martijndwars.webpush.AbstractPushService;
import nl.martijndwars.webpush.Encoding;
import nl.martijndwars.webpush.HttpRequest;
import nl.martijndwars.webpush.Notification;
import org.jose4j.lang.JoseException;

// web-push 라이브러리로 페이로드 암호화와 VAPID 헤더만 만든다.
// 라이브러리의 PushService.sendAsync()는 발송마다 HTTP 클라이언트를 새로 만들기 때문에, 전송은 WebPushClient가 맡는다.
public class WebPushRequestFactory extends AbstractPushService<WebPushRequestFactory> {

    public WebPushRequestFactory(String publicKey, String privateKey, String subject)
            throws GeneralSecurityException {
        super(publicKey, privateKey, subject);
    }

    public HttpRequest create(Notification notification) throws GeneralSecurityException, IOException, JoseException {
        return prepareRequest(notification, Encoding.AES128GCM);
    }
}

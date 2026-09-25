package withoutc.chongchong.notification.sender;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import withoutc.chongchong.notification.exception.WebPushErrorCode;
import withoutc.chongchong.notification.exception.WebPushException;
import withoutc.chongchong.notification.exception.WebPushSendResult;
import withoutc.chongchong.notification.worker.dto.ClaimedDelivery;

@Component
@RequiredArgsConstructor
public class WebPushNotificationSender implements NotificationSender {

    private final ObjectMapper objectMapper;
    private final WebPushClient webPushClient;

    @Override
    public WebPushSendResult send(ClaimedDelivery delivery) {
        String payload = createPayload(delivery);

        int status = webPushClient.send(delivery.endpoint(), delivery.p256dh(), delivery.auth(), payload);
        return result(status);
    }

    private String createPayload(ClaimedDelivery delivery) {
        WebPushPayload payload = new WebPushPayload(
                delivery.notificationId(),
                delivery.title(),
                delivery.body(),
                delivery.deepLink()
        );
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JacksonException exception) {
            throw new WebPushException(WebPushErrorCode.WEB_PUSH_PAYLOAD_SERIALIZATION_FAILED);
        }
    }

    private WebPushSendResult result(int status) {
        if (status >= 200 && status < 300) {
            return WebPushSendResult.SENT;
        }
        if (status == 404 || status == 410) {
            return WebPushSendResult.SUBSCRIPTION_EXPIRED;
        }
        if (status == 429 || status >= 500) {
            return WebPushSendResult.RETRYABLE_FAILURE;
        }
        return WebPushSendResult.PERMANENT_FAILURE;
    }

    private record WebPushPayload(
            Long notificationId,
            String title,
            String body,
            String deepLink
    ) {
    }
}

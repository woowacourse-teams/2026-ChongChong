package withoutc.chongchong.notification.sender;

import lombok.RequiredArgsConstructor;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import withoutc.chongchong.notification.exception.WebPushErrorCode;
import withoutc.chongchong.notification.exception.WebPushException;
import withoutc.chongchong.notification.exception.WebPushSendResult;
import withoutc.chongchong.notification.token.WebPushNotificationClickTokenProvider;
import withoutc.chongchong.notification.worker.dto.ClaimedDelivery;

@RequiredArgsConstructor
public class WebPushNotificationSender implements NotificationSender {

    private final ObjectMapper objectMapper;
    private final WebPushClient webPushClient;
    private final WebPushNotificationClickTokenProvider clickTokenProvider;

    @Override
    public WebPushSendResult send(ClaimedDelivery delivery) {
        String payload = createPayload(delivery);

        WebPushClient.WebPushResponse response = webPushClient.send(
                delivery.endpoint(), delivery.p256dh(), delivery.auth(), payload);
        return result(response.status());
    }

    private String createPayload(ClaimedDelivery delivery) {
        WebPushPayload payload = new WebPushPayload(
                delivery.id(),
                delivery.notificationId(),
                clickTokenProvider.createClickToken(delivery.id()),
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
        if (status == 429) {
            return WebPushSendResult.RATE_LIMITED;
        }
        if (status >= 500) {
            return WebPushSendResult.PROVIDER_UNAVAILABLE;
        }
        return WebPushSendResult.PERMANENT_FAILURE;
    }

    private record WebPushPayload(
            Long deliveryId,
            Long notificationId,
            String clickToken,
            String title,
            String body,
            String deepLink
    ) {
    }
}

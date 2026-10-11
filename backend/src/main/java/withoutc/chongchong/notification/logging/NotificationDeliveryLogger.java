package withoutc.chongchong.notification.logging;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.event.Level;
import org.slf4j.spi.LoggingEventBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import withoutc.chongchong.notification.entity.NotificationDelivery;
import withoutc.chongchong.notification.exception.WebPushErrorCode;
import withoutc.chongchong.notification.exception.WebPushSendResult;
import withoutc.chongchong.notification.worker.dto.ClaimedDelivery;

@Component
@Slf4j
public class NotificationDeliveryLogger {

    private static final String WORKER_COMPONENT = "notification_delivery_worker";
    private static final String RECOVERY_COMPONENT = "notification_delivery_recovery";

    private final String service;
    private final String environment;

    public NotificationDeliveryLogger(
            @Value("${spring.application.name:unknown}") String service,
            @Value("${app.environment:local}") String environment
    ) {
        this.service = service;
        this.environment = environment;
    }

    public void retryResult(ClaimedDelivery delivery, WebPushSendResult result, boolean retryScheduled) {
        logRetry(
                WORKER_COMPONENT,
                delivery.id(),
                delivery.subscriptionId(),
                result.name(),
                result.getErrorCode().getCode(),
                retryScheduled,
                "web push retry scheduled",
                "web push retry exhausted",
                null
        );
    }

    public void retryException(
            ClaimedDelivery delivery,
            WebPushErrorCode errorCode,
            boolean retryScheduled,
            Throwable throwable
    ) {
        logRetry(
                WORKER_COMPONENT,
                delivery.id(),
                delivery.subscriptionId(),
                errorCode.name(),
                errorCode.getCode(),
                retryScheduled,
                "web push retry scheduled",
                "web push retry exhausted",
                throwable
        );
    }

    public void subscriptionExpired(ClaimedDelivery delivery, WebPushSendResult result) {
        logDelivery(
                WORKER_COMPONENT,
                delivery.id(),
                delivery.subscriptionId(),
                Level.INFO,
                "web_push_subscription_expired",
                result.name(),
                result.getErrorCode().getCode(),
                "web push subscription expired",
                null
        );
    }

    public void permanentFailure(ClaimedDelivery delivery, WebPushSendResult result) {
        logDelivery(
                WORKER_COMPONENT,
                delivery.id(),
                delivery.subscriptionId(),
                Level.ERROR,
                "web_push_delivery_failed",
                result.name(),
                result.getErrorCode().getCode(),
                "web push delivery failed",
                null
        );
    }

    public void failure(ClaimedDelivery delivery, WebPushErrorCode errorCode, Throwable throwable) {
        logDelivery(
                WORKER_COMPONENT,
                delivery.id(),
                delivery.subscriptionId(),
                Level.ERROR,
                "web_push_delivery_failed",
                errorCode.name(),
                errorCode.getCode(),
                "web push delivery failed",
                throwable
        );
    }

    public void unexpectedFailure(ClaimedDelivery delivery, Throwable throwable) {
        logDelivery(
                WORKER_COMPONENT,
                delivery.id(),
                delivery.subscriptionId(),
                Level.ERROR,
                "web_push_delivery_failed",
                "UNEXPECTED_ERROR",
                WebPushErrorCode.UNEXPECTED_WEB_PUSH_ERROR.getCode(),
                "unexpected web push delivery error",
                throwable
        );
    }

    public void recoveryResult(NotificationDelivery delivery, boolean retryScheduled) {
        logRetry(
                RECOVERY_COMPONENT,
                delivery.getId(),
                delivery.getWebPushSubscription().getId(),
                "PROCESSING_TIMEOUT",
                null,
                retryScheduled,
                "web push retry scheduled after processing timeout",
                "web push retry exhausted after processing timeout",
                null
        );
    }

    private void logRetry(
            String component,
            Long deliveryId,
            Long subscriptionId,
            String result,
            String errorCode,
            boolean retryScheduled,
            String retryMessage,
            String exhaustedMessage,
            Throwable throwable
    ) {
        Level level;
        String event;
        String message;

        if (retryScheduled) {
            level = Level.WARN;
            event = "web_push_retry_scheduled";
            message = retryMessage;
        } else {
            level = Level.ERROR;
            event = "web_push_retry_exhausted";
            message = exhaustedMessage;
        }

        logDelivery(
                component,
                deliveryId,
                subscriptionId,
                level,
                event,
                result,
                errorCode,
                message,
                throwable
        );
    }

    private void logDelivery(
            String component,
            Long deliveryId,
            Long subscriptionId,
            Level level,
            String event,
            String result,
            String errorCode,
            String message,
            Throwable throwable
    ) {
        LoggingEventBuilder eventBuilder = log.atLevel(level)
                .addKeyValue("service", service)
                .addKeyValue("environment", environment)
                .addKeyValue("component", component)
                .addKeyValue("event", event)
                .addKeyValue("delivery_id", deliveryId)
                .addKeyValue("subscription_id", subscriptionId)
                .addKeyValue("result", result)
                .addKeyValue("error_code", errorCode);

        if (throwable == null) {
            eventBuilder.log(message);
            return;
        }
        eventBuilder.log(message, throwable);
    }
}

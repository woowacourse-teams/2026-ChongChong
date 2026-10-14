package withoutc.chongchong.notification.worker;

import io.micrometer.core.instrument.Timer;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import withoutc.chongchong.notification.metrics.NotificationDeliveryMetrics;
import withoutc.chongchong.notification.exception.WebPushErrorCode;
import withoutc.chongchong.notification.exception.WebPushException;
import withoutc.chongchong.notification.exception.WebPushSendResult;
import withoutc.chongchong.notification.logging.NotificationDeliveryLogger;
import withoutc.chongchong.notification.sender.NotificationSender;
import withoutc.chongchong.notification.worker.dto.ClaimedDelivery;

@Component
@RequiredArgsConstructor
public class NotificationDeliveryWorker {

    // TODO: 적절한 배치 사이즈 결정
    private static final int BATCH_SIZE = 100;

    private final DeliveryClaimService deliveryClaimService;
    private final DeliveryRecoveryService deliveryRecoveryService;
    private final NotificationSender sender;
    private final DeliveryResultService deliveryResultService;
    private final NotificationDeliveryLogger notificationDeliveryLogger;
    private final NotificationDeliveryMetrics notificationDeliveryMetrics;
    private final Clock clock;

    @Scheduled(fixedDelayString = "${web-push.delivery.fixed-delay-ms:1000}")
    public void process() {
        deliveryRecoveryService.recover(BATCH_SIZE);

        List<ClaimedDelivery> deliveries;
        Timer.Sample claimSample = notificationDeliveryMetrics.startClaim();
        try {
            deliveries = deliveryClaimService.claimBatch(BATCH_SIZE);
        } finally {
            notificationDeliveryMetrics.stopClaim(claimSample);
        }

        LocalDateTime claimCompletedAt = LocalDateTime.now(clock);
        for (ClaimedDelivery delivery : deliveries) {
            notificationDeliveryMetrics.recordQueueWait(
                    Duration.between(delivery.readyAt(), claimCompletedAt)
            );
            processOne(delivery, claimCompletedAt);
        }
    }

    public void processOne(ClaimedDelivery delivery) {
        processOne(delivery, null);
    }

    private void processOne(ClaimedDelivery delivery, LocalDateTime claimCompletedAt) {
        try {
            WebPushSendResult result = sendWithMetrics(delivery, claimCompletedAt);

            switch (result) {
                case WebPushSendResult.SENT -> deliveryResultService.markSent(delivery.id());
                case WebPushSendResult.RATE_LIMITED, WebPushSendResult.PROVIDER_UNAVAILABLE ->
                        handleRetryableResult(delivery, result);
                case WebPushSendResult.SUBSCRIPTION_EXPIRED -> {
                    deliveryResultService.markExpired(delivery.id(), delivery.subscriptionId(),
                            result.getErrorCode().getMessage());
                    notificationDeliveryLogger.subscriptionExpired(delivery, result);
                }
                case WebPushSendResult.PERMANENT_FAILURE -> {
                    deliveryResultService.markFailed(delivery.id(), result.getErrorCode().getMessage());
                    notificationDeliveryLogger.permanentFailure(delivery, result);
                }
            }
        } catch (WebPushException exception) {
            WebPushErrorCode errorCode = (WebPushErrorCode) exception.getErrorCode();
            String error = errorCode.getMessage();

            if (errorCode == WebPushErrorCode.WEB_PUSH_TRANSPORT_FAILED) {
                handleRetryableException(delivery, errorCode, exception);
                return;
            }

            if (errorCode == WebPushErrorCode.WEB_PUSH_INTERRUPTED) {
                handleRetryableException(delivery, errorCode, exception);
                throw exception;
            }

            deliveryResultService.markFailed(delivery.id(), error);
            notificationDeliveryLogger.failure(delivery, errorCode, exception);
        } catch (Exception exception) {
            deliveryResultService.markFailed(
                    delivery.id(),
                    WebPushErrorCode.UNEXPECTED_WEB_PUSH_ERROR.getMessage()
            );
            notificationDeliveryLogger.unexpectedFailure(delivery, exception);
        }
    }

    private WebPushSendResult sendWithMetrics(ClaimedDelivery delivery, LocalDateTime claimCompletedAt) {
        if (claimCompletedAt != null) {
            notificationDeliveryMetrics.recordPostClaimWait(
                    Duration.between(claimCompletedAt, LocalDateTime.now(clock))
            );
        }

        Timer.Sample sendSample = notificationDeliveryMetrics.startSend();
        boolean accepted = false;
        try {
            WebPushSendResult result = sender.send(delivery);
            accepted = result == WebPushSendResult.SENT;
            return result;
        } finally {
            notificationDeliveryMetrics.stopSend(sendSample, accepted);
        }
    }

    private void handleRetryableResult(ClaimedDelivery delivery, WebPushSendResult result) {
        boolean retryScheduled = deliveryResultService.markRetry(delivery.id(), result.getErrorCode().getMessage());
        notificationDeliveryLogger.retryResult(delivery, result, retryScheduled);
    }

    private void handleRetryableException(ClaimedDelivery delivery, WebPushErrorCode errorCode,
                                          WebPushException exception) {
        boolean retryScheduled = deliveryResultService.markRetry(delivery.id(), errorCode.getMessage());
        notificationDeliveryLogger.retryException(delivery, errorCode, retryScheduled, exception);
    }
}

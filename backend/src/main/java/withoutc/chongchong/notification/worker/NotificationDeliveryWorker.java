package withoutc.chongchong.notification.worker;

import io.micrometer.core.instrument.Timer;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import withoutc.chongchong.notification.exception.WebPushErrorCode;
import withoutc.chongchong.notification.exception.WebPushException;
import withoutc.chongchong.notification.exception.WebPushSendResult;
import withoutc.chongchong.notification.logging.NotificationDeliveryLogger;
import withoutc.chongchong.notification.metrics.NotificationDeliveryProcessingMetrics;
import withoutc.chongchong.notification.sender.NotificationSender;
import withoutc.chongchong.notification.worker.dto.ClaimedDelivery;

@Component
public class NotificationDeliveryWorker {

    private final DeliveryClaimService deliveryClaimService;
    private final DeliveryRecoveryService deliveryRecoveryService;
    private final DeliveryResultService deliveryResultService;
    private final NotificationSender sender;
    private final ThreadPoolTaskExecutor notificationDeliveryExecutor;
    private final NotificationDeliveryLogger notificationDeliveryLogger;
    private final NotificationDeliveryProcessingMetrics notificationDeliveryProcessingMetrics;
    private final Clock clock;

    public NotificationDeliveryWorker(
            DeliveryClaimService deliveryClaimService,
            DeliveryRecoveryService deliveryRecoveryService,
            DeliveryResultService deliveryResultService,
            NotificationSender sender,
            @Qualifier("notificationDeliveryExecutor") ThreadPoolTaskExecutor notificationDeliveryExecutor,
            NotificationDeliveryLogger notificationDeliveryLogger,
            NotificationDeliveryProcessingMetrics notificationDeliveryProcessingMetrics,
            Clock clock
    ) {
        this.deliveryClaimService = deliveryClaimService;
        this.deliveryRecoveryService = deliveryRecoveryService;
        this.deliveryResultService = deliveryResultService;
        this.sender = sender;
        this.notificationDeliveryExecutor = notificationDeliveryExecutor;
        this.notificationDeliveryLogger = notificationDeliveryLogger;
        this.notificationDeliveryProcessingMetrics = notificationDeliveryProcessingMetrics;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${web-push.delivery.fixed-delay-ms:1000}")
    public void process() {
        int availableSlots = Math.max(
                0,
                notificationDeliveryExecutor.getMaxPoolSize() - notificationDeliveryExecutor.getActiveCount());

        if (availableSlots == 0) {
            return;
        }

        deliveryRecoveryService.recover(availableSlots);

        List<ClaimedDelivery> deliveries;
        Timer.Sample claimSample = notificationDeliveryProcessingMetrics.startClaim();
        try {
            deliveries = deliveryClaimService.claimBatch(availableSlots);
        } finally {
            notificationDeliveryProcessingMetrics.stopClaim(claimSample);
        }

        LocalDateTime claimCompletedAt = LocalDateTime.now(clock);
        for (int index = 0; index < deliveries.size(); index++) {
            ClaimedDelivery delivery = deliveries.get(index);
            try {
                notificationDeliveryExecutor.execute(
                        () -> processOne(delivery, claimCompletedAt));
                notificationDeliveryProcessingMetrics.recordQueueWait(
                        Duration.between(delivery.readyAt(), claimCompletedAt)
                );
            } catch (TaskRejectedException exception) {
                releaseUnsubmittedDeliveries(deliveries, index, exception);
                return;
            }
        }
    }

    private void releaseUnsubmittedDeliveries(
            List<ClaimedDelivery> deliveries,
            int firstUnsubmittedIndex,
            TaskRejectedException rejection
    ) {
        for (int index = firstUnsubmittedIndex; index < deliveries.size(); index++) {
            ClaimedDelivery delivery = deliveries.get(index);
            try {
                boolean isReleased = deliveryResultService.releaseClaim(delivery.id());
                notificationDeliveryLogger.submissionRejected(delivery, isReleased, rejection);
            } catch (RuntimeException exception) {
                notificationDeliveryLogger.submissionRejected(delivery, false, exception);
            }
        }
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
            notificationDeliveryProcessingMetrics.recordPostClaimWait(
                    Duration.between(claimCompletedAt, LocalDateTime.now(clock))
            );
        }

        Timer.Sample sendSample = notificationDeliveryProcessingMetrics.startSend();
        boolean accepted = false;
        try {
            WebPushSendResult result = sender.send(delivery);
            accepted = result == WebPushSendResult.SENT;
            return result;
        } finally {
            notificationDeliveryProcessingMetrics.stopSend(sendSample, accepted);
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

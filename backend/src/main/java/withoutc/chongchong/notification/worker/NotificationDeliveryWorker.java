package withoutc.chongchong.notification.worker;

import io.micrometer.core.instrument.Timer;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.core.Ordered;
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
public class NotificationDeliveryWorker implements ApplicationListener<ContextClosedEvent>, Ordered {

    // 작업이 밀려 있을 때 빈 슬롯을 확인하는 간격
    private static final Duration BURST_POLL_INTERVAL = Duration.ofMillis(50);
    // 작업이 밀려 있을 때 process() 한 번이 이어서 가져오기를 계속하는 최대 시간
    // recover()는 process() 시작 시에만 돌기 때문에 중간에 끊어야 다시 실행
    private static final Duration MAX_BURST_DURATION = Duration.ofSeconds(10);

    private final DeliveryClaimService deliveryClaimService;
    private final DeliveryRecoveryService deliveryRecoveryService;
    private final DeliveryResultService deliveryResultService;
    private final NotificationSender sender;
    private final ThreadPoolTaskExecutor notificationDeliveryExecutor;
    private final NotificationDeliveryLogger notificationDeliveryLogger;
    private final NotificationDeliveryProcessingMetrics notificationDeliveryProcessingMetrics;
    private final Clock clock;
    private volatile boolean stopping;

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

    // 발송 전용 스케줄러가 1초 간격으로 호출. 빈 슬롯만큼 claim해서 발송 스레드에 넘김
    @Scheduled(fixedDelayString = "${web-push.delivery.fixed-delay-ms:1000}", scheduler = "notificationDeliveryScheduler")
    public void process() {
        if (stopping) {
            return;
        }
        int availableSlots = availableSlots();

        if (availableSlots == 0) {
            return;
        }

        deliveryRecoveryService.recover(availableSlots);

        // 요청한 수만큼 꽉 채워 가져오면 큐에 더 있다고 보고, 50ms마다 빈 슬롯만큼 이어서 가져옴
        boolean backlog = dispatch(availableSlots);
        long deadline = System.nanoTime() + MAX_BURST_DURATION.toNanos();
        while (backlog && !stopping && System.nanoTime() < deadline) {
            if (!pause(BURST_POLL_INTERVAL)) {
                return;
            }
            int freeSlots = availableSlots();
            if (freeSlots > 0) {
                backlog = dispatch(freeSlots);
            }
        }
    }

    // 종료가 시작되면 새 작업을 가져오지 않음
    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        stopping = true;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private int availableSlots() {
        return Math.max(
                0,
                notificationDeliveryExecutor.getMaxPoolSize() - notificationDeliveryExecutor.getActiveCount());
    }

    private boolean dispatch(int slots) {
        List<ClaimedDelivery> deliveries;
        Timer.Sample claimSample = notificationDeliveryProcessingMetrics.startClaim();
        try {
            deliveries = deliveryClaimService.claimBatch(slots);
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
                return false;
            }
        }
        return deliveries.size() == slots;
    }

    private boolean pause(Duration duration) {
        try {
            Thread.sleep(duration);
            return true;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return false;
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

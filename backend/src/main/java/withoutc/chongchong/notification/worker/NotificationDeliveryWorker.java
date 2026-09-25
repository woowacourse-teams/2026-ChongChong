package withoutc.chongchong.notification.worker;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import withoutc.chongchong.notification.exception.WebPushErrorCode;
import withoutc.chongchong.notification.exception.WebPushException;
import withoutc.chongchong.notification.exception.WebPushSendResult;
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

    @Scheduled(fixedDelayString = "${web-push.delivery.fixed-delay-ms:1000}")
    public void process() {
        deliveryRecoveryService.recover(BATCH_SIZE);

        List<ClaimedDelivery> deliveries = deliveryClaimService.claimBatch(BATCH_SIZE);

        for (ClaimedDelivery delivery : deliveries) {
            processOne(delivery);
        }
    }

    public void processOne(ClaimedDelivery delivery) {
        try {
            WebPushSendResult result = sender.send(delivery);

            switch (result) {
                case WebPushSendResult.SENT -> deliveryResultService.markSent(delivery.id());
                case WebPushSendResult.RETRYABLE_FAILURE ->
                        deliveryResultService.markRetry(delivery.id(), result.getMessage());
                case WebPushSendResult.SUBSCRIPTION_EXPIRED ->
                        deliveryResultService.markExpired(delivery.id(), delivery.subscriptionId(),
                                result.getMessage());
                case WebPushSendResult.PERMANENT_FAILURE ->
                        deliveryResultService.markFailed(delivery.id(), result.getMessage());
            }
        } catch (WebPushException exception) {
            WebPushErrorCode errorCode = (WebPushErrorCode) exception.getErrorCode();
            String error = errorCode.getMessage();

            if (errorCode == WebPushErrorCode.WEB_PUSH_TRANSPORT_FAILED) {
                deliveryResultService.markRetry(delivery.id(), error);
                return;
            }

            if (errorCode == WebPushErrorCode.WEB_PUSH_INTERRUPTED) {
                deliveryResultService.markRetry(delivery.id(), error);
                throw exception;
            }

            deliveryResultService.markFailed(delivery.id(), error);
        }
    }
}

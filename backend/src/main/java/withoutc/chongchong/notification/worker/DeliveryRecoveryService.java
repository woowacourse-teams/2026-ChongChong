package withoutc.chongchong.notification.worker;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import withoutc.chongchong.notification.entity.NotificationDelivery;
import withoutc.chongchong.notification.repository.NotificationDeliveryRepository;

@Service
public class DeliveryRecoveryService {

    private static final String PROCESSING_TIMEOUT_ERROR =
            "PROCESSING 상태가 오래 유지되어 재처리합니다.";

    private final NotificationDeliveryRepository notificationDeliveryRepository;
    private final Clock clock;
    private final long processingTimeoutMs;

    public DeliveryRecoveryService(
            NotificationDeliveryRepository notificationDeliveryRepository,
            Clock clock,
            @Value("${web-push.delivery.processing-timeout-ms:300000}") long processingTimeoutMs
    ) {
        this.notificationDeliveryRepository = notificationDeliveryRepository;
        this.clock = clock;
        this.processingTimeoutMs = processingTimeoutMs;
    }

    // PROCESSING 상태가 5분 이상 지속되면 다시 처리
    @Transactional
    public void recover(int batchSize) {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime expiryTime = now.minus(Duration.ofMillis(processingTimeoutMs));

        List<NotificationDelivery> deliveries =
                notificationDeliveryRepository.findStuckProcessingForUpdate(expiryTime, batchSize);

        for (NotificationDelivery delivery : deliveries) {
            recover(delivery, now);
        }
    }

    private void recover(NotificationDelivery delivery, LocalDateTime now) {
        if (delivery.getAttemptCount() >= DeliveryResultService.MAX_ATTEMPTS) {
            delivery.markFailed(PROCESSING_TIMEOUT_ERROR);
            return;
        }
        delivery.markRetry(now, PROCESSING_TIMEOUT_ERROR);
    }
}

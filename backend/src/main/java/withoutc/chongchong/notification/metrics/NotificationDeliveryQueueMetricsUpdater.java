package withoutc.chongchong.notification.metrics;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import withoutc.chongchong.notification.entity.DeliveryStatus;
import withoutc.chongchong.notification.repository.NotificationDeliveryRepository;
import withoutc.chongchong.notification.repository.projection.ClaimableQueueSnapshotProjection;

@Component
@RequiredArgsConstructor
public class NotificationDeliveryQueueMetricsUpdater {

    private final NotificationDeliveryRepository notificationDeliveryRepository;
    private final NotificationDeliveryMetrics notificationDeliveryMetrics;
    private final Clock clock;

    @Scheduled(fixedDelayString = "${web-push.delivery.metrics.fixed-delay-ms:10000}")
    public void refresh() {
        LocalDateTime now = LocalDateTime.now(clock);
        ClaimableQueueSnapshotProjection snapshot = notificationDeliveryRepository.findClaimableQueueSnapshot(
                now, DeliveryStatus.PENDING, DeliveryStatus.RETRY_WAIT);
        if (snapshot.readyCount() == 0 || snapshot.oldestReadyAt() == null) {
            notificationDeliveryMetrics.clearQueueSnapshot();
            return;
        }

        Duration oldestReadyAge = Duration.between(snapshot.oldestReadyAt(), now);
        notificationDeliveryMetrics.updateQueueSnapshot(snapshot.readyCount(), oldestReadyAge);
    }
}

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
    private final NotificationDeliveryQueueMetrics notificationDeliveryQueueMetrics;
    private final Clock clock;

    @Scheduled(
            scheduler = "notificationMetricsTaskScheduler",
            fixedDelayString = "${web-push.delivery.metrics.fixed-delay-ms:10000}"
    )
    public void refresh() {
        LocalDateTime now = LocalDateTime.now(clock);
        ClaimableQueueSnapshotProjection snapshot = notificationDeliveryRepository.findClaimableQueueSnapshot(
                now, DeliveryStatus.PENDING, DeliveryStatus.RETRY_WAIT);
        if (snapshot.readyCount() == 0) {
            notificationDeliveryQueueMetrics.clearQueueSnapshot(clock.millis());
            return;
        }

        Duration oldestReadyAge = Duration.between(snapshot.oldestReadyAt(), now);
        notificationDeliveryQueueMetrics.updateQueueSnapshot(
                snapshot.readyCount(), oldestReadyAge, clock.millis());
    }
}

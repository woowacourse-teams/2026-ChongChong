package withoutc.chongchong.notification.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.TimeGauge;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Component;

@Component
public class NotificationDeliveryQueueMetrics {

    private static final String QUEUE_READY_NAME = "notification.delivery.queue.ready";
    private static final String QUEUE_OLDEST_READY_AGE_NAME = "notification.delivery.queue.oldest.ready.age";
    private static final String QUEUE_SNAPSHOT_LAST_SUCCESS_NAME =
            "notification.delivery.queue.snapshot.last.success.timestamp";

    private final AtomicReference<QueueSnapshot> queueSnapshot =
            new AtomicReference<>(new QueueSnapshot(0, 0, 0));

    public NotificationDeliveryQueueMetrics(MeterRegistry meterRegistry) {
        Gauge.builder(QUEUE_READY_NAME, queueSnapshot, snapshot -> snapshot.get().readyCount())
                .description("현재 claim 가능한 알림 전송 작업 수")
                .register(meterRegistry);

        TimeGauge.builder(QUEUE_OLDEST_READY_AGE_NAME, queueSnapshot, TimeUnit.NANOSECONDS,
                        snapshot -> snapshot.get().oldestReadyAgeNanos())
                .description("현재 claim 가능한 작업 중 가장 오래 대기한 작업의 경과 시간")
                .register(meterRegistry);

        Gauge.builder(QUEUE_SNAPSHOT_LAST_SUCCESS_NAME, queueSnapshot,
                        snapshot -> snapshot.get().lastSuccessTimestampMillis() / 1000.0)
                .description("알림 큐 스냅샷을 마지막으로 성공 조회한 시각의 Unix timestamp")
                .baseUnit("seconds")
                .register(meterRegistry);
    }

    public void updateQueueSnapshot(
            long readyCount,
            Duration oldestReadyAge,
            long snapshotTimestampMillis
    ) {
        long oldestReadyAgeNanos = Math.max(0, oldestReadyAge.toNanos());
        queueSnapshot.set(new QueueSnapshot(readyCount, oldestReadyAgeNanos, snapshotTimestampMillis));
    }

    public void clearQueueSnapshot(long snapshotTimestampMillis) {
        queueSnapshot.set(new QueueSnapshot(0, 0, snapshotTimestampMillis));
    }

    private record QueueSnapshot(
            long readyCount,
            long oldestReadyAgeNanos,
            long lastSuccessTimestampMillis
    ) {
    }
}

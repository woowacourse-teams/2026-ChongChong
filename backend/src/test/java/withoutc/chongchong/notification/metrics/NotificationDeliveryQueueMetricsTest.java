package withoutc.chongchong.notification.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NotificationDeliveryQueueMetricsTest {

    @Test
    @DisplayName("큐의 작업 수와 가장 오래된 대기 시간을 메트릭에 반영한다")
    void updateQueueMetrics() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        NotificationDeliveryQueueMetrics metrics = new NotificationDeliveryQueueMetrics(meterRegistry);

        metrics.updateQueueSnapshot(3, Duration.ofSeconds(5), 1_792_526_400_000L);

        assertThat(meterRegistry.get("notification.delivery.queue.ready").gauge().value()).isEqualTo(3);
        assertThat(meterRegistry.get("notification.delivery.queue.oldest.ready.age")
                .timeGauge().value(TimeUnit.SECONDS)).isEqualTo(5);
        assertThat(meterRegistry.get("notification.delivery.queue.snapshot.last.success.timestamp").gauge().value())
                .isEqualTo(1_792_526_400.0);

        metrics.updateQueueSnapshot(3, Duration.ofSeconds(7), 1_792_526_410_000L);
        assertThat(meterRegistry.get("notification.delivery.queue.oldest.ready.age")
                .timeGauge().value(TimeUnit.SECONDS)).isEqualTo(7);
    }
}

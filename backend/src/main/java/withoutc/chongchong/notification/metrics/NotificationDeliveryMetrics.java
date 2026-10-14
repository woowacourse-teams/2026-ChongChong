package withoutc.chongchong.notification.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.TimeGauge;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

@Component
public class NotificationDeliveryMetrics {

    private static final String QUEUE_READY_NAME = "notification.delivery.queue.ready";
    private static final String QUEUE_OLDEST_READY_AGE_NAME = "notification.delivery.queue.oldest.ready.age";
    private static final String QUEUE_WAIT_NAME = "notification.delivery.queue.wait";
    private static final String POST_CLAIM_WAIT_NAME = "notification.delivery.post.claim.wait";
    private static final String CLAIM_NAME = "notification.delivery.claim";
    private static final String SEND_NAME = "notification.delivery.send";
    private static final String OUTCOME_TAG = "outcome";
    private static final String SEND_SUCCESS_OUTCOME = "success";
    private static final String SEND_FAILURE_OUTCOME = "failure";

    private final MeterRegistry meterRegistry;
    private final Timer queueWaitTimer;
    private final Timer postClaimWaitTimer;
    private final Timer claimTimer;
    private final AtomicLong readyQueueSize = new AtomicLong();
    private final AtomicLong oldestReadyAgeNanos = new AtomicLong();

    public NotificationDeliveryMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.queueWaitTimer = Timer.builder(QUEUE_WAIT_NAME).register(meterRegistry);
        this.postClaimWaitTimer = Timer.builder(POST_CLAIM_WAIT_NAME).register(meterRegistry);
        this.claimTimer = Timer.builder(CLAIM_NAME).register(meterRegistry);

        Gauge.builder(QUEUE_READY_NAME, readyQueueSize, AtomicLong::doubleValue)
                .description("현재 claim 가능한 알림 전송 작업 수")
                .register(meterRegistry);
        TimeGauge.builder(QUEUE_OLDEST_READY_AGE_NAME, oldestReadyAgeNanos, TimeUnit.NANOSECONDS,
                        AtomicLong::doubleValue)
                .description("현재 claim 가능한 작업 중 가장 오래 대기한 작업의 경과 시간")
                .register(meterRegistry);
    }

    public Timer.Sample startClaim() {
        return Timer.start(meterRegistry);
    }

    public void stopClaim(Timer.Sample sample) {
        sample.stop(claimTimer);
    }

    public void recordQueueWait(Duration duration) {
        recordIfNonNegative(queueWaitTimer, duration);
    }

    public void recordPostClaimWait(Duration duration) {
        recordIfNonNegative(postClaimWaitTimer, duration);
    }

    public Timer.Sample startSend() {
        return Timer.start(meterRegistry);
    }

    public void stopSend(Timer.Sample sample, boolean accepted) {
        Timer timer = Timer.builder(SEND_NAME)
                .tag(OUTCOME_TAG, sendOutcome(accepted))
                .register(meterRegistry);
        sample.stop(timer);
    }

    private String sendOutcome(boolean accepted) {
        if (accepted) {
            return SEND_SUCCESS_OUTCOME;
        }
        return SEND_FAILURE_OUTCOME;
    }

    public void updateQueueSnapshot(long readyCount, Duration oldestReadyAge) {
        readyQueueSize.set(readyCount);
        oldestReadyAgeNanos.set(Math.max(0, oldestReadyAge.toNanos()));
    }

    public void clearQueueSnapshot() {
        readyQueueSize.set(0);
        oldestReadyAgeNanos.set(0);
    }

    private void recordIfNonNegative(Timer timer, Duration duration) {
        if (!duration.isNegative()) {
            timer.record(duration);
        }
    }
}

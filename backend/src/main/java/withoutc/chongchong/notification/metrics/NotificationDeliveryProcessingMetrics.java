package withoutc.chongchong.notification.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import org.springframework.stereotype.Component;

@Component
public class NotificationDeliveryProcessingMetrics {

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

    public NotificationDeliveryProcessingMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.queueWaitTimer = Timer.builder(QUEUE_WAIT_NAME).register(meterRegistry);
        this.postClaimWaitTimer = Timer.builder(POST_CLAIM_WAIT_NAME).register(meterRegistry);
        this.claimTimer = Timer.builder(CLAIM_NAME).register(meterRegistry);
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

    private void recordIfNonNegative(Timer timer, Duration duration) {
        if (!duration.isNegative()) {
            timer.record(duration);
        }
    }
}

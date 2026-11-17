package withoutc.chongchong.file.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

@Component
public class FileCleanupMetrics {

    private static final String MARKED_NAME = "file.cleanup.marked";
    private static final String DELETED_NAME = "file.cleanup.deleted";
    private static final String DELETE_FAILED_NAME = "file.cleanup.delete.failed";
    private static final String DELETE_PENDING_NAME = "file.cleanup.delete.pending";
    private static final String EXHAUSTED_NAME = "file.cleanup.delete.exhausted";
    private static final String DRY_RUN_CANDIDATES_NAME = "file.cleanup.dry.run.candidates";
    private static final String LAST_SUCCESS_NAME = "file.cleanup.last.success.timestamp";

    private final MeterRegistry meterRegistry;
    private final Counter markedPendingExpired;
    private final Counter markedUnreferenced;
    private final Counter deleted;
    private final AtomicLong deletePending = new AtomicLong();
    private final AtomicLong exhausted = new AtomicLong();
    private final AtomicLong dryRunCandidates = new AtomicLong();
    private final AtomicLong lastSuccessTimestampMillis = new AtomicLong();

    public FileCleanupMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.markedPendingExpired = markedCounter("pending_expired");
        this.markedUnreferenced = markedCounter("unreferenced");
        this.deleted = Counter.builder(DELETED_NAME)
                .description("S3 객체와 메타데이터 행을 모두 삭제한 파일 수")
                .register(meterRegistry);
        Gauge.builder(DELETE_PENDING_NAME, deletePending, AtomicLong::get)
                .description("삭제를 재시도할 수 있는 DELETE_PENDING 파일 수")
                .register(meterRegistry);
        Gauge.builder(EXHAUSTED_NAME, exhausted, AtomicLong::get)
                .description("최대 시도 횟수를 넘어 더 이상 삭제를 시도하지 않는 파일 수")
                .register(meterRegistry);
        Gauge.builder(DRY_RUN_CANDIDATES_NAME, dryRunCandidates, AtomicLong::get)
                .description("드라이런 마지막 실행에서 찾은 정리 후보 수")
                .register(meterRegistry);
        Gauge.builder(LAST_SUCCESS_NAME, lastSuccessTimestampMillis, value -> value.get() / 1000.0)
                .description("파일 정리 작업이 마지막으로 성공한 시각의 Unix timestamp")
                .baseUnit("seconds")
                .register(meterRegistry);
    }

    public void recordMarked(int pendingExpired, int unreferenced) {
        markedPendingExpired.increment(pendingExpired);
        markedUnreferenced.increment(unreferenced);
    }

    public void recordDeleted(int count) {
        deleted.increment(count);
    }

    public void recordDeleteFailed(String error) {
        Counter.builder(DELETE_FAILED_NAME)
                .description("파일 삭제에 실패한 횟수")
                .tag("error", error)
                .register(meterRegistry)
                .increment();
    }

    public void updateBacklog(long retryable, long exhaustedCount) {
        deletePending.set(retryable);
        exhausted.set(exhaustedCount);
    }

    public void updateDryRunCandidates(long count) {
        dryRunCandidates.set(count);
    }

    public void recordSuccess(long timestampMillis) {
        lastSuccessTimestampMillis.set(timestampMillis);
    }

    private Counter markedCounter(String reason) {
        return Counter.builder(MARKED_NAME)
                .description("삭제 대기로 표시한 고아 파일 수")
                .tag("reason", reason)
                .register(meterRegistry);
    }
}

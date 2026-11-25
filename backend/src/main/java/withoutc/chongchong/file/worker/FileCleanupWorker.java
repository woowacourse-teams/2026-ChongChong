package withoutc.chongchong.file.worker;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import withoutc.chongchong.file.config.FileCleanupProperties;
import withoutc.chongchong.file.logging.FileCleanupLogger;
import withoutc.chongchong.file.metrics.FileCleanupMetrics;
import withoutc.chongchong.file.repository.FileCleanupRepository.DeletionBacklog;
import withoutc.chongchong.file.service.FileCleanupTransactionService;
import withoutc.chongchong.file.service.FileCleanupTransactionService.DeletionTarget;
import withoutc.chongchong.file.service.FileCleanupTransactionService.MarkResult;
import withoutc.chongchong.file.service.FileCleanupTransactionService.OrphanCutoffs;
import withoutc.chongchong.file.service.S3FileDeleter;
import withoutc.chongchong.file.service.S3FileDeleter.DeleteResult;

@Component
@RequiredArgsConstructor
public class FileCleanupWorker implements ApplicationListener<ContextClosedEvent> {

    private final FileCleanupProperties properties;
    private final FileCleanupTransactionService transactionService;
    private final S3FileDeleter s3FileDeleter;
    private final FileCleanupMetrics metrics;
    private final FileCleanupLogger logger;
    private final Clock clock;
    private volatile boolean stopping;

    @Scheduled(
            fixedDelayString = "${file.cleanup.fixed-delay:10m}",
            initialDelayString = "${file.cleanup.initial-delay:1m}",
            scheduler = "fileCleanupScheduler"
    )
    public void scheduledRun() {
        if (!properties.enabled()) {
            return;
        }
        try {
            run();
        } catch (RuntimeException exception) {
            logger.runFailed(exception);
        }
    }

    public FileCleanupRunResult run() {
        long startedAt = clock.millis();
        LocalDateTime now = LocalDateTime.now(clock);
        OrphanCutoffs cutoffs = new OrphanCutoffs(
                now.minus(properties.pendingGrace()),
                now.minus(properties.unreferencedGrace())
        );

        RunCounter counter = new RunCounter();
        mark(cutoffs, counter);
        if (properties.dryRun()) {
            metrics.updateDryRunCandidates(counter.candidates);
        } else {
            delete(now, counter);
            DeletionBacklog backlog = transactionService.findBacklog(properties.maxAttempts());
            metrics.updateBacklog(backlog.retryable(), backlog.exhausted());
        }

        FileCleanupRunResult result = counter.toResult(properties.dryRun(), clock.millis() - startedAt);
        metrics.recordSuccess(clock.millis());
        logger.runCompleted(result);
        return result;
    }

    // 종료가 시작되면 지금 배치만 마치고 새 배치를 집지 않는다. 남은 파일은 다음 실행이 이어서 정리한다.
    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        stopping = true;
    }

    private void mark(OrphanCutoffs cutoffs, RunCounter counter) {
        long afterId = 0;
        for (int batch = 0; batch < properties.maxBatchesPerRun() && !stopping; batch++) {
            List<Long> candidateIds = transactionService.findCandidateIds(afterId, cutoffs, properties.batchSize());
            if (candidateIds.isEmpty()) {
                return;
            }
            afterId = candidateIds.getLast();
            counter.addCandidates(candidateIds.size());
            if (!properties.dryRun()) {
                markBatch(candidateIds, cutoffs, counter);
            }
        }
    }

    private void markBatch(List<Long> candidateIds, OrphanCutoffs cutoffs, RunCounter counter) {
        MarkResult result = transactionService.markBatch(candidateIds, cutoffs);
        counter.addMarked(result);
        metrics.recordMarked(result.pendingExpired(), result.unreferenced());
    }

    private void delete(LocalDateTime now, RunCounter counter) {
        LocalDateTime leaseCutoff = now.minus(properties.deleteLease());
        for (int batch = 0; batch < properties.maxBatchesPerRun() && !stopping; batch++) {
            List<DeletionTarget> targets = transactionService.claim(
                    now, leaseCutoff, properties.maxAttempts(), properties.batchSize());
            if (targets.isEmpty()) {
                return;
            }
            deleteBatch(targets, counter);
        }
    }

    private void deleteBatch(List<DeletionTarget> targets, RunCounter counter) {
        DeleteResult result = s3FileDeleter.delete(targets.stream().map(DeletionTarget::objectKey).toList());
        Map<Boolean, List<DeletionTarget>> byObjectDeleted = targets.stream()
                .collect(Collectors.partitioningBy(target -> result.isDeleted(target.objectKey())));

        byObjectDeleted.get(false).forEach(target -> recordFailure(target, result.errorOf(target.objectKey()), counter));
        deleteRows(byObjectDeleted.get(true), counter);
    }

    private void deleteRows(List<DeletionTarget> targets, RunCounter counter) {
        if (targets.isEmpty()) {
            return;
        }
        try {
            recordDeleted(transactionService.deleteRows(targets.stream().map(DeletionTarget::id).toList()), counter);
        } catch (DataIntegrityViolationException exception) {
            targets.forEach(target -> deleteRowIsolated(target, counter));
        }
    }

    private void deleteRowIsolated(DeletionTarget target, RunCounter counter) {
        try {
            recordDeleted(transactionService.deleteRows(List.of(target.id())), counter);
        } catch (DataIntegrityViolationException exception) {
            logger.referenceViolation(target.id(), target.objectKey(), exception);
            counter.addDeleteFailed();
            metrics.recordDeleteFailed("ReferenceViolation");
        }
    }

    private void recordDeleted(int deleted, RunCounter counter) {
        counter.addDeleted(deleted);
        metrics.recordDeleted(deleted);
    }

    private void recordFailure(DeletionTarget target, String error, RunCounter counter) {
        counter.addDeleteFailed();
        metrics.recordDeleteFailed(error);
        logger.deleteFailed(target.id(), target.objectKey(), target.attempts(), error);
    }

    private static final class RunCounter {
        private int candidates;
        private int markedPendingExpired;
        private int markedUnreferenced;
        private int markSkipped;
        private int deleted;
        private int deleteFailed;

        private void addCandidates(int count) {
            candidates += count;
        }

        private void addMarked(MarkResult result) {
            markedPendingExpired += result.pendingExpired();
            markedUnreferenced += result.unreferenced();
            markSkipped += result.skipped();
        }

        private void addDeleted(int count) {
            deleted += count;
        }

        private void addDeleteFailed() {
            deleteFailed++;
        }

        private FileCleanupRunResult toResult(boolean dryRun, long durationMillis) {
            return new FileCleanupRunResult(dryRun, candidates, markedPendingExpired, markedUnreferenced,
                    markSkipped, deleted, deleteFailed, durationMillis);
        }
    }
}

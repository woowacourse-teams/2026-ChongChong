package withoutc.chongchong.file.worker;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import withoutc.chongchong.file.config.FileCleanupProperties;
import withoutc.chongchong.file.logging.FileCleanupLogger;
import withoutc.chongchong.file.metrics.FileCleanupMetrics;
import withoutc.chongchong.file.repository.FileCleanupRepository.DeletionBacklog;
import withoutc.chongchong.file.service.cleanup.FileCleanupDeleteService;
import withoutc.chongchong.file.service.cleanup.FileCleanupDeleteService.DeletionTarget;
import withoutc.chongchong.file.service.cleanup.FileCleanupMarkService;
import withoutc.chongchong.file.service.cleanup.FileCleanupMarkService.MarkResult;
import withoutc.chongchong.file.service.cleanup.FileCleanupMarkService.OrphanCutoffs;
import withoutc.chongchong.file.service.cleanup.S3FileDeleter;
import withoutc.chongchong.file.service.cleanup.S3FileDeleter.DeleteResult;

@Component
@RequiredArgsConstructor
public class FileCleanupWorker {

    private final FileCleanupProperties properties;
    private final FileCleanupMarkService markService;
    private final FileCleanupDeleteService deleteService;
    private final S3FileDeleter s3FileDeleter;
    private final FileCleanupMetrics metrics;
    private final FileCleanupLogger logger;
    private final Clock clock;

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
            DeletionBacklog backlog = deleteService.findBacklog(properties.maxAttempts());
            metrics.updateBacklog(backlog.retryable(), backlog.exhausted());
        }

        FileCleanupRunResult result = counter.toResult(properties.dryRun(), clock.millis() - startedAt);
        metrics.recordSuccess(clock.millis());
        logger.runCompleted(result);
        return result;
    }

    private void mark(OrphanCutoffs cutoffs, RunCounter counter) {
        long afterId = 0;
        for (int batch = 0; batch < properties.maxBatchesPerRun(); batch++) {
            List<Long> candidateIds = markService.findCandidateIds(afterId, cutoffs, properties.batchSize());
            if (candidateIds.isEmpty()) {
                return;
            }
            afterId = candidateIds.getLast();
            counter.candidates += candidateIds.size();
            if (properties.dryRun()) {
                continue;
            }
            MarkResult result = markService.markBatch(candidateIds, cutoffs);
            counter.markedPendingExpired += result.pendingExpired();
            counter.markedUnreferenced += result.unreferenced();
            counter.markSkipped += result.skipped();
            metrics.recordMarked(result.pendingExpired(), result.unreferenced());
        }
    }

    private void delete(LocalDateTime now, RunCounter counter) {
        LocalDateTime leaseCutoff = now.minus(properties.deleteLease());
        for (int batch = 0; batch < properties.maxBatchesPerRun(); batch++) {
            List<DeletionTarget> targets = deleteService.claim(
                    now, leaseCutoff, properties.maxAttempts(), properties.batchSize());
            if (targets.isEmpty()) {
                return;
            }
            deleteBatch(targets, counter);
        }
    }

    private void deleteBatch(List<DeletionTarget> targets, RunCounter counter) {
        DeleteResult result = s3FileDeleter.delete(targets.stream().map(DeletionTarget::objectKey).toList());

        List<DeletionTarget> objectDeleted = new ArrayList<>();
        for (DeletionTarget target : targets) {
            if (result.deletedKeys().contains(target.objectKey())) {
                objectDeleted.add(target);
                continue;
            }
            String error = result.failures().getOrDefault(target.objectKey(), "Unknown");
            recordFailure(target, error, counter);
        }
        deleteRows(objectDeleted, counter);
    }

    private void deleteRows(List<DeletionTarget> targets, RunCounter counter) {
        if (targets.isEmpty()) {
            return;
        }
        try {
            int deleted = deleteService.deleteRows(targets.stream().map(DeletionTarget::id).toList());
            counter.deleted += deleted;
            metrics.recordDeleted(deleted);
        } catch (DataIntegrityViolationException exception) {
            targets.forEach(target -> deleteRowIsolated(target, counter));
        }
    }

    private void deleteRowIsolated(DeletionTarget target, RunCounter counter) {
        try {
            int deleted = deleteService.deleteRows(List.of(target.id()));
            counter.deleted += deleted;
            metrics.recordDeleted(deleted);
        } catch (DataIntegrityViolationException exception) {
            logger.referenceViolation(target.id(), target.objectKey(), exception);
            counter.deleteFailed++;
            metrics.recordDeleteFailed("ReferenceViolation");
        }
    }

    private void recordFailure(DeletionTarget target, String error, RunCounter counter) {
        counter.deleteFailed++;
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

        private FileCleanupRunResult toResult(boolean dryRun, long durationMillis) {
            return new FileCleanupRunResult(dryRun, candidates, markedPendingExpired, markedUnreferenced,
                    markSkipped, deleted, deleteFailed, durationMillis);
        }
    }
}

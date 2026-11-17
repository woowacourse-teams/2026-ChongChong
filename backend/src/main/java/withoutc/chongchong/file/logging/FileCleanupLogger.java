package withoutc.chongchong.file.logging;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.event.Level;
import org.slf4j.spi.LoggingEventBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import withoutc.chongchong.file.worker.FileCleanupRunResult;

@Component
@Slf4j
public class FileCleanupLogger {

    private static final String COMPONENT = "file_cleanup_worker";

    private final String service;
    private final String environment;

    public FileCleanupLogger(
            @Value("${spring.application.name:unknown}") String service,
            @Value("${app.environment:local}") String environment
    ) {
        this.service = service;
        this.environment = environment;
    }

    public void runCompleted(FileCleanupRunResult result) {
        event(Level.INFO, result.dryRun() ? "file_cleanup_dry_run_completed" : "file_cleanup_run_completed")
                .addKeyValue("dry_run", result.dryRun())
                .addKeyValue("candidates", result.candidates())
                .addKeyValue("marked_pending_expired", result.markedPendingExpired())
                .addKeyValue("marked_unreferenced", result.markedUnreferenced())
                .addKeyValue("mark_skipped", result.markSkipped())
                .addKeyValue("deleted", result.deleted())
                .addKeyValue("delete_failed", result.deleteFailed())
                .addKeyValue("duration_ms", result.durationMillis())
                .log("file cleanup run completed");
    }

    public void deleteFailed(Long fileId, String objectKey, int attempts, String error) {
        event(Level.WARN, "file_cleanup_delete_failed")
                .addKeyValue("file_id", fileId)
                .addKeyValue("object_key", objectKey)
                .addKeyValue("attempts", attempts)
                .addKeyValue("error_code", error)
                .log("file cleanup delete failed");
    }

    public void referenceViolation(Long fileId, String objectKey, Throwable throwable) {
        event(Level.ERROR, "file_cleanup_reference_violation")
                .addKeyValue("file_id", fileId)
                .addKeyValue("object_key", objectKey)
                .log("file cleanup deleted an object whose row is still referenced", throwable);
    }

    public void runFailed(Throwable throwable) {
        event(Level.ERROR, "file_cleanup_run_failed").log("file cleanup run failed", throwable);
    }

    private LoggingEventBuilder event(Level level, String event) {
        return log.atLevel(level)
                .addKeyValue("service", service)
                .addKeyValue("environment", environment)
                .addKeyValue("component", COMPONENT)
                .addKeyValue("event", event);
    }
}

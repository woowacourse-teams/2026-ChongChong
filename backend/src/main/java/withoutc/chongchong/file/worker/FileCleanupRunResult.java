package withoutc.chongchong.file.worker;

public record FileCleanupRunResult(
        boolean dryRun,
        int candidates,
        int markedPendingExpired,
        int markedUnreferenced,
        int markSkipped,
        int deleted,
        int deleteFailed,
        long durationMillis
) {
}

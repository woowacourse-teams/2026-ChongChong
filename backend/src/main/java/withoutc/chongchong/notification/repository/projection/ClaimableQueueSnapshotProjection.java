package withoutc.chongchong.notification.repository.projection;

import java.time.LocalDateTime;

public record ClaimableQueueSnapshotProjection(
        long readyCount,
        LocalDateTime oldestReadyAt
) {
}

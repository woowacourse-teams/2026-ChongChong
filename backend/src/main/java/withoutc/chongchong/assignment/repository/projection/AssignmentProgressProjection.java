package withoutc.chongchong.assignment.repository.projection;

import java.time.LocalDateTime;

public record AssignmentProgressProjection(
        Long assignmentId,
        long memberCount,
        long completeCount,
        LocalDateTime nextRemindAt
) {
}

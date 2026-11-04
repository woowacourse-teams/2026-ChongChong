package withoutc.chongchong.assignment.repository.projection;

import java.time.LocalDateTime;

public record AssignmentCalendarProjection(
        Long id,
        String title,
        LocalDateTime createdAt,
        LocalDateTime closeAt,
        LocalDateTime submittedAt
) {
}

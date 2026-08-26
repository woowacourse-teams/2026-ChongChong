package withoutc.chongchong.assignment.repository.projection;

import java.time.LocalDateTime;

public record AssignmentSubmitterStatusProjection(
        Long memberId,
        String name,
        String profileImageUrl,
        boolean isSubmitted,
        LocalDateTime lastRemindAt
) {
}

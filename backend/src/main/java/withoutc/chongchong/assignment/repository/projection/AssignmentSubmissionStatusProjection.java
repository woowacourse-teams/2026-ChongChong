package withoutc.chongchong.assignment.repository.projection;

import java.time.LocalDateTime;

public record AssignmentSubmissionStatusProjection(
        Long assignmentId,
        LocalDateTime submittedAt
) {

    public boolean isSubmitted() {
        return submittedAt != null;
    }
}

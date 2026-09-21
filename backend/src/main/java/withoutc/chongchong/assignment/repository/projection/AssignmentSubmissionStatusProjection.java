package withoutc.chongchong.assignment.repository.projection;

import java.time.LocalDateTime;
import withoutc.chongchong.assignment.entity.SubmissionStatus;

public record AssignmentSubmissionStatusProjection(
        Long assignmentId,
        LocalDateTime submittedAt
) {

    public SubmissionStatus submissionStatus() {
        if (submittedAt == null) {
            return SubmissionStatus.NOT_SUBMITTED;
        }
        return SubmissionStatus.SUBMITTED;
    }
}

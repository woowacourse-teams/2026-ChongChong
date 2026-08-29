package withoutc.chongchong.assignment.repository.projection;

public record AssignmentSubmissionStatusProjection(
        Long assignmentId,
        boolean submitted
) {
}

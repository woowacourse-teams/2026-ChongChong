package withoutc.chongchong.assignment.controller.dto;

import withoutc.chongchong.assignment.entity.AssignmentSubmission;

public record AssignmentSubmitResponse(
        Long submissionId
) {
    public static AssignmentSubmitResponse from(AssignmentSubmission submission) {
        return new AssignmentSubmitResponse(submission.getId());
    }
}

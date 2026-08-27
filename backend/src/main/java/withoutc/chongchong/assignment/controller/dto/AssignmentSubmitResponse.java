package withoutc.chongchong.assignment.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import withoutc.chongchong.assignment.entity.AssignmentSubmission;

public record AssignmentSubmitResponse(
        @Schema(description = "생성된 제출 ID", example = "1")
        Long submissionId
) {
    public static AssignmentSubmitResponse from(AssignmentSubmission submission) {
        return new AssignmentSubmitResponse(submission.getId());
    }
}

package withoutc.chongchong.assignment.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import withoutc.chongchong.assignment.entity.AssignmentSubmission;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MySubmissionDetailResponse(
        Long submissionId,
        boolean submitted,
        LocalDateTime createdAt,
        String content,
        String link
) {
    public static MySubmissionDetailResponse from(AssignmentSubmission submission) {
        return new MySubmissionDetailResponse(
                submission.getId(),
                submission.isSubmitted(),
                submission.getSubmittedAt(),
                submission.getContent(),
                submission.getLink()
        );
    }
}

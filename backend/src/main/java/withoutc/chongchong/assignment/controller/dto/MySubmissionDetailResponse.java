package withoutc.chongchong.assignment.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import withoutc.chongchong.assignment.entity.AssignmentSubmission;
import withoutc.chongchong.assignment.entity.SubmissionStatus;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MySubmissionDetailResponse(
        Long submissionId,
        SubmissionStatus submissionStatus,
        LocalDateTime createdAt,
        String content,
        String link
) {
    public static MySubmissionDetailResponse notAssigned() {
        return new MySubmissionDetailResponse(
                null,
                SubmissionStatus.NOT_ASSIGNED,
                null,
                null,
                null
        );
    }

    public static MySubmissionDetailResponse from(AssignmentSubmission submission) {
        return new MySubmissionDetailResponse(
                submission.getId(),
                submission.submissionStatus(),
                submission.getSubmittedAt(),
                submission.getContent(),
                submission.getLink()
        );
    }
}

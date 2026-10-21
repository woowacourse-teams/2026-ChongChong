package withoutc.chongchong.assignment.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.List;
import withoutc.chongchong.assignment.entity.AssignmentSubmission;
import withoutc.chongchong.assignment.entity.SubmissionStatus;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MySubmissionDetailResponse(
        Long submissionId,
        SubmissionStatus submissionStatus,
        LocalDateTime createdAt,
        String content,
        String link,
        List<AssignmentAttachmentResponse> attachments
) {
    public static MySubmissionDetailResponse notAssigned() {
        return new MySubmissionDetailResponse(
                null,
                SubmissionStatus.NOT_ASSIGNED,
                null,
                null,
                null,
                List.of()
        );
    }

    public static MySubmissionDetailResponse of(LocalDateTime now, AssignmentSubmission submission,
                                                List<AssignmentAttachmentResponse> attachments) {
        return new MySubmissionDetailResponse(
                submission.getId(),
                submission.submissionStatus(now),
                submission.getSubmittedAt(),
                submission.getContent(),
                submission.getLink(),
                attachments
        );
    }
}

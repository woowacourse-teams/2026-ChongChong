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
        List<AssignmentAttachmentResponse> attachments,
        boolean assigned
) {
    public static MySubmissionDetailResponse notAssigned() {
        return new MySubmissionDetailResponse(
                null,
                SubmissionStatus.NOT_ASSIGNED,
                null,
                null,
                null,
                List.of(),
                false
        );
    }

    public static MySubmissionDetailResponse of(LocalDateTime now, AssignmentSubmission submission,
                                                List<AssignmentAttachmentResponse> attachments) {
        return new MySubmissionDetailResponse(
                submission.getId(),
                getSubmissionStatus(submission, now),
                submission.getSubmittedAt(),
                submission.getContent(),
                submission.getLink(),
                attachments,
                submission.isAssigned()
        );
    }

    private static SubmissionStatus getSubmissionStatus(AssignmentSubmission submission, LocalDateTime now) {
        if (!submission.isAssigned() && submission.getSubmittedAt() == null) {
            return SubmissionStatus.NOT_ASSIGNED;
        }
        return submission.submissionStatus(now);
    }
}

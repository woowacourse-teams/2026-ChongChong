package withoutc.chongchong.assignment.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import withoutc.chongchong.assignment.entity.Assignment;
import withoutc.chongchong.assignment.entity.SubmissionStatus;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AssignmentSummaryResponse(
        Long id,
        String title,
        String content,
        String submissionMethod,
        LocalDateTime closeAt,
        Integer memberCount,
        Integer completeCount,
        Boolean isComplete,
        LocalDateTime remindAt,
        SubmissionStatus submissionStatus
) {
    public static AssignmentSummaryResponse forLeader(Assignment assignment, SubmissionStatus submissionStatus) {

        int memberCount = assignment.getSubmissionCount();
        int completeCount = assignment.getSubmittedCount();

        boolean isComplete = (memberCount == completeCount);

        return new AssignmentSummaryResponse(
                assignment.getId(),
                assignment.getTitle(),
                assignment.getContent(),
                assignment.getSubmissionMethod(),
                assignment.getCloseAt(),
                memberCount,
                completeCount,
                isComplete,
                assignment.getNextRemindAt(),
                submissionStatus
        );
    }

    public static AssignmentSummaryResponse forMember(Assignment assignment, SubmissionStatus submissionStatus) {
        return new AssignmentSummaryResponse(
                assignment.getId(),
                assignment.getTitle(),
                assignment.getContent(),
                assignment.getSubmissionMethod(),
                assignment.getCloseAt(),
                null,
                null,
                null,
                null,
                submissionStatus
        );
    }
}

package withoutc.chongchong.assignment.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import withoutc.chongchong.assignment.entity.Assignment;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AssignmentSummaryResponse(
        Long id,
        String title,
        String content,
        String submissionType,
        LocalDateTime closeAt,
        Integer memberCount,
        Integer completeCount,
        LocalDateTime remindAt,
        boolean isComplete
) {
    public static AssignmentSummaryResponse forLeader(Assignment assignment) {

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
                assignment.getNextRemindAt(),
                isComplete
        );
    }

    public static AssignmentSummaryResponse forMember(Assignment assignment, boolean isComplete) {
        return new AssignmentSummaryResponse(
                assignment.getId(),
                assignment.getTitle(),
                assignment.getContent(),
                assignment.getSubmissionMethod(),
                assignment.getCloseAt(),
                null,
                null,
                null,
                isComplete
        );
    }
}

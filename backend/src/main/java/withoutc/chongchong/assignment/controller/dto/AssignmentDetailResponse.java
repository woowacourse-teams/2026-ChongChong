package withoutc.chongchong.assignment.controller.dto;

import java.time.LocalDateTime;
import withoutc.chongchong.assignment.entity.Assignment;

public record AssignmentDetailResponse(
        Long id,
        String title,
        String content,
        String submissionMethod,
        LocalDateTime closeAt
) {
    public static AssignmentDetailResponse from(Assignment assignment) {
        return new AssignmentDetailResponse(
                assignment.getId(),
                assignment.getTitle(),
                assignment.getContent(),
                assignment.getSubmissionMethod(),
                assignment.getCloseAt()
        );
    }
}

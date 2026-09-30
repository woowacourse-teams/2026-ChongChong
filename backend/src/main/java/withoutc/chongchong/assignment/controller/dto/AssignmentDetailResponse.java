package withoutc.chongchong.assignment.controller.dto;

import java.time.LocalDateTime;
import withoutc.chongchong.assignment.entity.Assignment;
import withoutc.chongchong.assignment.entity.SubmissionTarget;
import withoutc.chongchong.assignment.entity.SubmissionVisibility;

public record AssignmentDetailResponse(
        Long id,
        String title,
        String content,
        String submissionMethod,
        SubmissionTarget submissionTarget,
        SubmissionVisibility submissionVisibility,
        LocalDateTime closeAt
) {
    public static AssignmentDetailResponse from(Assignment assignment) {
        return new AssignmentDetailResponse(
                assignment.getId(),
                assignment.getTitle(),
                assignment.getContent(),
                assignment.getSubmissionMethod(),
                assignment.getSubmissionTarget(),
                assignment.getSubmissionVisibility(),
                assignment.getCloseAt()
        );
    }
}

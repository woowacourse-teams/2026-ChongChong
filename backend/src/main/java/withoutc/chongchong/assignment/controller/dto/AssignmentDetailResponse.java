package withoutc.chongchong.assignment.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import withoutc.chongchong.assignment.entity.Assignment;

public record AssignmentDetailResponse(
        @Schema(description = "과제 ID", example = "1")
        Long id,
        @Schema(description = "과제 제목", example = "1주 차 과제")
        String title,
        @Schema(description = "과제 내용", example = "이번 주 과제를 제출해주세요.")
        String content,
        @Schema(description = "과제 제출 방법", example = "링크 제출")
        String submissionMethod,
        @Schema(description = "과제 마감 시각", example = "2026-08-29T23:59:00")
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

package withoutc.chongchong.assignment.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import withoutc.chongchong.assignment.entity.AssignmentSubmission;

public record MySubmissionDetailResponse(
        @Schema(description = "제출 완료 여부", example = "true")
        boolean submitted,
        @Schema(description = "제출 시각", example = "2026-08-27T10:30:00", nullable = true)
        LocalDateTime createdAt,
        @Schema(description = "제출 내용", example = "이번 주 과제를 완료했습니다.", nullable = true)
        String content,
        @Schema(description = "제출 링크", example = "https://github.com/example/project", nullable = true)
        String link
) {
    public static MySubmissionDetailResponse from(AssignmentSubmission submission) {
        return new MySubmissionDetailResponse(
                submission.isSubmitted(),
                submission.getSubmittedAt(),
                submission.getContent(),
                submission.getLink()
        );
    }
}

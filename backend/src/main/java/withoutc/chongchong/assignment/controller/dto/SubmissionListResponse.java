package withoutc.chongchong.assignment.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import withoutc.chongchong.assignment.entity.AssignmentSubmission;
import withoutc.chongchong.study.entity.StudyMember;

public record SubmissionListResponse(
        @Schema(description = "과제 제출 목록")
        List<SubmissionSummary> submissions
) {
    public static SubmissionListResponse from(List<SubmissionSummary> submissions) {
        return new SubmissionListResponse(submissions);
    }

    public record SubmissionSummary(
            @Schema(description = "제출 ID", example = "1")
            Long id,
            @Schema(description = "제출자 이름", example = "홍길동")
            String name,
            @Schema(description = "제출자 프로필 이미지 URL", example = "https://example.com/profile.png", nullable = true)
            String profileImage,
            @Schema(description = "제출 시각", example = "2026-08-27T10:30:00")
            LocalDateTime createdAt
    ) {
        public static SubmissionSummary from(AssignmentSubmission submission) {
            StudyMember member = submission.getMember();
            return new SubmissionSummary(
                    submission.getId(),
                    member.getName(),
                    member.getProfileImageUrl(),
                    submission.getSubmittedAt()
            );
        }
    }
}

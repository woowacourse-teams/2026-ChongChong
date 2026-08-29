package withoutc.chongchong.assignment.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import withoutc.chongchong.assignment.entity.AssignmentSubmission;
import withoutc.chongchong.study.entity.StudyMember;

public record SubmissionDetailResponse(
        @Schema(description = "제출 ID", example = "1")
        Long id,
        @Schema(description = "제출자 이름", example = "홍길동")
        String name,
        @Schema(description = "제출자 프로필 이미지 URL", example = "https://example.com/profile.png", nullable = true)
        String profileImage,
        @Schema(description = "제출 시각", example = "2026-08-27T10:30:00")
        LocalDateTime createdAt,
        @Schema(description = "제출 내용", example = "이번 주 과제를 완료했습니다.", nullable = true)
        String content,
        @Schema(description = "제출 링크", example = "https://github.com/example/project", nullable = true)
        String link
) {
    public static SubmissionDetailResponse of(AssignmentSubmission submission, StudyMember member) {
        return new SubmissionDetailResponse(
                submission.getId(),
                member.getName(),
                member.getProfileImageUrl(),
                submission.getSubmittedAt(),
                submission.getContent(),
                submission.getLink()
        );
    }
}

package withoutc.chongchong.assignment.controller.dto;

import java.time.LocalDateTime;
import withoutc.chongchong.assignment.entity.AssignmentSubmission;
import withoutc.chongchong.study.entity.StudyMember;

public record SubmissionDetailResponse(
        Long id,
        String name,
        String profileImage,
        LocalDateTime createdAt,
        String content,
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

package withoutc.chongchong.assignment.controller.dto;

import java.time.LocalDateTime;
import java.util.List;
import withoutc.chongchong.assignment.entity.AssignmentSubmission;
import withoutc.chongchong.study.entity.StudyMember;

public record SubmissionListResponse(
        List<SubmissionSummary> submissions
) {
    public static SubmissionListResponse from(List<SubmissionSummary> submissions) {
        return new SubmissionListResponse(submissions);
    }

    public record SubmissionSummary(
            Long id,
            String name,
            String profileImage,
            LocalDateTime createdAt
    ) {
        public static SubmissionSummary from(AssignmentSubmission submission) {
            StudyMember member = submission.getMember();
            return new SubmissionSummary(
                    submission.getId(),
                    member.getName(),
                    member.getProfileImageUrl(),
                    submission.getCreatedAt()
            );
        }
    }
}

package withoutc.chongchong.comment.repository.projection;

import java.time.LocalDateTime;
import withoutc.chongchong.study.entity.StudyMemberRole;

public record CommentSummaryProjection(
        Long id,
        Long authorUserId,
        Long authorMemberId,
        String authorName,
        String profileImageUrl,
        StudyMemberRole authorRole,
        LocalDateTime createdAt,
        String content,
        LocalDateTime editedAt
) {
}

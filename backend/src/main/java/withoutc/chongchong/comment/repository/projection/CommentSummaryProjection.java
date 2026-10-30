package withoutc.chongchong.comment.repository.projection;

import java.time.LocalDateTime;

public record CommentSummaryProjection(
        Long id,
        Long authorUserId,
        Long authorMemberId,
        String authorName,
        String profileImageUrl,
        LocalDateTime createdAt,
        String content,
        LocalDateTime editedAt
) {
}

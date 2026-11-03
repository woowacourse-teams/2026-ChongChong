package withoutc.chongchong.comment.controller.dto;

import java.time.LocalDateTime;
import withoutc.chongchong.comment.repository.projection.CommentSummaryProjection;

public record CommentResponse(
        Long id,
        Author author,
        LocalDateTime createdAt,
        String content,
        boolean isEdited,
        boolean canEdit,
        boolean canDelete
) {
    private static final String DEPARTED_AUTHOR_NAME = "탈퇴한 스터디원";

    public static CommentResponse from(CommentSummaryProjection comment, boolean canEdit, boolean canDelete) {
        Author author = Author.of(comment.authorName(), comment.profileImageUrl(), comment.authorMemberId() == null);
        return new CommentResponse(comment.id(), author, comment.createdAt(), comment.content(),
                comment.editedAt() != null, canEdit, canDelete);
    }

    public record Author(String name, String profileImageUrl, boolean isDeparted) {

        public static Author of(String name, String profileImageUrl, boolean isDeparted) {
            if (isDeparted) {
                return new Author(DEPARTED_AUTHOR_NAME, null, true);
            }
            return new Author(name, profileImageUrl, false);
        }
    }
}

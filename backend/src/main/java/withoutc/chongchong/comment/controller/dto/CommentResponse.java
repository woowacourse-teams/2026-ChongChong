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
        boolean departed = comment.authorMemberId() == null;
        Author author;
        if (departed) {
            author = new Author(DEPARTED_AUTHOR_NAME, null, true);
        } else {
            author = new Author(comment.authorName(), comment.profileImageUrl(), false);
        }
        return new CommentResponse(comment.id(), author, comment.createdAt(), comment.content(),
                comment.editedAt() != null, canEdit, canDelete);
    }

    public record Author(String name, String profileImageUrl, boolean isDeparted) {
    }
}

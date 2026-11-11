package withoutc.chongchong.comment.controller.dto;

import java.time.LocalDateTime;
import withoutc.chongchong.comment.repository.projection.CommentSummaryProjection;
import withoutc.chongchong.study.entity.StudyMemberRole;

public record CommentResponse(
        Long id,
        Author author,
        LocalDateTime createdAt,
        String content,
        boolean isEdited,
        boolean isMine,
        boolean canEdit,
        boolean canDelete
) {
    private static final String DEPARTED_AUTHOR_NAME = "탈퇴한 스터디원";

    public static CommentResponse from(CommentSummaryProjection comment, Long userId, boolean canEdit, boolean canDelete) {
        Author author = Author.of(comment.authorName(), comment.profileImageUrl(), comment.authorRole(),
                comment.authorMemberId() == null);
        boolean isMine = userId.equals(comment.authorUserId());
        return new CommentResponse(comment.id(), author, comment.createdAt(), comment.content(),
                comment.editedAt() != null, isMine, canEdit, canDelete);
    }

    public record Author(String name, String profileImageUrl, StudyMemberRole role, boolean isDeparted) {

        public static Author of(String name, String profileImageUrl, StudyMemberRole role, boolean isDeparted) {
            if (isDeparted) {
                return new Author(DEPARTED_AUTHOR_NAME, null, null, true);
            }
            return new Author(name, profileImageUrl, role, false);
        }
    }
}

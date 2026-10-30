package withoutc.chongchong.comment.policy;

import org.springframework.stereotype.Component;
import withoutc.chongchong.auth.exception.AuthErrorCode;
import withoutc.chongchong.auth.exception.AuthException;
import withoutc.chongchong.comment.entity.Comment;

@Component
public class CommentAccessPolicy {

    public boolean canEdit(Long userId, Long authorUserId) {
        return authorUserId != null && authorUserId.equals(userId);
    }

    public boolean canDelete(Long userId, Long authorUserId, boolean isLeader) {
        return canEdit(userId, authorUserId) || isLeader;
    }

    public void requireCanEdit(Long userId, Comment comment) {
        if (!canEdit(userId, comment.authorUserId())) {
            throw new AuthException(AuthErrorCode.ACCESS_DENIED);
        }
    }

    public void requireCanDelete(Long userId, Comment comment, boolean isLeader) {
        if (!canDelete(userId, comment.authorUserId(), isLeader)) {
            throw new AuthException(AuthErrorCode.ACCESS_DENIED);
        }
    }
}

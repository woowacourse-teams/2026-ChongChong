package withoutc.chongchong.comment.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import withoutc.chongchong.auth.exception.AuthException;
import withoutc.chongchong.comment.entity.Comment;
import withoutc.chongchong.notification.entity.ResourceType;
import withoutc.chongchong.study.entity.StudyMember;
import withoutc.chongchong.user.entity.User;

class CommentAccessPolicyTest {

    private final CommentAccessPolicy policy = new CommentAccessPolicy();

    @Test
    @DisplayName("작성자 본인만 수정하고 작성자 또는 현재 리더만 삭제한다")
    void ownershipAndLeaderPermissions() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(7L);
        StudyMember author = mock(StudyMember.class);
        when(author.getUser()).thenReturn(user);
        Comment comment = Comment.create(ResourceType.NOTICE, 1L, author, "내용");

        policy.requireCanEdit(7L, comment);
        policy.requireCanDelete(7L, comment, false);
        policy.requireCanDelete(8L, comment, true);
        assertThatThrownBy(() -> policy.requireCanEdit(8L, comment)).isInstanceOf(AuthException.class);
        assertThatThrownBy(() -> policy.requireCanDelete(8L, comment, false)).isInstanceOf(AuthException.class);
        assertThat(policy.canEdit(7L, null)).isFalse();
        assertThat(policy.canDelete(7L, null, true)).isTrue();
        assertThat(policy.canDelete(7L, null, false)).isFalse();
    }
}

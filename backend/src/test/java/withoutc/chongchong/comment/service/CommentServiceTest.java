package withoutc.chongchong.comment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import withoutc.chongchong.comment.controller.dto.CommentWriteRequest;
import withoutc.chongchong.comment.entity.Comment;
import withoutc.chongchong.comment.exception.CommentErrorCode;
import withoutc.chongchong.comment.exception.CommentException;
import withoutc.chongchong.comment.policy.CommentAccessPolicy;
import withoutc.chongchong.comment.repository.CommentRepository;
import withoutc.chongchong.notice.entity.Notice;
import withoutc.chongchong.notice.repository.NoticeRepository;
import withoutc.chongchong.notification.service.NotificationService;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.entity.StudyMember;
import withoutc.chongchong.study.entity.StudyMemberRole;
import withoutc.chongchong.study.repository.StudyMemberRepository;
import withoutc.chongchong.study.repository.projection.StudyMemberIdentityProjection;
import withoutc.chongchong.user.repository.UserRepository;
import withoutc.chongchong.user.entity.User;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository comments;

    @Mock
    private NoticeRepository notices;

    @Mock
    private StudyMemberRepository members;

    @Mock
    private UserRepository users;

    @Mock
    private NotificationService notifications;

    private CommentService service;
    private StudyMember author;
    private Notice notice;

    @BeforeEach
    void setUp() {
        service = new CommentService(comments, notices, members, users, notifications,
                new CommentAccessPolicy(), Clock.systemUTC());
        author = member(1L);
        notice = mock(Notice.class);
    }

    @Test
    @DisplayName("등록 시점 리더 후보에서 작성자와 중복 사용자를 제외하고 저장한다")
    void filterRecipientsBeforeNotificationCreation() {
        locked(Set.of(1L, 2L));
        StudyMember leader = member(2L);
        when(members.findAllByStudyIdAndRole(10L, StudyMemberRole.LEADER))
                .thenReturn(List.of(author, leader, leader));
        when(comments.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment comment = invocation.getArgument(0);
            ReflectionTestUtils.setField(comment, "id", 42L);
            return comment;
        });

        assertThat(service.create(1L, 10L, 20L, new CommentWriteRequest(" 내용 ")).commentId()).isEqualTo(42L);

        ArgumentCaptor<Comment> saved = ArgumentCaptor.forClass(Comment.class);
        verify(comments).save(saved.capture());
        assertThat(saved.getValue().getContent()).isEqualTo("내용");
        verify(notifications).createNoticeCommentCreatedEventNotifications(saved.getValue(), notice, author,
                List.of(leader));
    }

    @Test
    @DisplayName("잠금 뒤 새 수신 후보가 필요하면 저장하지 않고 상태 변경 충돌을 반환한다")
    void rejectNewUnlockedRecipient() {
        locked(Set.of(1L));
        when(members.findAllByStudyIdAndRole(10L, StudyMemberRole.LEADER)).thenReturn(List.of(member(2L)));

        assertThatThrownBy(() -> service.create(1L, 10L, 20L, new CommentWriteRequest("내용")))
                .isInstanceOf(CommentException.class)
                .extracting(exception -> ((CommentException) exception).getErrorCode())
                .isEqualTo(CommentErrorCode.STATE_CHANGED);
        verify(comments, never()).save(any());
        verifyNoInteractions(notifications);
    }

    @Test
    @DisplayName("수신 후보가 줄어 없어져도 댓글은 정상 생성한다")
    void allowRecipientReduction() {
        locked(Set.of(1L, 2L));
        when(members.findAllByStudyIdAndRole(10L, StudyMemberRole.LEADER)).thenReturn(List.of());
        when(comments.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.create(1L, 10L, 20L, new CommentWriteRequest("내용"));

        ArgumentCaptor<Comment> saved = ArgumentCaptor.forClass(Comment.class);
        verify(comments).save(saved.capture());
        verify(notifications).createNoticeCommentCreatedEventNotifications(saved.getValue(), notice, author, List.of());
    }

    private void locked(Set<Long> userIds) {
        when(members.findIdentitiesByStudyId(10L)).thenReturn(userIds.stream()
                .map(id -> new StudyMemberIdentityProjection(id, id)).toList());
        when(members.getByStudyIdAndUserIdOrThrow(10L, 1L)).thenReturn(author);
        when(notices.getByIdAndStudyIdForUpdateOrThrow(20L, 10L)).thenReturn(notice);
    }

    private StudyMember member(Long id) {
        User user = User.create("사용자", null);
        ReflectionTestUtils.setField(user, "id", id);
        StudyMember member = StudyMember.create(mock(Study.class), user, "사용자", null, StudyMemberRole.LEADER);
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }
}

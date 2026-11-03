package withoutc.chongchong.comment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import withoutc.chongchong.assignment.policy.AssignmentAccessPolicy;
import withoutc.chongchong.assignment.repository.AssignmentRepository;
import withoutc.chongchong.assignment.repository.AssignmentSubmissionRepository;
import withoutc.chongchong.comment.controller.dto.CommentWriteRequest;
import withoutc.chongchong.comment.controller.dto.CommentListResponse;
import withoutc.chongchong.comment.entity.Comment;
import withoutc.chongchong.comment.exception.CommentErrorCode;
import withoutc.chongchong.comment.exception.CommentException;
import withoutc.chongchong.comment.policy.CommentAccessPolicy;
import withoutc.chongchong.comment.repository.CommentRepository;
import withoutc.chongchong.comment.repository.projection.CommentSummaryProjection;
import withoutc.chongchong.notice.entity.Notice;
import withoutc.chongchong.notice.repository.NoticeRepository;
import withoutc.chongchong.notification.service.NotificationService;
import withoutc.chongchong.notification.entity.ResourceType;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.entity.StudyMember;
import withoutc.chongchong.study.entity.StudyMemberRole;
import withoutc.chongchong.study.repository.StudyMemberRepository;
import withoutc.chongchong.study.repository.projection.StudyMemberIdentityProjection;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;
import withoutc.chongchong.user.service.UserProfileImageUrlResolver;

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

    @Mock
    private UserProfileImageUrlResolver profileImageUrlResolver;

    private CommentService service;
    private StudyMember author;
    private Notice notice;

    @BeforeEach
    void setUp() {
        CommentTargetResolver resolver = new CommentTargetResolver(notices, mock(AssignmentRepository.class),
                mock(AssignmentSubmissionRepository.class), new AssignmentAccessPolicy());
        service = new CommentService(comments, resolver, members, users, notifications,
                new CommentAccessPolicy(), Clock.systemUTC(), profileImageUrlResolver);
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

        assertThat(service.create(1L, 10L, CommentTarget.notice(20L), new CommentWriteRequest(" 내용 ")).commentId()).isEqualTo(42L);

        ArgumentCaptor<Comment> saved = ArgumentCaptor.forClass(Comment.class);
        verify(comments).save(saved.capture());
        assertThat(saved.getValue().getContent()).isEqualTo("내용");
        verify(notifications).createCommentCreatedEventNotifications(saved.getValue(), notice.getStudy(), author,
                List.of(leader));
    }

    @Test
    @DisplayName("잠금 뒤 새 수신 후보가 필요하면 저장하지 않고 상태 변경 충돌을 반환한다")
    void rejectNewUnlockedRecipient() {
        locked(Set.of(1L));
        when(members.findAllByStudyIdAndRole(10L, StudyMemberRole.LEADER)).thenReturn(List.of(member(2L)));

        assertThatThrownBy(() -> service.create(1L, 10L, CommentTarget.notice(20L), new CommentWriteRequest("내용")))
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

        service.create(1L, 10L, CommentTarget.notice(20L), new CommentWriteRequest("내용"));

        ArgumentCaptor<Comment> saved = ArgumentCaptor.forClass(Comment.class);
        verify(comments).save(saved.capture());
        verify(notifications).createCommentCreatedEventNotifications(saved.getValue(), notice.getStudy(), author, List.of());
    }

    @Test
    @DisplayName("현재 페이지의 활동 중 작성자만 중복 없이 해석하고 탈퇴자·다음 페이지 작성자는 제외한다")
    void resolvesVisibleActiveAuthorsOnly() {
        LocalDateTime now = LocalDateTime.of(2026, 11, 3, 10, 0);
        when(members.getByStudyIdAndUserIdOrThrow(10L, 1L)).thenReturn(author);
        when(notices.getByIdAndStudyIdOrThrow(20L, 10L)).thenReturn(notice);
        when(comments.findByCursor(eq(ResourceType.NOTICE), eq(20L), isNull(), eq(false), any()))
                .thenReturn(List.of(
                        new CommentSummaryProjection(1L, 2L, 7L, "스터디 이름", "stale.png", now, "첫 댓글", null),
                        new CommentSummaryProjection(2L, 2L, 7L, "스터디 이름", "stale.png", now, "둘째 댓글", now),
                        new CommentSummaryProjection(3L, 3L, null, "예전 이름", "private.png", now, "탈퇴 댓글", null),
                        new CommentSummaryProjection(4L, 4L, 8L, "다음 작성자", "next.png", now, "다음 댓글", null)));
        when(profileImageUrlResolver.resolveForMembers(List.of(7L))).thenReturn(Map.of(7L, "current-home.png"));

        CommentListResponse response = service.getList(1L, 10L, CommentTarget.notice(20L), null, null, 3);

        assertThat(response.hasNext()).isTrue();
        assertThat(response.nextCursor()).isEqualTo(3L);
        assertThat(response.comments()).hasSize(3);
        assertThat(response.comments().getFirst().author().name()).isEqualTo("스터디 이름");
        assertThat(response.comments().getFirst().author().profileImageUrl()).isEqualTo("current-home.png");
        assertThat(response.comments().get(1).author().profileImageUrl()).isEqualTo("current-home.png");
        assertThat(response.comments().get(1).isEdited()).isTrue();
        assertThat(response.comments().getLast().author().name()).isEqualTo("탈퇴한 스터디원");
        assertThat(response.comments().getLast().author().profileImageUrl()).isNull();
        assertThat(response.comments().getLast().author().isDeparted()).isTrue();
        verify(profileImageUrlResolver).resolveForMembers(List.of(7L));
    }

    @Test
    @DisplayName("댓글을 조회한 뒤 이미지 조회 전에 멤버가 사라져도 예전 이름과 이미지를 표시하지 않는다")
    void masksAuthorMissingFromImageSources() {
        LocalDateTime now = LocalDateTime.of(2026, 11, 3, 10, 0);
        when(members.getByStudyIdAndUserIdOrThrow(10L, 1L)).thenReturn(author);
        when(notices.getByIdAndStudyIdOrThrow(20L, 10L)).thenReturn(notice);
        when(comments.findByCursor(eq(ResourceType.NOTICE), eq(20L), isNull(), eq(false), any()))
                .thenReturn(List.of(new CommentSummaryProjection(
                        1L, 2L, 7L, "예전 이름", "private.png", now, "댓글 내용", null)));
        when(profileImageUrlResolver.resolveForMembers(List.of(7L))).thenReturn(Map.of());

        CommentListResponse response = service.getList(1L, 10L, CommentTarget.notice(20L), null, null, 3);

        assertThat(response.comments().getFirst().author().name()).isEqualTo("탈퇴한 스터디원");
        assertThat(response.comments().getFirst().author().profileImageUrl()).isNull();
        assertThat(response.comments().getFirst().author().isDeparted()).isTrue();
        assertThat(response.comments().getFirst().content()).isEqualTo("댓글 내용");
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

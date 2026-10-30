package withoutc.chongchong.comment.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import withoutc.chongchong.comment.controller.dto.CommentCreateResponse;
import withoutc.chongchong.comment.controller.dto.CommentListResponse;
import withoutc.chongchong.comment.controller.dto.CommentResponse;
import withoutc.chongchong.comment.controller.dto.CommentWriteRequest;
import withoutc.chongchong.comment.entity.Comment;
import withoutc.chongchong.comment.exception.CommentErrorCode;
import withoutc.chongchong.comment.exception.CommentException;
import withoutc.chongchong.comment.policy.CommentAccessPolicy;
import withoutc.chongchong.comment.repository.CommentRepository;
import withoutc.chongchong.comment.repository.projection.CommentSummaryProjection;
import withoutc.chongchong.global.pagination.CursorPageRequest;
import withoutc.chongchong.global.pagination.CursorPageResponse;
import withoutc.chongchong.notice.entity.Notice;
import withoutc.chongchong.notice.repository.NoticeRepository;
import withoutc.chongchong.notification.entity.ResourceType;
import withoutc.chongchong.notification.service.NotificationService;
import withoutc.chongchong.study.entity.StudyMember;
import withoutc.chongchong.study.entity.StudyMemberRole;
import withoutc.chongchong.study.repository.StudyMemberRepository;
import withoutc.chongchong.study.repository.projection.StudyMemberIdentityProjection;
import withoutc.chongchong.user.repository.UserRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private static final long MIN_COMMENT_ID = 1;
    private static final String INVALID_FOCUS_COMMENT_ID_MESSAGE = "댓글 ID는 양수여야 합니다.";

    private final CommentRepository commentRepository;
    private final NoticeRepository noticeRepository;
    private final StudyMemberRepository studyMemberRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final CommentAccessPolicy commentAccessPolicy;
    private final Clock clock;

    @Transactional
    public CommentCreateResponse create(Long userId, Long studyId, Long noticeId, CommentWriteRequest request) {
        List<StudyMemberIdentityProjection> candidates = studyMemberRepository.findIdentitiesByStudyId(studyId);
        userRepository.findAllByIdInForShare(Stream.concat(Stream.of(userId), candidates.stream()
                .map(StudyMemberIdentityProjection::userId)).distinct().sorted().toList());
        userRepository.getByIdOrThrow(userId);
        Notice notice = noticeRepository.getByIdAndStudyIdForUpdateOrThrow(noticeId, studyId);
        StudyMember author = studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, userId);
        List<StudyMember> recipients = recipients(userId, studyId, candidates);
        Comment comment = commentRepository.save(Comment.create(ResourceType.NOTICE, noticeId, author,
                request.content()));
        notificationService.createNoticeCommentCreatedEventNotifications(comment, notice, author, recipients);
        return new CommentCreateResponse(comment.getId());
    }

    public CommentListResponse getList(Long userId, Long studyId, Long noticeId,
                                       Long cursor, Long focusCommentId, int size) {
        CursorPageRequest pageRequest = CursorPageRequest.of(cursor, size);
        validateFocusComment(cursor, focusCommentId);
        StudyMember actor = studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, userId);
        noticeRepository.getByIdAndStudyIdOrThrow(noticeId, studyId);
        Long cursorId = pageRequest.cursor();
        if (focusCommentId != null) {
            Comment focus = commentRepository.getByIdAndResourceOrThrow(focusCommentId, ResourceType.NOTICE, noticeId);
            cursorId = focus.getId();
        }
        PageRequest pageable = PageRequest.of(0, pageRequest.fetchSize());
        List<CommentSummaryProjection> results = commentRepository.findByCursor(
                ResourceType.NOTICE, noticeId, cursorId, focusCommentId != null, pageable);
        CursorPageResponse<CommentSummaryProjection> commentPage = CursorPageResponse.of(
                results, pageRequest, CommentSummaryProjection::id);
        List<CommentResponse> comments = commentPage.content().stream()
                .map(comment -> CommentResponse.from(comment,
                        commentAccessPolicy.canEdit(userId, comment.authorUserId()),
                        commentAccessPolicy.canDelete(userId, comment.authorUserId(), actor.isLeader())))
                .toList();
        return new CommentListResponse(comments, commentPage.nextCursor(), commentPage.hasNext());
    }

    @Transactional
    public void update(Long userId, Long studyId, Long noticeId, Long commentId, CommentWriteRequest request) {
        userRepository.getByIdForShareOrThrow(userId);
        noticeRepository.getByIdAndStudyIdForUpdateOrThrow(noticeId, studyId);
        Comment comment = commentRepository.getByIdAndResourceOrThrow(commentId, ResourceType.NOTICE, noticeId);
        commentAccessPolicy.requireCanEdit(userId, comment);
        comment.update(request.content(), LocalDateTime.now(clock));
    }

    @Transactional
    public void delete(Long userId, Long studyId, Long noticeId, Long commentId) {
        userRepository.getByIdForShareOrThrow(userId);
        noticeRepository.getByIdAndStudyIdForUpdateOrThrow(noticeId, studyId);
        Comment comment = commentRepository.getByIdAndResourceOrThrow(commentId, ResourceType.NOTICE, noticeId);
        boolean isLeader = studyMemberRepository.findByStudyIdAndUserId(studyId, userId)
                .map(StudyMember::isLeader).orElse(false);
        commentAccessPolicy.requireCanDelete(userId, comment, isLeader);
        commentRepository.delete(comment);
    }

    private List<StudyMember> recipients(Long userId, Long studyId,
                                         List<StudyMemberIdentityProjection> candidates) {
        Map<Long, StudyMember> recipients = new LinkedHashMap<>();
        for (StudyMember leader : studyMemberRepository.findAllByStudyIdAndRole(studyId, StudyMemberRole.LEADER)) {
            Long recipientId = leader.getUser().getId();
            if (userId.equals(recipientId)) {
                continue;
            }
            if (candidates.stream().noneMatch(candidate -> candidate.matches(leader))) {
                throw new CommentException(CommentErrorCode.STATE_CHANGED);
            }
            recipients.putIfAbsent(recipientId, leader);
        }
        return List.copyOf(recipients.values());
    }

    private void validateFocusComment(Long cursor, Long focusCommentId) {
        if (cursor != null && focusCommentId != null) {
            throw new CommentException(CommentErrorCode.INVALID_PAGE_REQUEST);
        }
        if (focusCommentId != null && focusCommentId < MIN_COMMENT_ID) {
            throw new IllegalArgumentException(INVALID_FOCUS_COMMENT_ID_MESSAGE);
        }
    }
}

package withoutc.chongchong.notice.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import withoutc.chongchong.auth.exception.AuthErrorCode;
import withoutc.chongchong.auth.exception.AuthException;
import withoutc.chongchong.global.pagination.CursorPageRequest;
import withoutc.chongchong.global.pagination.CursorPageResponse;
import withoutc.chongchong.notice.controller.dto.NoticeCreateRequest;
import withoutc.chongchong.notice.controller.dto.NoticeCreateResponse;
import withoutc.chongchong.notice.controller.dto.NoticeDetailResponse;
import withoutc.chongchong.notice.controller.dto.NoticeListResponse;
import withoutc.chongchong.notice.controller.dto.NoticeReadResponse;
import withoutc.chongchong.notice.controller.dto.NoticeReadStatusResponse;
import withoutc.chongchong.notice.controller.dto.NoticeStatusesResponse;
import withoutc.chongchong.notice.controller.dto.NoticeStatusesResponse.UnreadMember;
import withoutc.chongchong.notice.controller.dto.NoticeSummaryResponse;
import withoutc.chongchong.notice.controller.dto.NoticeUpdateRequest;
import withoutc.chongchong.notice.entity.Notice;
import withoutc.chongchong.notice.entity.NoticeRecipient;
import withoutc.chongchong.notice.exception.NoticeErrorCode;
import withoutc.chongchong.notice.exception.NoticeException;
import withoutc.chongchong.notice.repository.NoticeRecipientRepository;
import withoutc.chongchong.notice.repository.NoticeRepository;
import withoutc.chongchong.notice.repository.projection.NoticeReadStatusProjection;
import withoutc.chongchong.notice.repository.projection.NoticeRecipientStatusProjection;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.entity.StudyMember;
import withoutc.chongchong.study.repository.StudyMemberRepository;
import withoutc.chongchong.study.repository.StudyRepository;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class NoticeService {
    private final StudyRepository studyRepository;
    private final StudyMemberRepository studyMemberRepository;
    private final NoticeRepository noticeRepository;
    private final NoticeRecipientRepository noticeRecipientRepository;

    private final Clock clock;

    @Transactional
    public NoticeCreateResponse create(Long userId, Long studyId, NoticeCreateRequest request) {
        Study study = studyRepository.getByIdOrThrow(studyId);

        validateLeader(studyId, userId);

        List<StudyMember> members = studyMemberRepository.findAllByStudyId(studyId).stream()
                .filter(studyMember -> !studyMember.isLeader()).toList();

        StudyMember writer = studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, userId);

        Notice notice = Notice.create(writer, request.title(), request.content());
        LocalDateTime now = LocalDateTime.now(clock);
        notice.addReminders(request.remindAts(), now);
        notice.addRecipients(members);

        noticeRepository.save(notice);

        return NoticeCreateResponse.from(notice);
    }

    @Transactional
    public void delete(Long userId, Long studyId, Long noticeId) {
        validateLeader(studyId, userId);

        Notice notice = noticeRepository.getByIdOrThrow(noticeId);
        validateNoticeBelongsToStudy(studyId, notice);

        noticeRepository.delete(notice);
    }

    @Transactional
    public void update(Long userId, Long studyId, Long noticeId, NoticeUpdateRequest request) {
        validateLeader(studyId, userId);

        Notice notice = noticeRepository.getByIdOrThrow(noticeId);
        validateNoticeBelongsToStudy(studyId, notice);

        LocalDateTime now = LocalDateTime.now(clock);
        notice.update(request.title(), request.content(), request.remindAts(), now);
        noticeRepository.save(notice);
    }

    public NoticeListResponse getList(Long userId, Long studyId, Long cursor, int size) {
        CursorPageRequest pageRequest = CursorPageRequest.of(cursor, size);
        StudyMember member = studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, userId);

        Pageable pageable = PageRequest.of(0, pageRequest.fetchSize());
        List<Notice> notices = noticeRepository.findByCursor(studyId, pageRequest.cursor(), pageable);

        CursorPageResponse<Notice> noticePage = CursorPageResponse.of(notices, pageRequest, Notice::getId);

        List<NoticeSummaryResponse> noticeSummaries = createNoticeSummaries(member, noticePage.content());
        return NoticeListResponse.of(noticePage.nextCursor(), noticePage.hasNext(), noticeSummaries);
    }

    public NoticeStatusesResponse getNoticeStatuses(Long userId, Long studyId, Long noticeId) {
        validateLeader(studyId, userId);

        Notice notice = noticeRepository.getByIdOrThrow(noticeId);
        validateNoticeBelongsToStudy(studyId, notice);

        List<NoticeRecipientStatusProjection> statuses = noticeRecipientRepository.findStatusesByNoticeId(noticeId);

        List<NoticeStatusesResponse.ReadMember> readMembers = statuses.stream()
                .filter(NoticeRecipientStatusProjection::isRead)
                .map(status -> NoticeStatusesResponse.ReadMember.of(
                        status.memberId(),
                        status.name(),
                        status.profileImageUrl()
                ))
                .toList();

        List<UnreadMember> unreadMembers = statuses.stream()
                .filter(status -> !status.isRead())
                .map(status -> UnreadMember.of(
                        status.memberId(),
                        status.name(),
                        status.profileImageUrl(),
                        status.lastRemindAt()
                ))
                .toList();

        return NoticeStatusesResponse.of(
                noticeId,
                notice.getNextRemindAt(),
                readMembers,
                unreadMembers
        );
    }

    @Transactional
    public NoticeReadResponse markAsRead(Long userId, Long studyId, Long noticeId) {
        StudyMember member = studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, userId);
        Notice notice = noticeRepository.getByIdOrThrow(noticeId);
        validateNoticeBelongsToStudy(studyId, notice);

        NoticeRecipient recipient = noticeRecipientRepository.getByNoticeIdAndMemberIdOrThrow(noticeId, member.getId());
        recipient.markAsRead(clock);

        return NoticeReadResponse.from(recipient);
    }

    public NoticeReadStatusResponse getReadStatus(Long userId, Long studyId, Long noticeId) {
        StudyMember member = studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, userId);
        Notice notice = noticeRepository.getByIdOrThrow(noticeId);
        validateNoticeBelongsToStudy(studyId, notice);

        NoticeRecipient recipient = noticeRecipientRepository.getByNoticeIdAndMemberIdOrThrow(noticeId, member.getId());
        return NoticeReadStatusResponse.from(recipient);
    }

    private List<NoticeSummaryResponse> createNoticeSummaries(StudyMember member, List<Notice> notices) {
        if (member.isLeader()) {
            return notices.stream().map(NoticeSummaryResponse::forLeader).toList();
        }

        if (notices.isEmpty()) {
            return List.of();
        }

        List<Long> noticeIds = notices.stream().map(Notice::getId).toList();

        Map<Long, Boolean> readStatusByNoticeId = noticeRecipientRepository.findReadStatusesByNoticeIdsAndMemberId(
                        noticeIds, member.getId()).stream()
                .collect(Collectors.toMap(NoticeReadStatusProjection::noticeId, NoticeReadStatusProjection::isRead));

        return notices.stream().map(notice -> NoticeSummaryResponse.forMember(notice,
                getReadStatus(readStatusByNoticeId, notice.getId()))).toList();
    }

    public NoticeDetailResponse getDetail(Long userId, Long studyId, Long noticeId) {
        studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, userId);

        Notice notice = noticeRepository.getByIdOrThrow(noticeId);
        validateNoticeBelongsToStudy(studyId, notice);

        return NoticeDetailResponse.from(notice);
    }

    private boolean getReadStatus(Map<Long, Boolean> readStatusByNoticeId, Long noticeId) {
        Boolean isRead = readStatusByNoticeId.get(noticeId);
        if (isRead == null) {
            throw new NoticeException(NoticeErrorCode.NOTICE_RECIPIENT_NOT_FOUND);
        }
        return isRead;
    }

    private void validateLeader(Long studyId, Long userId) {
        StudyMember member = studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, userId);
        if (!member.isLeader()) {
            throw new AuthException(AuthErrorCode.ACCESS_DENIED);
        }
    }

    private void validateNoticeBelongsToStudy(Long studyId, Notice notice) {
        if (!Objects.equals(notice.getStudy().getId(), studyId)) {
            throw new NoticeException(NoticeErrorCode.NOTICE_NOT_FOUND);
        }
    }
}

package withoutc.chongchong.notice.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import withoutc.chongchong.global.pagination.CursorPageRequest;
import withoutc.chongchong.global.pagination.CursorPageResponse;
import withoutc.chongchong.notice.controller.dto.NoticeCreateRequest;
import withoutc.chongchong.notice.controller.dto.NoticeCreateResponse;
import withoutc.chongchong.notice.controller.dto.NoticeDetailResponse;
import withoutc.chongchong.notice.controller.dto.NoticeListResponse;
import withoutc.chongchong.notice.controller.dto.NoticeSummaryResponse;
import withoutc.chongchong.notice.controller.dto.NoticeUpdateRequest;
import withoutc.chongchong.notice.entity.Notice;
import withoutc.chongchong.notice.entity.NoticeReadStatus;
import withoutc.chongchong.notice.policy.NoticeAccessPolicy;
import withoutc.chongchong.notice.repository.NoticeRecipientRepository;
import withoutc.chongchong.notice.repository.NoticeRepository;
import withoutc.chongchong.notice.repository.projection.NoticeReadStatusProjection;
import withoutc.chongchong.notification.entity.ResourceType;
import withoutc.chongchong.notification.service.NotificationService;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.entity.StudyMember;
import withoutc.chongchong.study.repository.StudyMemberRepository;
import withoutc.chongchong.study.repository.StudyRepository;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class NoticeService {
    private final StudyMemberRepository studyMemberRepository;
    private final NoticeRepository noticeRepository;
    private final NoticeRecipientRepository noticeRecipientRepository;
    private final StudyRepository studyRepository;
    private final NotificationService notificationService;

    private final NoticeAccessPolicy noticeAccessPolicy;
    private final Clock clock;

    @Transactional
    public NoticeCreateResponse create(Long userId, Long studyId, NoticeCreateRequest request) {
        StudyMember actor = studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, userId);
        noticeAccessPolicy.requireCanCreateNotice(actor);

        List<StudyMember> members = studyMemberRepository.findAllByStudyId(studyId).stream()
                .filter(studyMember -> !studyMember.isLeader()).toList();

        Study study = studyRepository.getByIdOrThrow(studyId);

        Notice notice = Notice.create(study, request.title(), request.content());
        LocalDateTime now = LocalDateTime.now(clock);
        notice.addReminders(request.remindAts(), now);
        notice.addRecipients(members);

        noticeRepository.save(notice);
        notificationService.createNoticeCreatedEventNotifications(notice, members);

        return NoticeCreateResponse.from(notice);
    }

    @Transactional
    public void delete(Long userId, Long studyId, Long noticeId) {
        StudyMember actor = studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, userId);
        noticeAccessPolicy.requireCanDeleteNotice(actor);

        Notice notice = noticeRepository.getByIdAndStudyIdOrThrow(noticeId, studyId);

        notificationService.deleteNotificationsForResource(ResourceType.NOTICE, noticeId);
        noticeRepository.delete(notice);
    }

    @Transactional
    public void update(Long userId, Long studyId, Long noticeId, NoticeUpdateRequest request) {
        StudyMember actor = studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, userId);
        noticeAccessPolicy.requireCanUpdateNotice(actor);

        Notice notice = noticeRepository.getByIdAndStudyIdOrThrow(noticeId, studyId);

        LocalDateTime now = LocalDateTime.now(clock);
        notice.update(request.title(), request.content(), request.remindAts(), now);
        noticeRepository.save(notice);
    }

    public NoticeDetailResponse getDetail(Long userId, Long studyId, Long noticeId) {
        studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, userId);

        Notice notice = noticeRepository.getByIdAndStudyIdOrThrow(noticeId, studyId);

        return NoticeDetailResponse.from(notice);
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

    private List<NoticeSummaryResponse> createNoticeSummaries(StudyMember member, List<Notice> notices) {
        if (notices.isEmpty()) {
            return List.of();
        }

        if (member.isLeader()) {
            return notices.stream().map(NoticeSummaryResponse::forLeader).toList();
        }

        List<Long> noticeIds = notices.stream().map(Notice::getId).toList();

        Map<Long, NoticeReadStatus> readStatusByNoticeId = noticeRecipientRepository.findMyReadStatusesByNoticeIdsAndMemberId(
                noticeIds, member.getId()).stream().collect(
                Collectors.toMap(NoticeReadStatusProjection::noticeId, NoticeReadStatusProjection::readStatus));

        return notices.stream().map(notice -> NoticeSummaryResponse.forMember(notice,
                readStatusByNoticeId.getOrDefault(notice.getId(), NoticeReadStatus.NOT_ASSIGNED))).toList();
    }

}

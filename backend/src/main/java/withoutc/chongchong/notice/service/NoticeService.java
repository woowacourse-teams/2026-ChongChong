package withoutc.chongchong.notice.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import withoutc.chongchong.comment.repository.CommentRepository;
import withoutc.chongchong.file.entity.FileMetadata;
import withoutc.chongchong.file.service.FileUrlGenerator;
import withoutc.chongchong.global.pagination.CursorPageRequest;
import withoutc.chongchong.global.pagination.CursorPageResponse;
import withoutc.chongchong.notice.controller.dto.NoticeAttachmentResponse;
import withoutc.chongchong.notice.controller.dto.NoticeCreateRequest;
import withoutc.chongchong.notice.controller.dto.NoticeCreateResponse;
import withoutc.chongchong.notice.controller.dto.NoticeDetailResponse;
import withoutc.chongchong.notice.controller.dto.NoticeListResponse;
import withoutc.chongchong.notice.controller.dto.NoticeSummaryResponse;
import withoutc.chongchong.notice.controller.dto.NoticeUpdateRequest;
import withoutc.chongchong.notice.entity.Notice;
import withoutc.chongchong.notice.entity.NoticeAttachment;
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
import withoutc.chongchong.study.repository.projection.StudyMemberIdentityProjection;
import withoutc.chongchong.user.repository.UserRepository;
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
    private final NoticeAttachmentService noticeAttachmentService;
    private final FileUrlGenerator fileUrlGenerator;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;

    private final NoticeAccessPolicy noticeAccessPolicy;
    private final Clock clock;

    @Transactional
    public NoticeCreateResponse create(Long userId, Long studyId, NoticeCreateRequest request) {
        studyMemberRepository.requireExistsByStudyIdAndUserId(studyId, userId);
        List<StudyMemberIdentityProjection> candidates = studyMemberRepository.findIdentitiesByStudyId(studyId);
        userRepository.findAllByIdInForShare(Stream.concat(Stream.of(userId), candidates.stream()
                .map(StudyMemberIdentityProjection::userId)).distinct().sorted().toList());
        StudyMember actor = studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, userId);
        noticeAccessPolicy.requireCanCreateNotice(actor);
        Study study = studyRepository.getByIdForShareOrThrow(studyId);

        List<FileMetadata> attachmentFiles = noticeAttachmentService.prepareForCreate(
                userId, request.attachmentFileIds()
        );

        List<StudyMember> members = studyMemberRepository.findAllByStudyId(studyId).stream()
                .filter(member -> candidates.stream().anyMatch(candidate -> candidate.matches(member)))
                .filter(studyMember -> !studyMember.isLeader()).toList();

        Notice notice = Notice.create(study, request.title(), request.content());
        LocalDateTime now = LocalDateTime.now(clock);
        notice.addReminders(request.remindAts(), now);
        notice.addRecipients(members);
        notice.addAttachments(attachmentFiles);

        noticeRepository.save(notice);
        notificationService.createNoticeCreatedEventNotifications(notice, members);

        return NoticeCreateResponse.from(notice);
    }

    @Transactional
    public void delete(Long userId, Long studyId, Long noticeId) {
        studyMemberRepository.requireExistsByStudyIdAndUserId(studyId, userId);
        StudyMember actor = studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, userId);
        noticeAccessPolicy.requireCanDeleteNotice(actor);

        Notice notice = noticeRepository.getByIdAndStudyIdForUpdateOrThrow(noticeId, studyId);
        noticeAttachmentService.prepareForDelete(notice);

        commentRepository.deleteAllByResourceTypeAndResourceId(ResourceType.NOTICE, noticeId);
        notificationService.deleteNotificationsForResource(ResourceType.NOTICE, noticeId);
        noticeRepository.delete(notice);
    }

    @Transactional
    public void update(Long userId, Long studyId, Long noticeId, NoticeUpdateRequest request) {
        studyMemberRepository.requireExistsByStudyIdAndUserId(studyId, userId);
        StudyMember actor = studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, userId);
        noticeAccessPolicy.requireCanUpdateNotice(actor);

        Notice notice = noticeRepository.getByIdAndStudyIdForUpdateOrThrow(noticeId, studyId);

        LocalDateTime now = LocalDateTime.now(clock);
        notice.update(request.title(), request.content(), request.remindAts(), now);
        replaceAttachments(userId, notice, request.attachmentFileIds());
        noticeRepository.save(notice);
    }

    private void replaceAttachments(Long userId, Notice notice, List<Long> fileIds) {
        if (fileIds == null) {
            return;
        }
        List<FileMetadata> files = noticeAttachmentService.prepareForUpdate(userId, notice, fileIds);
        notice.clearAttachments();
        noticeRepository.flush();
        notice.addAttachments(files);
    }

    public NoticeDetailResponse getDetail(Long userId, Long studyId, Long noticeId) {
        studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, userId);

        Notice notice = noticeRepository.getByIdAndStudyIdOrThrow(noticeId, studyId);

        List<NoticeAttachmentResponse> attachments = notice.getAttachments().stream()
                .map(this::toAttachmentResponse)
                .toList();
        return NoticeDetailResponse.from(notice, attachments);
    }

    private NoticeAttachmentResponse toAttachmentResponse(NoticeAttachment attachment) {
        FileMetadata file = attachment.getFile();
        String imageUrl = fileUrlGenerator.generateImageUrl(file.getObjectKey(), file.getContentType());
        return NoticeAttachmentResponse.from(attachment, imageUrl);
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

package withoutc.chongchong.notice.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import withoutc.chongchong.notice.controller.dto.NoticeReadResponse;
import withoutc.chongchong.notice.controller.dto.NoticeReadStatusResponse;
import withoutc.chongchong.notice.controller.dto.NoticeReadStatusesResponse;
import withoutc.chongchong.notice.controller.dto.NoticeReadStatusesResponse.ReadMember;
import withoutc.chongchong.notice.controller.dto.NoticeReadStatusesResponse.UnreadMember;
import withoutc.chongchong.notice.entity.Notice;
import withoutc.chongchong.notice.entity.NoticeRecipient;
import withoutc.chongchong.notice.policy.NoticeAccessPolicy;
import withoutc.chongchong.notice.repository.NoticeRecipientRepository;
import withoutc.chongchong.notice.repository.NoticeRepository;
import withoutc.chongchong.notice.repository.projection.NoticeRecipientStatusProjection;
import withoutc.chongchong.study.entity.StudyMember;
import withoutc.chongchong.study.repository.StudyMemberRepository;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class NoticeReadService {

    private final StudyMemberRepository studyMemberRepository;
    private final NoticeRepository noticeRepository;
    private final NoticeRecipientRepository noticeRecipientRepository;

    private final NoticeAccessPolicy noticeAccessPolicy;
    private final Clock clock;

    public NoticeReadStatusesResponse getAllReadStatuses(Long userId, Long studyId, Long noticeId) {
        StudyMember actor = studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, userId);
        noticeAccessPolicy.requireCanViewReadStatuses(actor);

        Notice notice = noticeRepository.getByIdAndStudyIdOrThrow(noticeId, studyId);

        List<NoticeRecipientStatusProjection> statuses = noticeRecipientRepository.findAllReadStatusesByNoticeId(
                noticeId);

        return NoticeReadStatusesResponse.of(noticeId, notice.getNextRemindAt(),
                toReadMembers(statuses), toUnreadMembers(statuses));
    }

    private List<ReadMember> toReadMembers(List<NoticeRecipientStatusProjection> statuses) {
        return statuses.stream()
                .filter(NoticeRecipientStatusProjection::isRead)
                .map(status -> ReadMember.of(status.memberId(), status.name(), status.profileImageUrl(), status.readAt()))
                .toList();
    }

    private List<UnreadMember> toUnreadMembers(List<NoticeRecipientStatusProjection> statuses) {
        return statuses.stream()
                .filter(status -> !status.isRead())
                .map(status -> UnreadMember.of(status.memberId(), status.name(), status.profileImageUrl(),
                        status.lastRemindAt()))
                .toList();
    }

    @Transactional
    public NoticeReadResponse markAsRead(Long userId, Long studyId, Long noticeId) {
        StudyMember member = studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, userId);
        noticeRepository.getByIdAndStudyIdOrThrow(noticeId, studyId);

        NoticeRecipient recipient = noticeRecipientRepository.getByNoticeIdAndMemberIdOrThrow(noticeId, member.getId());
        LocalDateTime now = LocalDateTime.now(clock);
        recipient.markAsRead(now);

        return NoticeReadResponse.from(recipient);
    }

    public NoticeReadStatusResponse getMyReadStatus(Long userId, Long studyId, Long noticeId) {
        StudyMember member = studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, userId);
        noticeRepository.getByIdAndStudyIdOrThrow(noticeId, studyId);

        Optional<NoticeRecipient> recipient = noticeRecipientRepository.findByNoticeIdAndMemberId(noticeId,
                member.getId());

        return recipient.map(NoticeReadStatusResponse::from)
                .orElseGet(NoticeReadStatusResponse::notAssigned);
    }
}

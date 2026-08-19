package withoutc.chongchong.notice.service;

import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import withoutc.chongchong.notice.dto.NoticeCreateRequest;
import withoutc.chongchong.notice.dto.NoticeCreateResponse;
import withoutc.chongchong.notice.dto.NoticeDetailResponse;
import withoutc.chongchong.notice.dto.NoticeListResponse;
import withoutc.chongchong.notice.dto.NoticeUpdateRequest;
import withoutc.chongchong.notice.entity.Notice;
import withoutc.chongchong.notice.entity.NoticeRecipient;
import withoutc.chongchong.notice.exception.NoticeErrorCode;
import withoutc.chongchong.notice.exception.NoticeException;
import withoutc.chongchong.notice.repository.NoticeRecipientRepository;
import withoutc.chongchong.notice.repository.NoticeRepository;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.entity.StudyMember;
import withoutc.chongchong.study.repository.StudyMemberRepository;
import withoutc.chongchong.study.repository.StudyRepository;
import withoutc.chongchong.user.entity.User;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class NoticeService {
    private final StudyRepository studyRepository;
    private final StudyMemberRepository studyMemberRepository;
    private final NoticeRepository noticeRepository;
    private final NoticeRecipientRepository noticeRecipientRepository;

    @Transactional
    public NoticeCreateResponse create(User user, Long studyId, NoticeCreateRequest request) {
        Study study = studyRepository.getByIdOrThrow(studyId);

        StudyMember leader = studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, user.getId());
        validateLeader(leader);

        Notice notice = Notice.create(study, leader, request.title(), request.content());
        notice = noticeRepository.save(notice);

        List<StudyMember> members = studyMemberRepository.findAllByStudyId(studyId);
        createNoticeRecipients(notice, members);

        return new NoticeCreateResponse(notice.getId());
    }

    @Transactional
    public void delete(User user, Long studyId, Long noticeId) {
        StudyMember leader = studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, user.getId());
        validateLeader(leader);

        Notice notice = noticeRepository.getByIdOrThrow(noticeId);
        validateNoticeBelongsToStudy(studyId, notice);

        noticeRepository.delete(notice);
        noticeRecipientRepository.deleteAllByNoticeId(noticeId);
    }

    @Transactional
    public void update(User user, Long studyId, Long noticeId, NoticeUpdateRequest request) {
        StudyMember leader = studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, user.getId());
        validateLeader(leader);

        Notice notice = noticeRepository.getByIdOrThrow(noticeId);
        validateNoticeBelongsToStudy(studyId, notice);

        notice.update(request.title(), request.content());
        noticeRepository.save(notice);
    }

    public NoticeListResponse list(User user, Long studyId, Long cursor, int size) {
        StudyMember member = studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, user.getId());

        Pageable pageable = PageRequest.of(0, size + 1);
        List<Notice> notices = noticeRepository.findByCursor(studyId, cursor, pageable);

        if (member.isLeader()) {
            // 리더 전용 dto 변환 로직
        } else {
            // 멤버 전용 dto 변환 로직
        }
        return null;
    }

    public NoticeDetailResponse detail(User user, Long studyId, Long noticeId) {
        studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, user.getId());

        Notice notice = noticeRepository.getByIdOrThrow(noticeId);
        validateNoticeBelongsToStudy(studyId, notice);

        return NoticeDetailResponse.from(notice);
    }

    private void validateLeader(StudyMember member) {
        if (!member.isLeader()) {
            // TODO throw 403
        }
    }

    private void validateNoticeBelongsToStudy(Long studyId, Notice notice) {
        if (!Objects.equals(notice.getStudy().getId(), studyId)) {
            throw new NoticeException(NoticeErrorCode.NOTICE_NOT_FOUND);
        }
    }

    private void createNoticeRecipients(Notice notice, List<StudyMember> members) {
        List<NoticeRecipient> noticeRecipients = members.stream()
                .map(member -> NoticeRecipient.create(member, notice))
                .toList();

        noticeRecipientRepository.saveAll(noticeRecipients);
    }
}

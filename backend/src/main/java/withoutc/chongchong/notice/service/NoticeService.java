package withoutc.chongchong.notice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import withoutc.chongchong.notice.dto.NoticeCreateRequest;
import withoutc.chongchong.notice.dto.NoticeCreateResponse;
import withoutc.chongchong.notice.dto.NoticeUpdateRequest;
import withoutc.chongchong.notice.entity.Notice;
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

    @Transactional
    public NoticeCreateResponse create(User user, Long studyId, NoticeCreateRequest request) {
        Study study = studyRepository.getByIdOrThrow(studyId);
        StudyMember writer = studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, user.getId());

        Notice notice = Notice.create(study, writer, request.title(), request.content());
        noticeRepository.save(notice);

        return new NoticeCreateResponse(notice.getId());
    }

    @Transactional
    public void delete(User user, Long studyId, Long noticeId) {
        Notice notice = noticeRepository.getByIdOrThrow(noticeId);

        StudyMember member = studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, user.getId());
        validateAdmin(member);

        noticeRepository.delete(notice);
    }

    @Transactional
    public void update(User user, Long studyId, Long noticeId, NoticeUpdateRequest request) {
        Notice notice = noticeRepository.getByIdOrThrow(noticeId);

        StudyMember member = studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, user.getId());
        validateAdmin(member);

        notice.update(request.title(), request.content());
        noticeRepository.save(notice);
    }

    private void validateAdmin(StudyMember member) {
        if (!member.isAdmin()) {
            // TODO throw 403
        }
    }

}

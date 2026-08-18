package withoutc.chongchong.notice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import withoutc.chongchong.notice.dto.NoticeCreateRequest;
import withoutc.chongchong.notice.dto.NoticeCreateResponse;
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
// TODO : 스터디 리드만 공지 생성-수정-삭제 가능하도록 권한 로직 추가 필요
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
        if (!member.isAdmin()) {
            // TODO throw 403
        }

        noticeRepository.delete(notice);
    }

}

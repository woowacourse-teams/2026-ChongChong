package withoutc.chongchong.notice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import withoutc.chongchong.notice.dto.NoticeCreateRequest;
import withoutc.chongchong.notice.dto.NoticeCreateResponse;
import withoutc.chongchong.notice.dto.NoticeDetailResponse;
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

        StudyMember leader = studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, user.getId());
        validateLeader(leader);

        Notice notice = Notice.create(study, leader, request.title(), request.content());
        noticeRepository.save(notice);

        return new NoticeCreateResponse(notice.getId());
    }

    @Transactional
    public void delete(User user, Long studyId, Long noticeId) {
        // TODO 이 스터디의 공지가 맞는지 검증 필요
        StudyMember leader = studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, user.getId());
        validateLeader(leader);

        Notice notice = noticeRepository.getByIdOrThrow(noticeId);

        noticeRepository.delete(notice);
    }

    @Transactional
    public void update(User user, Long studyId, Long noticeId, NoticeUpdateRequest request) {
        // TODO 이 스터디의 공지가 맞는지 검증 필요
        StudyMember leader = studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, user.getId());
        validateLeader(leader);

        Notice notice = noticeRepository.getByIdOrThrow(noticeId);

        notice.update(request.title(), request.content());
        noticeRepository.save(notice);
    }

    public NoticeDetailResponse detail(User user, Long studyId, Long noticeId) {
        // TODO 이 스터디의 공지가 맞는지 검증 필요
        studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, user.getId());

        Notice notice = noticeRepository.getByIdOrThrow(noticeId);
        return NoticeDetailResponse.from(notice);
    }

    private void validateLeader(StudyMember member) {
        if (!member.isLeader()) {
            // TODO throw 403
        }
    }
}

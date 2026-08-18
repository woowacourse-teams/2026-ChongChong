package withoutc.chongchong.study.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import withoutc.chongchong.study.dto.StudyCreateRequest;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.exception.StudyErrorCode;
import withoutc.chongchong.study.exception.StudyException;
import withoutc.chongchong.study.repository.StudyRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudyService {

    private final StudyRepository studyRepository;
    private final StudyInviteLinkGenerator studyInviteLinkGenerator;

    // TODO: User, StudyMember 구현 후 User Id로 StudyMember(LEADER) 생성
    @Transactional
    public void create(StudyCreateRequest request) {
        Study study = request.toStudy();

        studyRepository.save(study);
    }

    // TODO: 인증, 인가 구현 후 해당 스터디에 속해 있는지 확인
    public String getInviteLink(Long studyId) {
        studyRepository.findById(studyId)
                .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_NOT_FOUND));

        return studyInviteLinkGenerator.generate(studyId);
    }
}

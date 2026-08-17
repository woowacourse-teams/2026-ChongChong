package withoutc.chongchong.study.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import withoutc.chongchong.study.dto.StudyCreateRequest;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.repository.StudyRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudyService {

    private final StudyRepository studyRepository;

    // TODO: User, StudyMember 구현 후 User Id로 StudyMember(LEADER) 생성
    @Transactional
    public void create(StudyCreateRequest request) {
        Study study = request.toStudy();

        studyRepository.save(study);
    }
}

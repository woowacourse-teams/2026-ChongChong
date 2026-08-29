package withoutc.chongchong.study.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.exception.StudyErrorCode;
import withoutc.chongchong.study.exception.StudyException;

@Repository
public interface StudyRepository extends JpaRepository<Study, Long> {
    default Study getByIdOrThrow(Long id) {
        return findById(id).orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_NOT_FOUND));
    }
}

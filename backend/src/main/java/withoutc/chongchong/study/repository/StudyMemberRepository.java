package withoutc.chongchong.study.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import withoutc.chongchong.study.entity.StudyMember;
import withoutc.chongchong.study.exception.StudyMemberErrorCode;
import withoutc.chongchong.study.exception.StudyMemberException;

@Repository
public interface StudyMemberRepository extends JpaRepository<StudyMember, Long> {
    Optional<StudyMember> findByStudyIdAndUserId(Long studyId, Long userId);

    List<StudyMember> findAllByStudyId(Long studyId);

    default StudyMember getByStudyIdAndUserIdOrThrow(Long studyId, Long userId) {
        return findByStudyIdAndUserId(studyId, userId)
                .orElseThrow(() -> new StudyMemberException(StudyMemberErrorCode.STUDY_ACCESS_DENIED));
    }
}

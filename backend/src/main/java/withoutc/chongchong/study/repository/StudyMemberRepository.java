package withoutc.chongchong.study.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import withoutc.chongchong.study.entity.StudyMember;
import withoutc.chongchong.study.exception.StudyMemberErrorCode;
import withoutc.chongchong.study.exception.StudyMemberException;

@Repository
public interface StudyMemberRepository extends JpaRepository<StudyMember, Long> {
    default StudyMember findByIdOrThrow(Long id) {
        return findById(id).orElseThrow(() -> new StudyMemberException(StudyMemberErrorCode.STUDY_MEMBER_NOT_FOUND));
    }
}

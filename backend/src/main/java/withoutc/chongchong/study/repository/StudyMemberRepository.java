package withoutc.chongchong.study.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import withoutc.chongchong.study.entity.StudyMember;

@Repository
public interface StudyMemberRepository extends JpaRepository<StudyMember, Long> {

    Optional<StudyMember> findByStudyIdAndUserId(Long studyId, Long userId);

    @EntityGraph(attributePaths = "study")
    List<StudyMember> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    int countByUserId(Long userId);

    int countByStudyId(Long studyId);

    void deleteAllByStudyId(Long studyId);
}

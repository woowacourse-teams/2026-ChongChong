package withoutc.chongchong.study.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import withoutc.chongchong.study.entity.StudyMember;

@Repository
public interface StudyMemberRepository extends JpaRepository<StudyMember, Long> {
}

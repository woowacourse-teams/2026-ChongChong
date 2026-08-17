package withoutc.chongchong.study.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import withoutc.chongchong.study.entity.Study;

@Repository
public interface StudyRepository extends JpaRepository<Study, Long> {
}

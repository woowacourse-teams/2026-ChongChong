package withoutc.chongchong.notice.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import withoutc.chongchong.notice.entity.Notice;

@Repository
public interface NoticeRepository extends JpaRepository<Notice, Long> {

    List<Notice> findAllByStudyId(Long studyId);

    void deleteAllByStudyId(Long studyId);
}

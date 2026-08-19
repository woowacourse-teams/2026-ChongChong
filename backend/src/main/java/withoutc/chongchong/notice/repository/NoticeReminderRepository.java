package withoutc.chongchong.notice.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import withoutc.chongchong.notice.entity.NoticeReminder;

public interface NoticeReminderRepository extends JpaRepository<NoticeReminder, Long> {
    List<NoticeReminder> findAllByNoticeId(Long noticeId);
}

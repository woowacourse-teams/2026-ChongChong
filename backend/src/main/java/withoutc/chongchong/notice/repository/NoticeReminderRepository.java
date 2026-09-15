package withoutc.chongchong.notice.repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import withoutc.chongchong.notice.entity.NoticeReminder;
import withoutc.chongchong.notice.entity.NoticeReminderStatus;

public interface NoticeReminderRepository extends JpaRepository<NoticeReminder, Long> {

    List<NoticeReminder> findAllByStatusAndRemindAtLessThanEqual(NoticeReminderStatus status, LocalDateTime now);
}

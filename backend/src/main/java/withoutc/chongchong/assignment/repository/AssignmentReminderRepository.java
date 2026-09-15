package withoutc.chongchong.assignment.repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import withoutc.chongchong.assignment.entity.AssignmentReminder;
import withoutc.chongchong.assignment.entity.AssignmentReminderStatus;

public interface AssignmentReminderRepository extends JpaRepository<AssignmentReminder, Long> {

    List<AssignmentReminder> findAllByStatusAndRemindAtLessThanEqual(AssignmentReminderStatus status,
                                                                     LocalDateTime now);
}

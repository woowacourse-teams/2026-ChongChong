package withoutc.chongchong.assignment.repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import withoutc.chongchong.assignment.entity.AssignmentReminder;

public interface AssignmentReminderRepository extends JpaRepository<AssignmentReminder, Long> {

    @Query(value = """
            SELECT *
            FROM assignment_reminders
            WHERE status = 'PENDING'
              AND remind_at <= :now
            ORDER BY id
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<AssignmentReminder> findClaimableForUpdate(
            @Param("now") LocalDateTime now,
            @Param("batchSize") int batchSize
    );
}

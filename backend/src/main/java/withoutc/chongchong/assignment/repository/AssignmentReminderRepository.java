package withoutc.chongchong.assignment.repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import withoutc.chongchong.assignment.entity.AssignmentReminder;
import withoutc.chongchong.notification.repository.projection.ReminderCandidateProjection;

public interface AssignmentReminderRepository extends JpaRepository<AssignmentReminder, Long> {

    @Query("""
            SELECT new withoutc.chongchong.notification.repository.projection.ReminderCandidateProjection(
                withoutc.chongchong.notification.entity.ResourceType.ASSIGNMENT,
                reminder.id, reminder.assignment.id, reminder.assignment.study.id)
            FROM AssignmentReminder reminder
            WHERE reminder.status = withoutc.chongchong.assignment.entity.AssignmentReminderStatus.PENDING
              AND reminder.remindAt <= :now
            ORDER BY reminder.id
            """)
    List<ReminderCandidateProjection> findDueCandidates(@Param("now") LocalDateTime now, Pageable pageable);

    @Query(value = """
            SELECT * FROM assignment_reminders
            WHERE id = :reminderId AND status = 'PENDING' AND remind_at <= :now
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<AssignmentReminder> findClaimableByIdForUpdate(@Param("reminderId") Long reminderId,
                                                      @Param("now") LocalDateTime now);

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

    @Query(value = """
            SELECT *
            FROM assignment_reminders
            WHERE assignment_id = :assignmentId
            ORDER BY id
            FOR UPDATE
            """, nativeQuery = true)
    List<AssignmentReminder> findAllByAssignmentIdForUpdate(@Param("assignmentId") Long assignmentId);

    @Query(value = """
            SELECT *
            FROM assignment_reminders
            WHERE assignment_id IN (
                SELECT id
                FROM assignments
                WHERE study_id = :studyId
            )
            ORDER BY id
            FOR UPDATE
            """, nativeQuery = true)
    List<AssignmentReminder> findAllByStudyIdForUpdate(@Param("studyId") Long studyId);
}

package withoutc.chongchong.assignment.repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import withoutc.chongchong.assignment.entity.AssignmentRecurrence;
import withoutc.chongchong.assignment.exception.AssignmentErrorCode;
import withoutc.chongchong.assignment.exception.AssignmentException;
import withoutc.chongchong.assignment.repository.projection.AssignmentRecurrenceCandidate;

public interface AssignmentRecurrenceRepository extends JpaRepository<AssignmentRecurrence, Long> {

    @Query("""
            SELECT new withoutc.chongchong.assignment.repository.projection.AssignmentRecurrenceCandidate(
                recurrence.id, recurrence.study.id
            )
            FROM AssignmentRecurrence recurrence
            WHERE recurrence.schedule.nextPublishAt <= :now
            ORDER BY recurrence.schedule.nextPublishAt, recurrence.id
            """)
    List<AssignmentRecurrenceCandidate> findDueCandidates(@Param("now") LocalDateTime now, Pageable pageable);

    @Query(value = """
            SELECT * FROM assignment_recurrences
            WHERE id = :id AND next_publish_at <= :now
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<AssignmentRecurrence> findDueByIdForUpdate(@Param("id") Long id, @Param("now") LocalDateTime now);

    @EntityGraph(attributePaths = {"remindOffsets", "submitterIds"})
    List<AssignmentRecurrence> findAllByStudyIdOrderByIdDesc(Long studyId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT recurrence FROM AssignmentRecurrence recurrence
            WHERE recurrence.id = :id AND recurrence.study.id = :studyId
            """)
    Optional<AssignmentRecurrence> findByIdAndStudyIdForUpdate(@Param("id") Long id, @Param("studyId") Long studyId);

    default AssignmentRecurrence getByIdAndStudyIdForUpdateOrThrow(Long id, Long studyId) {
        return findByIdAndStudyIdForUpdate(id, studyId)
                .orElseThrow(() -> new AssignmentException(AssignmentErrorCode.ASSIGNMENT_RECURRENCE_NOT_FOUND));
    }
}

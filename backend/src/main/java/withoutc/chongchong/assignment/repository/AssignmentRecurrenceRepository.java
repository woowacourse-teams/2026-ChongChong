package withoutc.chongchong.assignment.repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import withoutc.chongchong.assignment.entity.AssignmentRecurrence;
import withoutc.chongchong.assignment.exception.AssignmentErrorCode;
import withoutc.chongchong.assignment.exception.AssignmentException;

public interface AssignmentRecurrenceRepository extends JpaRepository<AssignmentRecurrence, Long> {

    @EntityGraph(attributePaths = {"remindOffsets", "submitterIds"})
    List<AssignmentRecurrence> findAllByStudyIdOrderByIdAsc(Long studyId);

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

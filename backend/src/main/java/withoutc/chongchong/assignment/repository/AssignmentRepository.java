package withoutc.chongchong.assignment.repository;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import withoutc.chongchong.assignment.entity.Assignment;
import withoutc.chongchong.assignment.exception.AssignmentErrorCode;
import withoutc.chongchong.assignment.exception.AssignmentException;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    @Query("""
            SELECT n
            FROM Assignment n
            WHERE n.study.id = :studyId
              AND (:cursor IS NULL OR n.id < :cursor)
            ORDER BY n.id DESC
            """)
    List<Assignment> findByCursor(
            @Param("studyId") Long studyId,
            @Param("cursor") Long cursor,
            Pageable pageable
    );

    List<Assignment> findAllByStudyId(Long studyId);

    void deleteAllByStudyId(Long studyId);

    default Assignment getByIdOrThrow(Long assignmentId) {
        return findById(assignmentId).orElseThrow(
                () -> new AssignmentException(AssignmentErrorCode.ASSIGNMENT_NOT_FOUND));
    }
}

package withoutc.chongchong.assignment.repository;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import withoutc.chongchong.assignment.entity.Assignment;
import withoutc.chongchong.assignment.exception.AssignmentErrorCode;
import withoutc.chongchong.assignment.exception.AssignmentException;
import withoutc.chongchong.assignment.repository.projection.LeaderAssignmentSummaryProjection;

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

    @Query("""
            SELECT a
            FROM Assignment a
            JOIN AssignmentSubmission s ON s.assignment = a
            WHERE a.study.id = :studyId
              AND s.member.id = :memberId
              AND (:cursor IS NULL OR a.id < :cursor)
            ORDER BY a.id DESC
            """)
    List<Assignment> findByCursorAndMemberId(
            @Param("studyId") Long studyId,
            @Param("memberId") Long memberId,
            @Param("cursor") Long cursor,
            Pageable pageable
    );

    List<Assignment> findAllByStudyId(Long studyId);

    @Query("""
            SELECT new withoutc.chongchong.assignment.repository.projection.LeaderAssignmentSummaryProjection(
            a.id,
            a.title,
            SUM(CASE WHEN s.submitted = TRUE THEN 1 ELSE 0 END)
            )
            FROM Assignment a
            JOIN AssignmentSubmission s ON s.assignment.id = a.id
            WHERE a.study.id = :studyId
            GROUP BY a.id, a.title, a.createdAt
            HAVING SUM(CASE WHEN s.submitted = FALSE THEN 1 ELSE 0 END) > 0
            ORDER BY a.createdAt DESC
            """)
    List<LeaderAssignmentSummaryProjection> findIncompleteAssignmentSummariesByStudyId(
            @Param("studyId") Long studyId
    );

    @Query("""
            SELECT a
            FROM Assignment a
            WHERE a.study.id = :studyId
            AND EXISTS (
            SELECT s.id
            FROM AssignmentSubmission s
            WHERE s.assignment = a AND s.member.id = :memberId
            AND s.submitted IS FALSE
            )
            ORDER BY a.createdAt DESC
            """)
    List<Assignment> findIncompleteAssignmentsByStudyIdAndMemberId(
            @Param("studyId") Long studyId,
            @Param("memberId") Long memberId
    );

    void deleteAllByStudyId(Long studyId);

    default Assignment getByIdOrThrow(Long assignmentId) {
        return findById(assignmentId).orElseThrow(
                () -> new AssignmentException(AssignmentErrorCode.ASSIGNMENT_NOT_FOUND));
    }
}

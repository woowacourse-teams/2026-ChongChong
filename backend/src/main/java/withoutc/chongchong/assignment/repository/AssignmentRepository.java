package withoutc.chongchong.assignment.repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import withoutc.chongchong.assignment.entity.Assignment;
import withoutc.chongchong.assignment.exception.AssignmentErrorCode;
import withoutc.chongchong.assignment.exception.AssignmentException;
import withoutc.chongchong.assignment.repository.projection.LeaderAssignmentSummaryProjection;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    Optional<Assignment> findByIdAndStudyId(Long id, Long studyId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT assignment FROM Assignment assignment
            WHERE assignment.id = :id AND assignment.study.id = :studyId
            """)
    Optional<Assignment> findByIdAndStudyIdForUpdate(@Param("id") Long id, @Param("studyId") Long studyId);

    default Assignment getByIdAndStudyIdForUpdateOrThrow(Long id, Long studyId) {
        return findByIdAndStudyIdForUpdate(id, studyId)
                .orElseThrow(() -> new AssignmentException(AssignmentErrorCode.ASSIGNMENT_NOT_FOUND));
    }

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT assignment FROM Assignment assignment
            WHERE assignment.study.id = :studyId
            ORDER BY assignment.id
            """)
    List<Assignment> findAllByStudyIdForUpdate(@Param("studyId") Long studyId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT assignment FROM Assignment assignment WHERE EXISTS (
                SELECT submission.id FROM AssignmentSubmission submission
                WHERE submission.assignment = assignment AND submission.member.id = :memberId
            ) OR EXISTS (
                SELECT comment.id FROM Comment comment
                WHERE comment.authorMember.id = :memberId AND (
                    (comment.resourceType = withoutc.chongchong.notification.entity.ResourceType.ASSIGNMENT
                     AND comment.resourceId = assignment.id)
                    OR (comment.resourceType = withoutc.chongchong.notification.entity.ResourceType.ASSIGNMENT_SUBMISSION
                        AND EXISTS (SELECT submission.id FROM AssignmentSubmission submission
                            WHERE submission.assignment = assignment AND submission.id = comment.resourceId))
                )
            ) ORDER BY assignment.id
            """)
    List<Assignment> findAllAffectedByMemberIdForUpdate(@Param("memberId") Long memberId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT assignment FROM Assignment assignment WHERE EXISTS (
                SELECT submission.id FROM AssignmentSubmission submission
                WHERE submission.assignment = assignment AND submission.member.user.id = :userId
            ) OR EXISTS (
                SELECT comment.id FROM Comment comment
                WHERE comment.authorUser.id = :userId AND (
                    (comment.resourceType = withoutc.chongchong.notification.entity.ResourceType.ASSIGNMENT
                     AND comment.resourceId = assignment.id)
                    OR (comment.resourceType = withoutc.chongchong.notification.entity.ResourceType.ASSIGNMENT_SUBMISSION
                        AND EXISTS (SELECT submission.id FROM AssignmentSubmission submission
                            WHERE submission.assignment = assignment AND submission.id = comment.resourceId))
                )
            ) ORDER BY assignment.id
            """)
    List<Assignment> findAllAffectedByUserIdForUpdate(@Param("userId") Long userId);

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

    // 리더용
    @Query("""
            SELECT new withoutc.chongchong.assignment.repository.projection.LeaderAssignmentSummaryProjection(
            a.id,
            a.title,
            COUNT(s.id),
            SUM(CASE WHEN s.submittedAt IS NOT NULL THEN 1 ELSE 0 END)
            )
            FROM Assignment a
            JOIN AssignmentSubmission s ON s.assignment.id = a.id
            WHERE a.study.id = :studyId
            GROUP BY a.id, a.title, a.createdAt
            HAVING SUM(CASE WHEN s.submittedAt IS NULL THEN 1 ELSE 0 END) > 0
            ORDER BY a.createdAt DESC
            """)
    List<LeaderAssignmentSummaryProjection> findIncompleteAssignmentSummariesByStudyId(
            @Param("studyId") Long studyId
    );

    // 스터디원용
    @Query("""
            SELECT a
            FROM Assignment a
            WHERE a.study.id = :studyId
            AND EXISTS (
            SELECT s.id
            FROM AssignmentSubmission s
            WHERE s.assignment = a AND s.member.id = :memberId
            AND s.submittedAt IS NULL
            )
            ORDER BY a.createdAt DESC
            """)
    List<Assignment> findIncompleteAssignmentsByStudyIdAndMemberId(
            @Param("studyId") Long studyId,
            @Param("memberId") Long memberId
    );

    // 리더용
    @Query("""
            SELECT count(a.id)
            FROM Assignment a
            WHERE a.study.id = :studyId
            AND EXISTS (
            SELECT s.id
            FROM AssignmentSubmission s
            WHERE s.assignment.id = a.id AND s.submittedAt IS NULL
            )
            """)
    long countIncompleteAssignmentByStudyId(
            @Param("studyId") Long studyId
    );

    // 스터디원용
    @Query("""
            SELECT count(a.id)
            FROM Assignment a
            WHERE a.study.id = :studyId
            AND EXISTS (
            SELECT s.id
            FROM AssignmentSubmission s
            WHERE s.assignment.id = a.id AND s.member.id = :memberId AND s.submittedAt IS NULL
            )
            """)
    long countIncompleteAssignmentByStudyIdAndMemberId(
            @Param("studyId") Long studyId,
            @Param("memberId") Long memberId
    );

    default Assignment getByIdAndStudyIdOrThrow(Long id, Long studyId) {
        return findByIdAndStudyId(id, studyId).orElseThrow(
                () -> new AssignmentException(AssignmentErrorCode.ASSIGNMENT_NOT_FOUND));
    }

    default Assignment getByIdOrThrow(Long id) {
        return findById(id).orElseThrow(
                () -> new AssignmentException(AssignmentErrorCode.ASSIGNMENT_NOT_FOUND));
    }
}

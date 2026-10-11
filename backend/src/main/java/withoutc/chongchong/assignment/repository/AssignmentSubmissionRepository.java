package withoutc.chongchong.assignment.repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import withoutc.chongchong.assignment.entity.AssignmentSubmission;
import withoutc.chongchong.assignment.exception.AssignmentErrorCode;
import withoutc.chongchong.assignment.exception.AssignmentException;
import withoutc.chongchong.assignment.repository.projection.AssignmentSubmitterStatusProjection;
import withoutc.chongchong.study.entity.StudyMember;

public interface AssignmentSubmissionRepository extends JpaRepository<AssignmentSubmission, Long> {

    List<AssignmentSubmission> findAllByAssignmentIdInAndMemberId(
            List<Long> assignmentIds,
            Long memberId
    );

    @Query("""
            SELECT new withoutc.chongchong.assignment.repository.projection.AssignmentSubmitterStatusProjection(
                       member.id,
                       member.name,
                       member.profileImageUrl,
                       CASE WHEN submission.submittedAt IS NULL THEN false ELSE true END,
                       submission.submittedAt,
                       MAX(notification.createdAt)
                   )
            FROM AssignmentSubmission submission
            JOIN submission.member member
            LEFT JOIN Notification notification
              ON notification.recipient = member.user
             AND notification.resourceType = withoutc.chongchong.notification.entity.ResourceType.ASSIGNMENT
             AND notification.resourceId = submission.assignment.id
             AND notification.type = withoutc.chongchong.notification.entity.NotificationType.REMIND
            WHERE submission.assignment.id = :assignmentId
            GROUP BY member.id,
                     member.name,
                     member.profileImageUrl,
                     submission.submittedAt
            """)
    List<AssignmentSubmitterStatusProjection> findAllSubmitterStatusesByAssignmentId(
            @Param("assignmentId") Long assignmentId);

    @EntityGraph(attributePaths = "member")
    List<AssignmentSubmission> findAllByAssignmentIdAndSubmittedAtIsNotNull(Long assignmentId);

    Optional<AssignmentSubmission> findByAssignmentIdAndMemberId(Long assignmentId, Long memberId);

    Optional<AssignmentSubmission> findByIdAndAssignmentId(Long id, Long assignmentId);

    default AssignmentSubmission getByIdOrThrow(Long id) {
        return findById(id).orElseThrow(() -> new AssignmentException(
                AssignmentErrorCode.ASSIGNMENT_SUBMISSION_NOT_FOUND));
    }

    default AssignmentSubmission getByIdAndAssignmentIdOrThrow(Long id, Long assignmentId) {
        return findByIdAndAssignmentId(id, assignmentId).orElseThrow(() -> new AssignmentException(
                AssignmentErrorCode.ASSIGNMENT_SUBMISSION_NOT_FOUND));
    }

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<AssignmentSubmission> findWithLockByAssignmentIdAndMemberId(
            @Param("assignmentId") Long assignmentId,
            @Param("memberId") Long memberId
    );

    default AssignmentSubmission getWithLockByAssignmentIdAndMemberIdOrThrow(Long assignmentId, Long memberId) {
        return findWithLockByAssignmentIdAndMemberId(assignmentId, memberId).orElseThrow(() -> new AssignmentException(
                AssignmentErrorCode.ASSIGNMENT_SUBMISSION_NOT_FOUND));
    }

    @Query("""
            SELECT DISTINCT submission.member
            FROM AssignmentSubmission submission
            WHERE submission.assignment.id = :assignmentId
              AND submission.submittedAt IS NULL
            """)
    List<StudyMember> findUnsubmittedMembersByAssignmentId(@Param("assignmentId") Long assignmentId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM AssignmentSubmission submission WHERE submission.member.id = :memberId")
    int deleteAllByMemberId(@Param("memberId") Long memberId);
}

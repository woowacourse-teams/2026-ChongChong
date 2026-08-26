package withoutc.chongchong.assignment.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import withoutc.chongchong.assignment.entity.Assignment;
import withoutc.chongchong.assignment.entity.AssignmentSubmission;
import withoutc.chongchong.assignment.exception.AssignmentErrorCode;
import withoutc.chongchong.assignment.exception.AssignmentException;
import withoutc.chongchong.assignment.repository.projection.AssignmentSubmissionStatusProjection;
import withoutc.chongchong.assignment.repository.projection.AssignmentSubmitterStatusProjection;

public interface AssignmentSubmissionRepository extends JpaRepository<AssignmentSubmission, Long> {

    @Query("""
            SELECT new withoutc.chongchong.assignment.repository.projection.AssignmentSubmissionStatusProjection(
                       submission.assignment.id,
                       submission.submitted
                   )
            FROM AssignmentSubmission submission
            WHERE submission.assignment.id IN :assignmentIds
              AND submission.member.id = :memberId
            """)
    List<AssignmentSubmissionStatusProjection> findMySubmissionStatusesByAssignmentIdsAndMemberId(
            @Param("assignmentIds") List<Long> assignmentIds,
            @Param("memberId") Long memberId
    );

    @Query("""
            SELECT new withoutc.chongchong.assignment.repository.projection.AssignmentSubmitterStatusProjection(
                       member.id,
                       member.name,
                       member.profileImageUrl,
                       submission.submitted,
                       MAX(notification.createdAt)
                   )
            FROM AssignmentSubmission submission
            JOIN submission.member member
            LEFT JOIN Notification notification
              ON notification.recipient = member
             AND notification.resourceType = withoutc.chongchong.notification.entity.NotificationResourceType.ASSIGNMENT
             AND notification.resourceId = submission.assignment.id
             AND notification.type = withoutc.chongchong.notification.entity.NotificationType.REMIND
            WHERE submission.assignment.id = :assignmentId
            GROUP BY member.id,
                     member.name,
                     member.profileImageUrl,
                     submission.submitted
            """)
    List<AssignmentSubmitterStatusProjection> findAllSubmitterStatusesByAssignmentId(
            @Param("assignmentId") Long assignmentId);

    List<AssignmentSubmission> findAllByAssignmentId(Long assignmentId);

    Optional<AssignmentSubmission> findByAssignmentIdAndMemberId(Long assignmentId, Long memberId);

    Optional<AssignmentSubmission> findByIdAndMemberId(Long id, Long memberId);

    default AssignmentSubmission getByAssignmentIdAndMemberIdOrThrow(Long assignmentId, Long memberId) {
        return findByAssignmentIdAndMemberId(assignmentId, memberId).orElseThrow(() -> new AssignmentException(
                AssignmentErrorCode.ASSIGNMENT_SUBMISSION_NOT_FOUND));
    }

    default AssignmentSubmission getByIdAndMemberIdOrThrow(Long assignmentId, Long memberId) {
        return findByIdAndMemberId(assignmentId, memberId).orElseThrow(
                () -> new AssignmentException(AssignmentErrorCode.ASSIGNMENT_SUBMISSION_NOT_FOUND));
    }

    default AssignmentSubmission getByIdOrThrow(Long assignmentId) {
        return findById(assignmentId).orElseThrow(
                () -> new AssignmentException(AssignmentErrorCode.ASSIGNMENT_SUBMISSION_NOT_FOUND));
    }
}

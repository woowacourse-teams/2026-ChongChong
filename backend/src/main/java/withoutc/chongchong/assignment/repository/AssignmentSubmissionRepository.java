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

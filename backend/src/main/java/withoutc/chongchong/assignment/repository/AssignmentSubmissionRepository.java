package withoutc.chongchong.assignment.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import withoutc.chongchong.assignment.entity.AssignmentSubmission;
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
}

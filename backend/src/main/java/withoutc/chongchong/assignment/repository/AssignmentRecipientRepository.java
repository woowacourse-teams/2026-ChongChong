package withoutc.chongchong.assignment.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import withoutc.chongchong.assignment.entity.AssignmentRecipient;
import withoutc.chongchong.assignment.repository.projection.AssignmentSubmitStatusProjection;

public interface AssignmentRecipientRepository extends JpaRepository<AssignmentRecipient, Long> {

    @Query("""
            SELECT new withoutc.chongchong.assignment.repository.projection.AssignmentSubmitStatusProjection(
                       recipient.assignment.id,
                       recipient.isSubmit
                   )
            FROM AssignmentRecipient recipient
            WHERE recipient.assignment.id IN :assignmentIds
              AND recipient.member.id = :memberId
            """)
    List<AssignmentSubmitStatusProjection> findSubmitStatusesByAssignmentIdsAndMemberId(
            @Param("assignmentIds") List<Long> assignmentIds,
            @Param("memberId") Long memberId
    );
}

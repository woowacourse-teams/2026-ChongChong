package withoutc.chongchong.comment.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import withoutc.chongchong.comment.entity.Comment;
import withoutc.chongchong.comment.exception.CommentErrorCode;
import withoutc.chongchong.comment.exception.CommentException;
import withoutc.chongchong.comment.repository.projection.CommentSummaryProjection;
import withoutc.chongchong.notification.entity.ResourceType;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    Optional<Comment> findByIdAndResourceTypeAndResourceId(Long id, ResourceType resourceType, Long resourceId);

    default Comment getByIdAndResourceOrThrow(Long id, ResourceType resourceType, Long resourceId) {
        return findByIdAndResourceTypeAndResourceId(id, resourceType, resourceId)
                .orElseThrow(() -> new CommentException(CommentErrorCode.COMMENT_NOT_FOUND));
    }

    @Query("""
            SELECT new withoutc.chongchong.comment.repository.projection.CommentSummaryProjection(
                c.id, authorUser.id, author.id, author.name, author.profileImageUrl,
                c.createdAt, c.content, c.editedAt
            )
            FROM Comment c
            LEFT JOIN c.authorUser authorUser
            LEFT JOIN c.authorMember author
            WHERE c.resourceType = :resourceType AND c.resourceId = :resourceId
              AND (:cursor IS NULL OR c.id > :cursor OR (:inclusive = true AND c.id = :cursor))
            ORDER BY c.id ASC
            """)
    List<CommentSummaryProjection> findByCursor(
            @Param("resourceType") ResourceType resourceType,
            @Param("resourceId") Long resourceId,
            @Param("cursor") Long cursor,
            @Param("inclusive") boolean inclusive,
            Pageable pageable
    );

    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM Comment c WHERE c.resourceType = :resourceType AND c.resourceId = :resourceId")
    void deleteAllByResourceTypeAndResourceId(
            @Param("resourceType") ResourceType resourceType, @Param("resourceId") Long resourceId);

    @Modifying(flushAutomatically = true)
    @Query(value = """
            DELETE FROM comments c WHERE
              (c.resource_type = 'NOTICE' AND EXISTS
                (SELECT 1 FROM notices n WHERE n.id = c.resource_id AND n.study_id = :studyId))
              OR (c.resource_type = 'ASSIGNMENT' AND EXISTS
                (SELECT 1 FROM assignments a WHERE a.id = c.resource_id AND a.study_id = :studyId))
              OR (c.resource_type = 'ASSIGNMENT_SUBMISSION' AND EXISTS
                (SELECT 1 FROM assignment_submissions s JOIN assignments a ON a.id = s.assignment_id
                 WHERE s.id = c.resource_id AND a.study_id = :studyId))
            """, nativeQuery = true)
    void deleteAllByStudyId(@Param("studyId") Long studyId);

    @Modifying(flushAutomatically = true)
    @Query(value = """
            DELETE FROM comments c WHERE
              (c.resource_type = 'ASSIGNMENT' AND c.resource_id = :assignmentId)
              OR (c.resource_type = 'ASSIGNMENT_SUBMISSION' AND EXISTS
                (SELECT 1 FROM assignment_submissions s
                 WHERE s.id = c.resource_id AND s.assignment_id = :assignmentId))
            """, nativeQuery = true)
    void deleteAllForAssignment(@Param("assignmentId") Long assignmentId);

    @Modifying(flushAutomatically = true)
    @Query(value = """
            DELETE FROM comments c WHERE c.resource_type = 'ASSIGNMENT_SUBMISSION' AND EXISTS
              (SELECT 1 FROM assignment_submissions s
               WHERE s.id = c.resource_id AND s.member_id = :memberId)
            """, nativeQuery = true)
    void deleteAllForMemberSubmissions(@Param("memberId") Long memberId);

    @Modifying(flushAutomatically = true)
    @Query(value = """
            DELETE FROM comments c WHERE c.resource_type = 'ASSIGNMENT_SUBMISSION' AND EXISTS
              (SELECT 1 FROM assignment_submissions s JOIN study_members m ON m.id = s.member_id
               WHERE s.id = c.resource_id AND m.user_id = :userId)
            """, nativeQuery = true)
    void deleteAllForUserSubmissions(@Param("userId") Long userId);

    @Modifying(flushAutomatically = true)
    @Query(value = """
            DELETE FROM comments c WHERE c.resource_type = 'ASSIGNMENT_SUBMISSION' AND EXISTS
              (SELECT 1 FROM assignment_submissions s JOIN study_members m ON m.id = s.member_id
               WHERE s.id = c.resource_id AND s.assignment_id = :assignmentId AND m.role = 'LEADER')
            """, nativeQuery = true)
    void deleteAllForLeaderSubmissions(@Param("assignmentId") Long assignmentId);
}

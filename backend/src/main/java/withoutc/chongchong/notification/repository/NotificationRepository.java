package withoutc.chongchong.notification.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import withoutc.chongchong.notification.entity.Notification;
import withoutc.chongchong.notification.entity.ResourceType;
import withoutc.chongchong.notification.exception.NotificationErrorCode;
import withoutc.chongchong.notification.exception.NotificationException;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM Notification notification WHERE notification.recipient.id = :userId")
    int deleteAllByRecipientId(@Param("userId") Long userId);

    List<Notification> findAllByRecipientIdOrderByCreatedAtDesc(Long userId);

    @Query("""
            SELECT notification
            FROM Notification notification
            WHERE notification.id = :notificationId AND notification.recipient.id = :recipientId
            """)
    Optional<Notification> findByIdAndRecipientId(
            @Param("notificationId") Long notificationId,
            @Param("recipientId") Long recipientId
    );

    default Notification getByIdAndRecipientIdOrElseThrow(Long notificationId, Long recipientId) {
        return findByIdAndRecipientId(notificationId, recipientId).orElseThrow(
                () -> new NotificationException(NotificationErrorCode.NOTIFICATION_NOT_FOUND));
    }

    @Modifying
    @Query(value = """
            DELETE FROM notifications
            WHERE (
                notifications.resource_type = 'NOTICE'
                AND EXISTS (
                    SELECT 1
                    FROM notices
                    WHERE notices.id = notifications.resource_id
                      AND notices.study_id = :studyId
                )
            ) OR (
                notifications.resource_type = 'ASSIGNMENT'
                AND EXISTS (
                    SELECT 1
                    FROM assignments
                    WHERE assignments.id = notifications.resource_id
                      AND assignments.study_id = :studyId
                )
            ) OR (
                notifications.resource_type = 'ASSIGNMENT_SUBMISSION'
                AND EXISTS (
                    SELECT 1
                    FROM assignment_submissions AS submissions
                    JOIN assignments ON assignments.id = submissions.assignment_id
                    WHERE submissions.id = notifications.resource_id
                      AND assignments.study_id = :studyId
                )
            )
            """, nativeQuery = true)
    void deleteAllByStudyId(@Param("studyId") Long studyId);

    @Modifying
    @Query(value = """
            DELETE FROM notifications
            WHERE resource_type = 'ASSIGNMENT_SUBMISSION'
              AND EXISTS (
                  SELECT 1
                  FROM assignment_submissions AS submissions
                  WHERE submissions.id = notifications.resource_id
                    AND submissions.member_id = :memberId
              )
            """, nativeQuery = true)
    void deleteAllByMemberId(@Param("memberId") Long memberId);

    @Modifying
    @Query(value = """
            DELETE FROM notifications
            WHERE resource_type = 'ASSIGNMENT_SUBMISSION'
              AND EXISTS (
                  SELECT 1
                  FROM assignment_submissions AS submissions
                  JOIN study_members AS members ON members.id = submissions.member_id
                  WHERE submissions.id = notifications.resource_id
                    AND members.user_id = :userId
              )
            """, nativeQuery = true)
    void deleteAllByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("""
            DELETE FROM Notification n
            WHERE n.resourceType = :resourceType
              AND n.resourceId = :resourceId
            """)
    void deleteAllByResourceTypeAndResourceId(
            @Param("resourceType") ResourceType resourceType,
            @Param("resourceId") Long resourceId
    );

    @Modifying
    @Query(value = """
            DELETE FROM notifications
            WHERE resource_type = 'ASSIGNMENT_SUBMISSION'
              AND resource_id IN (
                  SELECT id
                  FROM assignment_submissions
                  WHERE assignment_id = :assignmentId
              )
            """, nativeQuery = true)
    void deleteAllForAssignmentSubmissions(@Param("assignmentId") Long assignmentId);
}

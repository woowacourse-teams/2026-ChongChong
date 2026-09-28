package withoutc.chongchong.notification.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import withoutc.chongchong.notification.entity.Notification;
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
}

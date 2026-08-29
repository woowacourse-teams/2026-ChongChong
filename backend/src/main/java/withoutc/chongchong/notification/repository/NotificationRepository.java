package withoutc.chongchong.notification.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import withoutc.chongchong.notification.entity.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM Notification notification WHERE notification.recipient.id = :memberId")
    int deleteAllByRecipientId(@Param("memberId") Long memberId);
}

package withoutc.chongchong.notification.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import withoutc.chongchong.notification.entity.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
}

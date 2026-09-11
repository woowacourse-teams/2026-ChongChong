package withoutc.chongchong.notification.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import withoutc.chongchong.notification.entity.NotificationDelivery;

public interface NotificationDeliveryRepository extends JpaRepository<NotificationDelivery, Long> {
}

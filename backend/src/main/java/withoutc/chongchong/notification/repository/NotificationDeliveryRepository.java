package withoutc.chongchong.notification.repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import withoutc.chongchong.notification.entity.NotificationDelivery;

public interface NotificationDeliveryRepository extends JpaRepository<NotificationDelivery, Long> {

    @Query(value = """
            SELECT *
            FROM notification_deliveries
            WHERE status = 'PENDING'
               OR (
                   status = 'RETRY_WAIT'
                   AND next_retry_at IS NOT NULL
                   AND next_retry_at <= :now
               )
            ORDER BY id
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<NotificationDelivery> findClaimableForUpdate(
            @Param("now") LocalDateTime now,
            @Param("batchSize") int batchSize
    );
}

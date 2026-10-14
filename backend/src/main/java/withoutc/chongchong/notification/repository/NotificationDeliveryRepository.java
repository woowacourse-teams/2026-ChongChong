package withoutc.chongchong.notification.repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import withoutc.chongchong.notification.entity.DeliveryStatus;
import withoutc.chongchong.notification.entity.NotificationDelivery;
import withoutc.chongchong.notification.exception.NotificationErrorCode;
import withoutc.chongchong.notification.exception.NotificationException;
import withoutc.chongchong.notification.repository.projection.ClaimableQueueSnapshotProjection;

public interface NotificationDeliveryRepository extends JpaRepository<NotificationDelivery, Long> {

    @Query("""
            SELECT new withoutc.chongchong.notification.repository.projection.ClaimableQueueSnapshotProjection(
                       COUNT(delivery.id),
                       MIN(CASE WHEN delivery.status = :pendingStatus
                                THEN delivery.createdAt
                                ELSE delivery.nextRetryAt
                           END)
                   )
            FROM NotificationDelivery delivery
            WHERE delivery.status = :pendingStatus
               OR (
                   delivery.status = :retryWaitStatus
                   AND delivery.nextRetryAt IS NOT NULL
                   AND delivery.nextRetryAt <= :now
               )
            """)
    ClaimableQueueSnapshotProjection findClaimableQueueSnapshot(
            @Param("now") LocalDateTime now,
            @Param("pendingStatus") DeliveryStatus pendingStatus,
            @Param("retryWaitStatus") DeliveryStatus retryWaitStatus
    );

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

    @Query(value = """
            SELECT *
            FROM notification_deliveries
            WHERE status = 'PROCESSING'
              AND claimed_at IS NOT NULL
              AND claimed_at <= :expiryTime
            ORDER BY claimed_at
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<NotificationDelivery> findStuckProcessingForUpdate(
            @Param("expiryTime") LocalDateTime expiryTime,
            @Param("batchSize") int batchSize
    );

    default NotificationDelivery getByIdOrThrow(Long id) {
        return findById(id).orElseThrow(() ->
                new NotificationException(NotificationErrorCode.NOTIFICATION_DELIVERY_NOT_FOUND));
    }
}

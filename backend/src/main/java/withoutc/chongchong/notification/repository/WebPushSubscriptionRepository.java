package withoutc.chongchong.notification.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import withoutc.chongchong.notification.entity.WebPushSubscription;
import withoutc.chongchong.notification.exception.WebPushErrorCode;
import withoutc.chongchong.notification.exception.WebPushException;

public interface WebPushSubscriptionRepository extends JpaRepository<WebPushSubscription, Long> {

    Optional<WebPushSubscription> findByEndpoint(String endpoint);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            INSERT INTO web_push_subscriptions (
                user_id,
                endpoint,
                p256dh,
                auth,
                is_active,
                created_at,
                updated_at
            )
            VALUES (
                :userId,
                :endpoint,
                :p256dh,
                :auth,
                true,
                CURRENT_TIMESTAMP,
                CURRENT_TIMESTAMP
            )
            ON CONFLICT (endpoint)
            DO UPDATE SET
                p256dh = EXCLUDED.p256dh,
                auth = EXCLUDED.auth,
                is_active = true,
                updated_at = CURRENT_TIMESTAMP
            WHERE web_push_subscriptions.user_id = EXCLUDED.user_id
            """, nativeQuery = true)
    int upsert(
            @Param("userId") Long userId,
            @Param("endpoint") String endpoint,
            @Param("p256dh") String p256dh,
            @Param("auth") String auth
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE web_push_subscriptions
            SET is_active = false,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = :subscriptionId
              AND user_id = :userId
            """, nativeQuery = true)
    void deactivateByIdAndUserId(
            @Param("subscriptionId") Long subscriptionId,
            @Param("userId") Long userId
    );

    List<WebPushSubscription> findByUserIdAndIsActiveTrue(Long userId);

    default WebPushSubscription getByIdOrThrow(Long subscriptionId) {
        return findById(subscriptionId).orElseThrow(
                () -> new WebPushException(WebPushErrorCode.WEB_PUSH_SUBSCRIPTION_NOT_FOUND));
    }
}

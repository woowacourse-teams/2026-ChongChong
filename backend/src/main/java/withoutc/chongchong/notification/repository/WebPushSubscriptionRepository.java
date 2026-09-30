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

    Optional<WebPushSubscription> findByUserIdAndInstallationId(Long userId, String installationId);

    default WebPushSubscription getByUserIdAndInstallationIdOrThrow(Long userId, String installationId) {
        return findByUserIdAndInstallationId(userId, installationId)
                .orElseThrow(() -> new WebPushException(WebPushErrorCode.INVALID_WEB_PUSH_SUBSCRIPTION));
    }

    @Query(value = """
            WITH advisory_lock AS (
                SELECT pg_advisory_xact_lock(
                    hashtextextended('web-push-installation:' || :installationId, 0)
                )
            )
            SELECT 1
            FROM advisory_lock
            """, nativeQuery = true)
    int lockInstallationRegistration(@Param("installationId") String installationId);

    @Query(value = """
            WITH advisory_lock AS (
                SELECT pg_advisory_xact_lock(
                    hashtextextended('web-push-endpoint:' || :endpoint, 0)
                )
            )
            SELECT 1
            FROM advisory_lock
            """, nativeQuery = true)
    int lockEndpointRegistration(@Param("endpoint") String endpoint);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE web_push_subscriptions
            SET is_active = false,
                updated_at = CURRENT_TIMESTAMP
            WHERE is_active = true
              AND user_id != :userId
              AND (
                  installation_id = :installationId
                  OR endpoint = :endpoint
              )
            """, nativeQuery = true)
    int deactivateConflictingActiveSubscriptions(
            @Param("userId") Long userId,
            @Param("installationId") String installationId,
            @Param("endpoint") String endpoint
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            INSERT INTO web_push_subscriptions (
                user_id,
                installation_id,
                endpoint,
                p256dh,
                auth,
                is_active,
                created_at,
                updated_at
            )
            VALUES (
                :userId,
                :installationId,
                :endpoint,
                :p256dh,
                :auth,
                true,
                CURRENT_TIMESTAMP,
                CURRENT_TIMESTAMP
            )
            ON CONFLICT (user_id, installation_id)
            DO UPDATE SET
                endpoint = EXCLUDED.endpoint,
                p256dh = EXCLUDED.p256dh,
                auth = EXCLUDED.auth,
                is_active = true,
                updated_at = CURRENT_TIMESTAMP
            """, nativeQuery = true)
    int upsert(
            @Param("userId") Long userId,
            @Param("installationId") String installationId,
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

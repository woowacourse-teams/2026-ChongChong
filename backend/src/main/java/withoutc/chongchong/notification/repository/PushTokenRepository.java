package withoutc.chongchong.notification.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import withoutc.chongchong.notification.entity.PushToken;

public interface PushTokenRepository extends JpaRepository<PushToken, Long> {

    @Modifying
    @Query(value = """
            INSERT INTO push_tokens (
                user_id,
                installation_id,
                provider,
                token,
                platform,
                is_active,
                created_at,
                updated_at
            )
            VALUES (
                :userId,
                :installationId,
                :provider,
                :token,
                :platform,
                true,
                CURRENT_TIMESTAMP,
                CURRENT_TIMESTAMP
            )
            ON CONFLICT (installation_id)
            DO UPDATE SET
                user_id = EXCLUDED.user_id,
                provider = EXCLUDED.provider,
                token = EXCLUDED.token,
                platform = EXCLUDED.platform,
                is_active = true,
                updated_at = CURRENT_TIMESTAMP
            """, nativeQuery = true)
    void upsert(
            @Param("userId") Long userId,
            @Param("installationId") String installationId,
            @Param("provider") String provider,
            @Param("token") String token,
            @Param("platform") String platform
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE push_tokens
            SET is_active = false,
                updated_at = CURRENT_TIMESTAMP
            WHERE installation_id = :installationId
              AND user_id = :userId
            """, nativeQuery = true)
    void deactivateByInstallationIdAndUserId(
            @Param("installationId") String installationId,
            @Param("userId") Long userId
    );
}

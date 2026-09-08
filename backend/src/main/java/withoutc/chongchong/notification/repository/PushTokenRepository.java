package withoutc.chongchong.notification.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import withoutc.chongchong.notification.entity.PushToken;

public interface PushTokenRepository extends JpaRepository<PushToken, Long>, PushTokenUpsertRepository {

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

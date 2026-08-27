package withoutc.chongchong.notification.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import withoutc.chongchong.notification.entity.PushToken;

public interface PushTokenRepository extends JpaRepository<PushToken, Long> {

    Optional<PushToken> findByUserIdAndInstallationId(Long userId, String installationId);
}

package withoutc.chongchong.notification.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import withoutc.chongchong.notification.entity.PushToken;
import withoutc.chongchong.notification.entity.TokenProvider;

public interface PushTokenRepository extends JpaRepository<PushToken, Long> {
    boolean existsByProviderAndToken(TokenProvider provider, String token);
}

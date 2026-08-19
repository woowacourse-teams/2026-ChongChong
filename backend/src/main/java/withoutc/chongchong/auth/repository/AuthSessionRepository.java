package withoutc.chongchong.auth.repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import withoutc.chongchong.auth.entity.AuthSession;
import withoutc.chongchong.auth.token.HashedRefreshToken;

@Repository
public interface AuthSessionRepository extends JpaRepository<AuthSession, Long> {

    Optional<AuthSession> findByUserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT authSession
            FROM AuthSession authSession
            WHERE authSession.refreshTokenHash = :refreshTokenHash
            """)
    Optional<AuthSession> findByRefreshTokenHashForUpdate(
            @Param("refreshTokenHash") HashedRefreshToken refreshTokenHash
    );
}

package withoutc.chongchong.auth.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import withoutc.chongchong.auth.entity.AuthSession;
import withoutc.chongchong.auth.token.HashedRefreshToken;
import withoutc.chongchong.auth.token.RawRefreshToken;
import withoutc.chongchong.auth.token.RefreshTokenHasher;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthSessionRepositoryTest {

    private static final Instant EXPIRES_AT = Instant.parse("2026-09-18T00:00:00Z");
    private static final HashedRefreshToken REFRESH_TOKEN_HASH = new HashedRefreshToken("a".repeat(64));

    @Autowired
    private AuthSessionRepository authSessionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenHasher refreshTokenHasher;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("사용자와 Refresh Token 해시 및 만료 시각을 저장하고 조회한다")
    void saveAndFindAuthSession() {
        User user = saveUser("총총이");
        AuthSession saved = authSessionRepository.saveAndFlush(
                AuthSession.create(user, REFRESH_TOKEN_HASH, EXPIRES_AT)
        );
        entityManager.clear();

        AuthSession found = authSessionRepository.findByUserId(user.getId()).orElseThrow();

        assertThat(found.getId()).isEqualTo(saved.getId());
        assertThat(found.getUser().getId()).isEqualTo(user.getId());
        assertThat(found.getRefreshTokenHash()).isEqualTo(REFRESH_TOKEN_HASH);
        assertThat(found.getExpiresAt()).isEqualTo(EXPIRES_AT);
        assertThat(countRowsWithAuditingTimestamps(saved.getId())).isOne();
    }

    @Test
    @DisplayName("Refresh Token 원문 대신 SHA-256 해시만 데이터베이스에 저장한다")
    void storeOnlyHashedRefreshToken() {
        User user = saveUser("총총이");
        RawRefreshToken rawRefreshToken = new RawRefreshToken("raw-refresh-token-for-persistence-test");
        HashedRefreshToken hashedRefreshToken = refreshTokenHasher.hash(rawRefreshToken);
        AuthSession saved = authSessionRepository.saveAndFlush(
                AuthSession.create(user, hashedRefreshToken, EXPIRES_AT)
        );

        String storedValue = jdbcTemplate.queryForObject(
                "SELECT refresh_token_hash FROM auth_sessions WHERE id = ?",
                String.class,
                saved.getId()
        );

        assertThat(storedValue)
                .isEqualTo(hashedRefreshToken.value())
                .isNotEqualTo(rawRefreshToken.value());
    }

    @Test
    @DisplayName("한 사용자에게 두 개의 인증 세션을 저장하지 않는다")
    void rejectDuplicateUserSession() {
        User user = saveUser("총총이");
        authSessionRepository.saveAndFlush(AuthSession.create(user, REFRESH_TOKEN_HASH, EXPIRES_AT));
        AuthSession duplicate = AuthSession.create(
                user,
                new HashedRefreshToken("b".repeat(64)),
                EXPIRES_AT
        );

        assertThatThrownBy(() -> authSessionRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("같은 Refresh Token 해시를 여러 인증 세션에 저장하지 않는다")
    void rejectDuplicateRefreshTokenHash() {
        User firstUser = saveUser("첫 번째 사용자");
        User secondUser = saveUser("두 번째 사용자");
        authSessionRepository.saveAndFlush(AuthSession.create(firstUser, REFRESH_TOKEN_HASH, EXPIRES_AT));
        AuthSession duplicate = AuthSession.create(secondUser, REFRESH_TOKEN_HASH, EXPIRES_AT);

        assertThatThrownBy(() -> authSessionRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Refresh Token 해시 컬럼은 64자이고 NULL을 허용하지 않는다")
    void defineRefreshTokenHashColumnConstraints() {
        Long maximumLength = jdbcTemplate.queryForObject("""
                SELECT CHARACTER_MAXIMUM_LENGTH
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_SCHEMA = 'PUBLIC'
                  AND TABLE_NAME = 'AUTH_SESSIONS'
                  AND COLUMN_NAME = 'REFRESH_TOKEN_HASH'
                """, Long.class);
        String nullable = jdbcTemplate.queryForObject("""
                SELECT IS_NULLABLE
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_SCHEMA = 'PUBLIC'
                  AND TABLE_NAME = 'AUTH_SESSIONS'
                  AND COLUMN_NAME = 'REFRESH_TOKEN_HASH'
                """, String.class);

        assertThat(maximumLength).isEqualTo(64L);
        assertThat(nullable).isEqualTo("NO");
    }

    @Test
    @DisplayName("사용자 행을 비관적 쓰기 잠금으로 조회한다")
    void findUserForUpdate() {
        User saved = saveUser("총총이");
        entityManager.clear();

        User locked = userRepository.findByIdForUpdate(saved.getId()).orElseThrow();

        assertThat(entityManager.getLockMode(locked)).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
    }

    @Test
    @DisplayName("Refresh Token 해시로 인증 세션을 비관적 쓰기 잠금 조회한다")
    void findAuthSessionByRefreshTokenHashForUpdate() {
        User user = saveUser("총총이");
        authSessionRepository.saveAndFlush(AuthSession.create(user, REFRESH_TOKEN_HASH, EXPIRES_AT));
        entityManager.clear();

        AuthSession locked = authSessionRepository.findByRefreshTokenHashForUpdate(REFRESH_TOKEN_HASH)
                .orElseThrow();

        assertThat(entityManager.getLockMode(locked)).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
    }

    private User saveUser(String name) {
        return userRepository.saveAndFlush(User.create(name, null));
    }

    private Integer countRowsWithAuditingTimestamps(Long authSessionId) {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM auth_sessions
                WHERE id = ?
                  AND created_at IS NOT NULL
                  AND updated_at IS NOT NULL
                """, Integer.class, authSessionId);
    }
}

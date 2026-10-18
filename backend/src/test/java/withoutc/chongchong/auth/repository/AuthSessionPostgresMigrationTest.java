package withoutc.chongchong.auth.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers(disabledWithoutDocker = true)
class AuthSessionPostgresMigrationTest {

    private static final String FIRST_HASH = "a".repeat(64);
    private static final String SECOND_HASH = "b".repeat(64);

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:16-alpine")
    )
            .withDatabaseName("chongchong")
            .withUsername("postgres")
            .withPassword("postgres");

    @Test
    @DisplayName("V21은 기존 세션을 보존하며 사용자별 다중 세션과 일반 인덱스를 허용하고 해시 유일 제약을 유지한다")
    void preserveExistingSessionAndAllowMultipleSessions() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()
        );
        migrateTo(dataSource, "20");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.update("""
                INSERT INTO users (id, name, created_at, updated_at)
                VALUES (1, '기존 사용자', TIMESTAMP '2026-10-01 12:00:00', TIMESTAMP '2026-10-01 12:00:00')
                """);
        insertSession(jdbc, 1L, 1L, FIRST_HASH);
        var existingSession = jdbc.queryForMap("SELECT * FROM auth_sessions WHERE id = 1");

        assertThatThrownBy(() -> insertSession(jdbc, 2L, 1L, SECOND_HASH))
                .isInstanceOf(DataIntegrityViolationException.class);

        migrateTo(dataSource, "21");

        assertThat(jdbc.queryForMap("SELECT * FROM auth_sessions WHERE id = 1"))
                .isEqualTo(existingSession);
        insertSession(jdbc, 2L, 1L, SECOND_HASH);
        assertThat(jdbc.queryForList(
                "SELECT refresh_token_hash FROM auth_sessions WHERE user_id = 1", String.class
        )).containsExactlyInAnyOrder(FIRST_HASH, SECOND_HASH);

        jdbc.update("""
                INSERT INTO users (id, name, created_at, updated_at)
                VALUES (2, '다른 사용자', TIMESTAMP '2026-10-01 12:00:00', TIMESTAMP '2026-10-01 12:00:00')
                """);
        assertThatThrownBy(() -> insertSession(jdbc, 3L, 1L, FIRST_HASH))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertSession(jdbc, 4L, 2L, FIRST_HASH))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM auth_sessions", Long.class)).isEqualTo(2L);
        assertThat(jdbc.queryForMap("SELECT * FROM auth_sessions WHERE id = 1"))
                .isEqualTo(existingSession);

        assertThat(jdbc.queryForObject("""
                SELECT NOT index_info.indisunique
                       AND index_info.indisvalid
                       AND index_info.indnatts = 1
                       AND pg_get_indexdef(index_info.indexrelid, 1, TRUE) = 'user_id'
                FROM pg_index index_info
                JOIN pg_class index_table ON index_table.oid = index_info.indexrelid
                WHERE index_table.relname = 'idx_auth_sessions_user_id'
                  AND index_info.indrelid = 'public.auth_sessions'::regclass
                """, Boolean.class)).isTrue();
    }

    private void migrateTo(DriverManagerDataSource dataSource, String version) {
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .target(version)
                .load()
                .migrate();
    }

    private void insertSession(JdbcTemplate jdbc, long sessionId, long userId, String hash) {
        jdbc.update("""
                INSERT INTO auth_sessions (id, user_id, refresh_token_hash, expires_at, created_at, updated_at)
                VALUES (?, ?, ?, TIMESTAMPTZ '2030-01-01 00:00:00+00',
                        TIMESTAMP '2026-10-01 12:00:00.123456', TIMESTAMP '2026-10-02 13:00:00.654321')
                """, sessionId, userId, hash);
    }
}

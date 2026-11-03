package withoutc.chongchong.file.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers(disabledWithoutDocker = true)
class FileOwnerMigrationTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:16-alpine"));

    @Test
    @DisplayName("PostgreSQL 기존 파일과 프로필을 보존하며 사용자 삭제 시 소유자 연결만 해제한다")
    void migratesPostgresWithoutDeletingFiles() {
        verifyMigration(new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()));
    }

    @Test
    @DisplayName("H2에서도 기존 자동 생성 외래 키를 교체하고 사용자 삭제 시 소유자 연결만 해제한다")
    void migratesH2WithoutDeletingFiles() {
        String url = "jdbc:h2:mem:file-owner-" + UUID.randomUUID() + ";MODE=PostgreSQL";
        try (SingleConnectionDataSource dataSource = new SingleConnectionDataSource(url, "sa", "", true)) {
            verifyMigration(dataSource);
        }
    }

    private void verifyMigration(DataSource dataSource) {
        migrate(dataSource, "29");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.update("""
                INSERT INTO users (id, name, created_at, updated_at)
                VALUES (1, '소유자', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                       (2, '다른 사용자', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);
        insertFile(jdbc, 10, 1, "COMPLETED");
        insertFile(jdbc, 11, 1, "PENDING");
        insertFile(jdbc, 12, 1, "DELETE_PENDING");
        insertFile(jdbc, 20, 2, "COMPLETED");
        jdbc.update("UPDATE users SET profile_image_file_id = 10 WHERE id = 1");

        migrate(dataSource, "30");

        assertThat(jdbc.queryForObject("SELECT profile_image_file_id FROM users WHERE id = 1", Long.class))
                .isEqualTo(10L);
        assertThat(jdbc.queryForList("SELECT owner_id FROM file_metadata ORDER BY id", Long.class))
                .containsExactly(1L, 1L, 1L, 2L);
        assertThatThrownBy(() -> jdbc.update("UPDATE file_metadata SET owner_id = 999 WHERE id = 10"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM file_metadata WHERE id = 10"))
                .isInstanceOf(DataIntegrityViolationException.class);

        jdbc.update("DELETE FROM users WHERE id = 1");

        assertThat(jdbc.queryForList("SELECT owner_id FROM file_metadata ORDER BY id", Long.class))
                .containsExactly(null, null, null, 2L);
        assertThat(jdbc.queryForList("SELECT status FROM file_metadata ORDER BY id", String.class))
                .containsExactly("COMPLETED", "PENDING", "DELETE_PENDING", "COMPLETED");
        assertThat(jdbc.queryForList("SELECT object_key FROM file_metadata ORDER BY id", String.class))
                .containsExactly("uploads/10", "uploads/11", "uploads/12", "uploads/20");
    }

    private void migrate(DataSource dataSource, String target) {
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration")
                .target(target).load().migrate();
    }

    private void insertFile(JdbcTemplate jdbc, long id, long ownerId, String status) {
        jdbc.update("""
                INSERT INTO file_metadata
                    (id, owner_id, original_file_name, object_key, content_type, size, status, created_at, updated_at)
                VALUES (?, ?, 'image.png', ?, 'image/png', 1024, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, id, ownerId, "uploads/" + id, status);
    }
}

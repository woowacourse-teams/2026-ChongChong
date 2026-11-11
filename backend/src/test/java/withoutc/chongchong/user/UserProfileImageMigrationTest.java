package withoutc.chongchong.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

class UserProfileImageMigrationTest {

    @Test
    @DisplayName("기존 홈 프로필을 보존하면서 선택적인 파일 연결과 중복·잘못된 참조·사용 중 파일 삭제를 제한한다")
    void addOptionalUniqueProfileImageWithoutChangingExistingProfiles() {
        String url = "jdbc:h2:mem:user-profile-image-" + UUID.randomUUID() + ";MODE=PostgreSQL";
        try (SingleConnectionDataSource dataSource = new SingleConnectionDataSource(url, "sa", "", true)) {
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            jdbc.execute("CREATE TABLE users (id BIGINT PRIMARY KEY, profile_image_url VARCHAR(2048))");
            jdbc.execute("""
                    CREATE TABLE file_metadata (
                        id BIGINT PRIMARY KEY,
                        owner_id BIGINT NOT NULL REFERENCES users (id),
                        status VARCHAR(20) NOT NULL
                    )
                    """);
            jdbc.update("INSERT INTO users (id, profile_image_url) VALUES (1, ?), (2, NULL)",
                    "https://example.com/social.png");

            Flyway.configure()
                    .dataSource(dataSource)
                    .locations("classpath:db/migration")
                    .baselineOnMigrate(true)
                    .baselineVersion("28")
                    .target("29")
                    .load()
                    .migrate();

            assertThat(jdbc.queryForList("SELECT profile_image_file_id FROM users ORDER BY id", Long.class))
                    .containsExactly(null, null);
            assertThat(jdbc.queryForList("SELECT profile_image_url FROM users ORDER BY id", String.class))
                    .containsExactly("https://example.com/social.png", null);

            jdbc.update("INSERT INTO file_metadata (id, owner_id, status) VALUES (10, 1, 'COMPLETED')");
            jdbc.update("UPDATE users SET profile_image_file_id = 10 WHERE id = 1");

            assertThat(jdbc.queryForObject("SELECT profile_image_file_id FROM users WHERE id = 1", Long.class))
                    .isEqualTo(10L);
            assertThatThrownBy(() -> jdbc.update("UPDATE users SET profile_image_file_id = 10 WHERE id = 2"))
                    .isInstanceOf(DataIntegrityViolationException.class);
            assertThatThrownBy(() -> jdbc.update("UPDATE users SET profile_image_file_id = 999 WHERE id = 2"))
                    .isInstanceOf(DataIntegrityViolationException.class);
            assertThatThrownBy(() -> jdbc.update("DELETE FROM file_metadata WHERE id = 10"))
                    .isInstanceOf(DataIntegrityViolationException.class);

            jdbc.update("UPDATE users SET profile_image_file_id = NULL WHERE id = 1");

            assertThat(jdbc.queryForObject("SELECT status FROM file_metadata WHERE id = 10", String.class))
                    .isEqualTo("COMPLETED");
            assertThat(jdbc.queryForObject("SELECT profile_image_url FROM users WHERE id = 1", String.class))
                    .isEqualTo("https://example.com/social.png");
        }
    }
}

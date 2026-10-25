package withoutc.chongchong.study;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

class StudyVisibilityMigrationTest {

    @Test
    @DisplayName("기존 스터디와 새 스터디는 기본적으로 비공개이며 공개 여부는 null일 수 없다")
    void migrateExistingStudiesAsPrivate() {
        String url = "jdbc:h2:mem:study-visibility-" + UUID.randomUUID() + ";MODE=PostgreSQL";
        try (SingleConnectionDataSource dataSource = new SingleConnectionDataSource(url, "sa", "", true)) {
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            jdbc.execute("CREATE TABLE studies (id BIGINT PRIMARY KEY, name VARCHAR(255) NOT NULL)");
            jdbc.update("INSERT INTO studies (id, name) VALUES (1, '기존 스터디')");
            jdbc.update("INSERT INTO studies (id, name) VALUES (2, '다른 기존 스터디')");

            Flyway.configure()
                    .dataSource(dataSource)
                    .locations("classpath:db/migration")
                    .baselineOnMigrate(true)
                    .baselineVersion("25")
                    .target("26")
                    .load()
                    .migrate();

            assertThat(jdbc.queryForList("SELECT is_public FROM studies ORDER BY id", Boolean.class))
                    .containsExactly(false, false);
            jdbc.update("INSERT INTO studies (id, name) VALUES (3, '새 스터디')");
            assertThat(jdbc.queryForObject("SELECT is_public FROM studies WHERE id = 3", Boolean.class))
                    .isFalse();
            jdbc.update("UPDATE studies SET is_public = TRUE WHERE id = 1");
            assertThat(jdbc.queryForObject("SELECT is_public FROM studies WHERE id = 1", Boolean.class))
                    .isTrue();
            assertThatThrownBy(() -> jdbc.update("UPDATE studies SET is_public = NULL WHERE id = 1"))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }
    }
}

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

class StudyImageMigrationTest {

    @Test
    @DisplayName("기존 스터디의 이미지는 null이고 한 파일을 여러 스터디에 연결할 수 없다")
    void addOptionalUniqueStudyImageFile() {
        String url = "jdbc:h2:mem:study-image-" + UUID.randomUUID() + ";MODE=PostgreSQL";
        try (SingleConnectionDataSource dataSource = new SingleConnectionDataSource(url, "sa", "", true)) {
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            jdbc.execute("CREATE TABLE file_metadata (id BIGINT PRIMARY KEY)");
            jdbc.execute("CREATE TABLE studies (id BIGINT PRIMARY KEY)");
            jdbc.update("INSERT INTO studies (id) VALUES (1), (2)");

            Flyway.configure()
                    .dataSource(dataSource)
                    .locations("classpath:db/migration")
                    .baselineOnMigrate(true)
                    .baselineVersion("26")
                    .target("27")
                    .load()
                    .migrate();

            assertThat(jdbc.queryForList("SELECT image_file_id FROM studies ORDER BY id", Long.class))
                    .containsExactly(null, null);

            jdbc.update("INSERT INTO file_metadata (id) VALUES (10)");
            jdbc.update("UPDATE studies SET image_file_id = 10 WHERE id = 1");
            assertThat(jdbc.queryForObject("SELECT image_file_id FROM studies WHERE id = 1", Long.class))
                    .isEqualTo(10L);
            assertThatThrownBy(() -> jdbc.update("UPDATE studies SET image_file_id = 10 WHERE id = 2"))
                    .isInstanceOf(DataIntegrityViolationException.class);
            assertThatThrownBy(() -> jdbc.update("UPDATE studies SET image_file_id = 999 WHERE id = 2"))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }
    }
}

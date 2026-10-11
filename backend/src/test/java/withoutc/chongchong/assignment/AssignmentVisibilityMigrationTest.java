package withoutc.chongchong.assignment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

class AssignmentVisibilityMigrationTest {

    @Test
    @DisplayName("기존 과제는 리더 공개로 이관하고 공개 범위 기본값과 제약을 적용한다")
    void migrateExistingAssignmentsAsLeaderOnly() {
        String url = "jdbc:h2:mem:visibility-" + UUID.randomUUID() + ";MODE=PostgreSQL";
        try (SingleConnectionDataSource dataSource = new SingleConnectionDataSource(url, "sa", "", true)) {
            Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").target("16")
                    .load().migrate();
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            jdbc.update("INSERT INTO studies (id, name) VALUES (1, '스터디')");
            jdbc.update("""
                    INSERT INTO assignments (id, title, content, close_at, study_id)
                    VALUES (1, '기존 과제', '내용', TIMESTAMP '2099-01-01 00:00:00', 1)
                    """);

            Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").target("17")
                    .load().migrate();

            assertThat(jdbc.queryForObject(
                    "SELECT submission_visibility FROM assignments WHERE id = 1", String.class))
                    .isEqualTo("LEADER_ONLY");
            jdbc.update("""
                    INSERT INTO assignments (id, title, content, close_at, study_id)
                    VALUES (2, '기본값 과제', '내용', TIMESTAMP '2099-01-01 00:00:00', 1)
                    """);
            assertThat(jdbc.queryForObject(
                    "SELECT submission_visibility FROM assignments WHERE id = 2", String.class))
                    .isEqualTo("LEADER_ONLY");

            jdbc.update("UPDATE assignments SET submission_visibility = 'ALL_STUDY_MEMBERS' WHERE id = 1");
            assertThat(jdbc.queryForObject(
                    "SELECT submission_visibility FROM assignments WHERE id = 1", String.class))
                    .isEqualTo("ALL_STUDY_MEMBERS");
            assertThatThrownBy(() -> jdbc.update(
                    "UPDATE assignments SET submission_visibility = NULL WHERE id = 1"))
                    .isInstanceOf(DataIntegrityViolationException.class);
            assertThatThrownBy(() -> jdbc.update(
                    "UPDATE assignments SET submission_visibility = 'UNKNOWN' WHERE id = 1"))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }
    }
}

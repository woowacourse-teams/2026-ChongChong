package withoutc.chongchong.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;

class TestDatabaseCleanerPostgresTest extends PostgresContainerTest {

    @Autowired
    private TestDatabaseCleaner databaseCleaner;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private Flyway flyway;

    @Test
    @DisplayName("애플리케이션 데이터를 정리해도 마이그레이션 이력은 보존하고 재실행하지 않는다")
    void clearsApplicationDataWithoutRemovingMigrationHistory() {
        databaseCleaner.clean();
        var migrationHistory = jdbcTemplate.queryForList(
                "SELECT * FROM flyway_schema_history ORDER BY installed_rank");
        assertThat(migrationHistory).isNotEmpty();
        userRepository.saveAndFlush(User.create("정리 대상", null));

        databaseCleaner.clean();

        assertThat(userRepository.count()).isZero();
        assertThat(jdbcTemplate.queryForList(
                "SELECT * FROM flyway_schema_history ORDER BY installed_rank"))
                .isEqualTo(migrationHistory);
        flyway.validate();
        assertThat(flyway.migrate().migrationsExecuted).isZero();
    }
}

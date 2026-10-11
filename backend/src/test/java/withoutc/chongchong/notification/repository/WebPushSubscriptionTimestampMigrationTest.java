package withoutc.chongchong.notification.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

class WebPushSubscriptionTimestampMigrationTest {

    private SingleConnectionDataSource dataSource;

    @AfterEach
    void closeDataSource() {
        if (dataSource != null) {
            dataSource.destroy();
        }
    }

    @Test
    void correctsUtcCreatedAtAndShiftsUtcUpdatesBeforeSeoulTimeRange() {
        String url = "jdbc:h2:mem:web-push-timestamps-%s;MODE=PostgreSQL".formatted(UUID.randomUUID());
        dataSource = new SingleConnectionDataSource(url, "sa", "", true);
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .target(MigrationVersion.fromVersion("17"))
                .load()
                .migrate();

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.update("INSERT INTO users (id, name) VALUES (1, '테스트 사용자')");

        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 1, 0, 0);
        insertSubscription(jdbcTemplate, "before-created", createdAt, createdAt.minusHours(2));
        insertSubscription(jdbcTemplate, "at-created", createdAt, createdAt);
        insertSubscription(jdbcTemplate, "utc-five-hours-after", createdAt, createdAt.plusHours(5));
        insertSubscription(jdbcTemplate, "at-corrected-created", createdAt, createdAt.plusHours(9));
        insertSubscription(jdbcTemplate, "after-corrected-created", createdAt, createdAt.plusHours(10));
        insertSubscription(jdbcTemplate, "null-updated-at", createdAt, null);

        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load()
                .migrate();

        LocalDateTime correctedCreatedAt = createdAt.plusHours(9);
        assertTimestamps(jdbcTemplate, "before-created", correctedCreatedAt, correctedCreatedAt);
        assertTimestamps(jdbcTemplate, "at-created", correctedCreatedAt, correctedCreatedAt);
        assertTimestamps(jdbcTemplate, "utc-five-hours-after", correctedCreatedAt, createdAt.plusHours(14));
        assertTimestamps(jdbcTemplate, "at-corrected-created", correctedCreatedAt, correctedCreatedAt);
        assertTimestamps(jdbcTemplate, "after-corrected-created", correctedCreatedAt, createdAt.plusHours(10));
        assertThat(readTimestamp(jdbcTemplate, "null-updated-at", "created_at")).isEqualTo(correctedCreatedAt);
        assertThat(readTimestamp(jdbcTemplate, "null-updated-at", "updated_at")).isNull();
    }

    private void insertSubscription(
            JdbcTemplate jdbcTemplate,
            String installationId,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        jdbcTemplate.update("""
                INSERT INTO web_push_subscriptions (user_id, installation_id, endpoint, p256dh, auth, is_active,
                                                    created_at, updated_at)
                VALUES (1, ?, ?, 'p256dh-key', 'auth-secret', TRUE, ?, ?)
                """, installationId, "https://push.example.com/" + installationId, createdAt, updatedAt);
    }

    private void assertTimestamps(
            JdbcTemplate jdbcTemplate,
            String installationId,
            LocalDateTime expectedCreatedAt,
            LocalDateTime expectedUpdatedAt
    ) {
        assertThat(readTimestamp(jdbcTemplate, installationId, "created_at")).isEqualTo(expectedCreatedAt);
        assertThat(readTimestamp(jdbcTemplate, installationId, "updated_at")).isEqualTo(expectedUpdatedAt);
    }

    private LocalDateTime readTimestamp(JdbcTemplate jdbcTemplate, String installationId, String column) {
        Timestamp timestamp = jdbcTemplate.queryForObject(
                "SELECT " + column + " FROM web_push_subscriptions WHERE installation_id = ?",
                Timestamp.class,
                installationId
        );
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}

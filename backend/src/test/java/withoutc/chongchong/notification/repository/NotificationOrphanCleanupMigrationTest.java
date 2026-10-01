package withoutc.chongchong.notification.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

class NotificationOrphanCleanupMigrationTest {

    private SingleConnectionDataSource dataSource;

    @AfterEach
    void closeDataSource() {
        if (dataSource != null) {
            dataSource.destroy();
        }
    }

    @Test
    void deletesOnlyNotificationsWhoseResourcesAreMissing() {
        String url = "jdbc:h2:mem:notification-orphan-cleanup-%s;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
                .formatted(UUID.randomUUID());
        dataSource = new SingleConnectionDataSource(url, "sa", "", true);
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .target(MigrationVersion.fromVersion("15"))
                .load()
                .migrate();
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        long recipientId = insertId(jdbcTemplate, "INSERT INTO users (name) VALUES (?)", "알림 수신자");
        long studyId = insertId(jdbcTemplate,
                "INSERT INTO studies (name, description) VALUES (?, ?)", "스터디", "설명");
        long noticeId = insertId(jdbcTemplate,
                "INSERT INTO notices (title, content, study_id) VALUES (?, ?, ?)", "공지", "내용", studyId);
        long subscriptionId = insertId(jdbcTemplate, """
                INSERT INTO web_push_subscriptions (user_id, installation_id, endpoint, p256dh, auth, is_active)
                VALUES (?, ?, ?, ?, ?, ?)
                """, recipientId, UUID.randomUUID().toString(), "https://push.example.com/subscription",
                "p256dh-key", "auth-secret", true);

        long validNotificationId = insertNotification(jdbcTemplate, recipientId, "NOTICE", noticeId);
        long orphanNoticeNotificationId = insertNotification(jdbcTemplate, recipientId, "NOTICE", 9001L);
        long orphanAssignmentNotificationId = insertNotification(jdbcTemplate, recipientId, "ASSIGNMENT", 9002L);
        long orphanSubmissionNotificationId = insertNotification(
                jdbcTemplate, recipientId, "ASSIGNMENT_SUBMISSION", 9003L);
        insertDelivery(jdbcTemplate, validNotificationId, subscriptionId);
        insertDelivery(jdbcTemplate, orphanNoticeNotificationId, subscriptionId);
        insertDelivery(jdbcTemplate, orphanAssignmentNotificationId, subscriptionId);
        insertDelivery(jdbcTemplate, orphanSubmissionNotificationId, subscriptionId);

        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load()
                .migrate();

        assertThat(count(jdbcTemplate, "SELECT COUNT(*) FROM notifications")).isOne();
        assertThat(count(jdbcTemplate,
                "SELECT COUNT(*) FROM notifications WHERE id = ?", validNotificationId)).isOne();
        assertThat(count(jdbcTemplate, "SELECT COUNT(*) FROM notification_deliveries")).isOne();
        assertThat(count(jdbcTemplate,
                "SELECT COUNT(*) FROM notification_deliveries WHERE notification_id = ?", validNotificationId))
                .isOne();
    }

    private long insertNotification(JdbcTemplate jdbcTemplate, long recipientId, String resourceType,
                                    long resourceId) {
        return insertId(jdbcTemplate, """
                INSERT INTO notifications (recipient_id, title, body, deep_link, type, resource_type,
                                           resource_id, is_read)
                VALUES (?, ?, ?, ?, 'NEW', ?, ?, FALSE)
                """, recipientId, "알림", "내용", "/studies/1", resourceType, resourceId);
    }

    private void insertDelivery(JdbcTemplate jdbcTemplate, long notificationId, long subscriptionId) {
        jdbcTemplate.update("""
                INSERT INTO notification_deliveries (notification_id, web_push_subscription_id, status, attempt_count)
                VALUES (?, ?, 'PENDING', 0)
                """, notificationId, subscriptionId);
    }

    private long insertId(JdbcTemplate jdbcTemplate, String sql, Object... parameters) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        PreparedStatementCreator statementCreator = connection -> {
            PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int index = 0; index < parameters.length; index++) {
                statement.setObject(index + 1, parameters[index]);
            }
            return statement;
        };
        jdbcTemplate.update(statementCreator, keyHolder);
        return keyHolder.getKey().longValue();
    }

    private long count(JdbcTemplate jdbcTemplate, String sql, Object... parameters) {
        return jdbcTemplate.queryForObject(sql, Long.class, parameters);
    }
}

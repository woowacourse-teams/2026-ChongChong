package withoutc.chongchong.notification.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers(disabledWithoutDocker = true)
class NotificationOrphanCleanupPostgresMigrationTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:16-alpine")
    )
            .withDatabaseName("chongchong")
            .withUsername("postgres")
            .withPassword("postgres");

    @Test
    void deletesOnlyNotificationsWhoseResourcesAreMissing() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
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
        long memberId = insertId(jdbcTemplate, """
                INSERT INTO study_members (name, role, study_id, user_id)
                VALUES (?, 'MEMBER', ?, ?)
                """, "멤버", studyId, recipientId);
        long noticeId = insertId(jdbcTemplate,
                "INSERT INTO notices (title, content, study_id) VALUES (?, ?, ?)", "공지", "내용", studyId);
        long assignmentId = insertId(jdbcTemplate, """
                INSERT INTO assignments (title, content, submission_method, close_at, submission_target, study_id)
                VALUES (?, ?, ?, ?, 'MEMBERS_ONLY', ?)
                """, "과제", "내용", "링크 제출", LocalDateTime.of(2030, 1, 2, 9, 0), studyId);
        long submissionId = insertId(jdbcTemplate, """
                INSERT INTO assignment_submissions (assignment_id, member_id)
                VALUES (?, ?)
                """, assignmentId, memberId);
        long subscriptionId = insertId(jdbcTemplate, """
                INSERT INTO web_push_subscriptions (user_id, installation_id, endpoint, p256dh, auth, is_active)
                VALUES (?, ?, ?, ?, ?, ?)
                """, recipientId, UUID.randomUUID().toString(), "https://push.example.com/subscription",
                "p256dh-key", "auth-secret", true);

        long validNoticeNotificationId = insertNotification(jdbcTemplate, recipientId, "NOTICE", noticeId);
        long validAssignmentNotificationId = insertNotification(jdbcTemplate, recipientId, "ASSIGNMENT", assignmentId);
        long validSubmissionNotificationId = insertNotification(
                jdbcTemplate, recipientId, "ASSIGNMENT_SUBMISSION", submissionId);
        long orphanNoticeNotificationId = insertNotification(jdbcTemplate, recipientId, "NOTICE", 9001L);
        long orphanAssignmentNotificationId = insertNotification(jdbcTemplate, recipientId, "ASSIGNMENT", 9002L);
        long orphanSubmissionNotificationId = insertNotification(
                jdbcTemplate, recipientId, "ASSIGNMENT_SUBMISSION", 9003L);
        insertDelivery(jdbcTemplate, validNoticeNotificationId, subscriptionId);
        insertDelivery(jdbcTemplate, validAssignmentNotificationId, subscriptionId);
        insertDelivery(jdbcTemplate, validSubmissionNotificationId, subscriptionId);
        insertDelivery(jdbcTemplate, orphanNoticeNotificationId, subscriptionId);
        insertDelivery(jdbcTemplate, orphanAssignmentNotificationId, subscriptionId);
        insertDelivery(jdbcTemplate, orphanSubmissionNotificationId, subscriptionId);

        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load()
                .migrate();

        assertThat(count(jdbcTemplate, "SELECT COUNT(*) FROM notifications")).isEqualTo(3);
        assertThat(count(jdbcTemplate, "SELECT COUNT(*) FROM notification_deliveries")).isEqualTo(3);
        assertThat(count(jdbcTemplate, """
                SELECT COUNT(*) FROM notifications
                WHERE id IN (?, ?, ?)
                """, validNoticeNotificationId, validAssignmentNotificationId, validSubmissionNotificationId))
                .isEqualTo(3);
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
            PreparedStatement statement = connection.prepareStatement(sql, new String[]{"id"});
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

package withoutc.chongchong.assignment.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
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
class AssignmentRecurrencePostgresMigrationTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 22, 12, 0);
    private static final LocalDate OCCURRENCE_DATE = LocalDate.of(2026, 12, 1);

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:16-alpine")
    )
            .withDatabaseName("chongchong")
            .withUsername("postgres")
            .withPassword("postgres");

    private JdbcTemplate jdbcTemplate;
    private long studyId;
    private long memberId;
    private long recurrenceId;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .cleanDisabled(false)
                .target(MigrationVersion.fromVersion("35"))
                .load();
        flyway.clean();
        flyway.migrate();
        jdbcTemplate = new JdbcTemplate(dataSource);

        long userId = insertId("INSERT INTO users (name, created_at, updated_at) VALUES (?, ?, ?)", "멤버", NOW, NOW);
        studyId = insertId("INSERT INTO studies (name, created_at, updated_at) VALUES (?, ?, ?)", "스터디", NOW, NOW);
        memberId = insertId("""
                INSERT INTO study_members (name, role, study_id, user_id, created_at, updated_at)
                VALUES (?, 'MEMBER', ?, ?, ?, ?)
                """, "멤버", studyId, userId, NOW, NOW);
        recurrenceId = insertRecurrence("SELECTED", null, 1);
    }

    @Test
    @DisplayName("같은 반복 규칙의 같은 회차 날짜는 한 번만 저장된다")
    void rejectsDuplicateOccurrence() {
        insertOccurrence(recurrenceId, OCCURRENCE_DATE);

        assertThatThrownBy(() -> insertOccurrence(recurrenceId, OCCURRENCE_DATE))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("반복 규칙을 삭제해도 생성된 회차는 남고 규칙 연결만 끊긴다")
    void keepsOccurrenceWhenRecurrenceIsDeleted() {
        long assignmentId = insertOccurrence(recurrenceId, OCCURRENCE_DATE);

        jdbcTemplate.update("DELETE FROM assignment_recurrences WHERE id = ?", recurrenceId);

        assertThat(jdbcTemplate.queryForObject(
                "SELECT recurrence_id FROM assignments WHERE id = ?", Long.class, assignmentId)).isNull();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT occurrence_date FROM assignments WHERE id = ?", LocalDate.class, assignmentId))
                .isEqualTo(OCCURRENCE_DATE);
    }

    @Test
    @DisplayName("스터디를 나간 멤버는 직접 선택한 제출 대상에서 빠진다")
    void removesSelectedSubmitterWhenMemberIsDeleted() {
        jdbcTemplate.update("INSERT INTO assignment_recurrence_submitters (recurrence_id, member_id) VALUES (?, ?)",
                recurrenceId, memberId);

        jdbcTemplate.update("DELETE FROM study_members WHERE id = ?", memberId);

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM assignment_recurrence_submitters WHERE recurrence_id = ?",
                Integer.class, recurrenceId)).isZero();
    }

    @Test
    @DisplayName("범위 지정 규칙만 제출 대상 범위를 가지고, 반복 간격은 1~4주다")
    void validatesSubmitterModeAndInterval() {
        assertThatThrownBy(() -> insertRecurrence("RANGE", null, 1))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertRecurrence("SELECTED", "MEMBERS_ONLY", 1))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertRecurrence("RANGE", "MEMBERS_ONLY", 5))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private long insertRecurrence(String submitterMode, String submissionTarget, int intervalWeeks) {
        return insertId("""
                INSERT INTO assignment_recurrences (
                    study_id, title, content, submission_method, submission_visibility, submitter_mode,
                    submission_target, close_time, interval_weeks, next_occurrence_date, next_publish_at,
                    created_at, updated_at
                )
                VALUES (?, '알고리즘 풀이', '내용', '', 'LEADER_ONLY', ?, ?, ?, ?, ?, ?, ?, ?)
                """, studyId, submitterMode, submissionTarget, LocalTime.of(23, 59), intervalWeeks,
                OCCURRENCE_DATE, NOW, NOW, NOW);
    }

    private long insertOccurrence(long recurrenceId, LocalDate occurrenceDate) {
        return insertId("""
                INSERT INTO assignments (
                    title, content, submission_method, close_at, submission_target, submission_visibility,
                    study_id, recurrence_id, occurrence_date, created_at, updated_at
                )
                VALUES ('알고리즘 풀이', '내용', '', ?, 'MEMBERS_ONLY', 'LEADER_ONLY', ?, ?, ?, ?, ?)
                """, occurrenceDate.atTime(23, 59), studyId, recurrenceId, occurrenceDate, NOW, NOW);
    }

    private long insertId(String sql, Object... args) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        PreparedStatementCreator creator = connection -> {
            PreparedStatement statement = connection.prepareStatement(sql, new String[]{"id"});
            for (int i = 0; i < args.length; i++) {
                statement.setObject(i + 1, args[i]);
            }
            return statement;
        };
        jdbcTemplate.update(creator, keyHolder);
        return keyHolder.getKey().longValue();
    }
}

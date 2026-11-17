package withoutc.chongchong.support;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("test")
public class TestDatabaseCleaner {

    private final JdbcTemplate jdbcTemplate;
    // 컨텍스트 시작 시 Flyway가 스키마를 확정하고 테스트 중 테이블을 추가·삭제하지 않으므로 한 번만 만든다.
    private volatile String postgreSqlTruncateStatement;

    public TestDatabaseCleaner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void clean() {
        String databaseProductName = databaseProductName();
        if (databaseProductName.equals("H2")) {
            cleanH2();
            return;
        }
        if (databaseProductName.equals("PostgreSQL")) {
            cleanPostgreSql();
            return;
        }
        throw new IllegalStateException("지원하지 않는 테스트 데이터베이스입니다: " + databaseProductName);
    }

    private void cleanH2() {
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");

        try {
            findH2TableNames().forEach(this::truncate);
        } finally {
            jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
        }
    }

    private void cleanPostgreSql() {
        String statement = postgreSqlTruncateStatement;
        if (statement == null) {
            statement = createPostgreSqlTruncateStatement();
            postgreSqlTruncateStatement = statement;
        }
        jdbcTemplate.execute(statement);
    }

    private String createPostgreSqlTruncateStatement() {
        List<String> tableNames = findPostgreSqlTableNames();
        if (tableNames.isEmpty()) {
            throw new IllegalStateException("정리할 PostgreSQL 테이블이 없습니다.");
        }
        // 테이블마다 TRUNCATE를 실행하면 문장마다 잠금·커밋 비용이 들어 한 문장으로 모두 정리한다.
        return tableNames.stream()
                .map(this::quoteIdentifier)
                .collect(Collectors.joining(", ", "TRUNCATE TABLE ", " RESTART IDENTITY CASCADE"));
    }

    private String databaseProductName() {
        return jdbcTemplate.execute((ConnectionCallback<String>) connection ->
                connection.getMetaData().getDatabaseProductName()
        );
    }

    private List<String> findH2TableNames() {
        return jdbcTemplate.queryForList("""
                SELECT TABLE_NAME
                FROM INFORMATION_SCHEMA.TABLES
                WHERE TABLE_SCHEMA = 'PUBLIC'
                  AND TABLE_TYPE = 'BASE TABLE'
                  AND LOWER(TABLE_NAME) <> 'flyway_schema_history'
                """, String.class);
    }

    private List<String> findPostgreSqlTableNames() {
        return jdbcTemplate.queryForList("""
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_type = 'BASE TABLE'
                  AND table_name <> 'flyway_schema_history'
                """, String.class);
    }

    private void truncate(String tableName) {
        jdbcTemplate.execute("TRUNCATE TABLE " + quoteIdentifier(tableName));
    }

    private String quoteIdentifier(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }
}

package withoutc.chongchong.support;

import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("test")
public class TestDatabaseCleaner {

    private final JdbcTemplate jdbcTemplate;

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
        findPostgreSqlTableNames().forEach(this::truncateAndRestartIdentity);
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
                """, String.class);
    }

    private List<String> findPostgreSqlTableNames() {
        return jdbcTemplate.queryForList("""
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_type = 'BASE TABLE'
                """, String.class);
    }

    private void truncate(String tableName) {
        jdbcTemplate.execute("TRUNCATE TABLE " + quoteIdentifier(tableName));
    }

    private void truncateAndRestartIdentity(String tableName) {
        jdbcTemplate.execute("TRUNCATE TABLE " + quoteIdentifier(tableName) + " RESTART IDENTITY CASCADE");
    }

    private String quoteIdentifier(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }
}

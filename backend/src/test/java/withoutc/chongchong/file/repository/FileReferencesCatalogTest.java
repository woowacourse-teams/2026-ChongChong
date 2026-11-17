package withoutc.chongchong.file.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import withoutc.chongchong.file.repository.FileReferences.Reference;
import withoutc.chongchong.support.PostgresContainerTest;

class FileReferencesCatalogTest extends PostgresContainerTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("file_metadata를 참조하는 모든 외래 키가 정리 작업의 참조 목록에 있다")
    void cleanupReferencesMatchDatabaseForeignKeys() {
        List<Reference> foreignKeys = jdbcTemplate.query("""
                SELECT source.relname AS table_name, attribute.attname AS column_name
                FROM pg_constraint constraint_info
                JOIN pg_class source ON source.oid = constraint_info.conrelid
                JOIN pg_class target ON target.oid = constraint_info.confrelid
                JOIN pg_attribute attribute
                     ON attribute.attrelid = constraint_info.conrelid
                    AND attribute.attnum = ANY (constraint_info.conkey)
                WHERE constraint_info.contype = 'f'
                  AND target.relname = 'file_metadata'
                """, (rs, rowNum) -> new Reference(rs.getString("table_name"), rs.getString("column_name")));

        assertThat(FileReferences.ALL)
                .as("파일을 참조하는 컬럼을 추가했다면 FileReferences.ALL에도 추가해야 정리 작업이 사용 중인 파일을 지우지 않는다")
                .containsExactlyInAnyOrderElementsOf(foreignKeys);
    }
}

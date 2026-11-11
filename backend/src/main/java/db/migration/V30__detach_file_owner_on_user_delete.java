package db.migration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V30__detach_file_owner_on_user_delete extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws SQLException {
        Connection connection = context.getConnection();
        String constraintName = findOwnerConstraint(connection);
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE file_metadata ALTER COLUMN owner_id DROP NOT NULL");
            statement.execute("ALTER TABLE file_metadata DROP CONSTRAINT " + quoteIdentifier(constraintName));
            statement.execute("""
                    ALTER TABLE file_metadata ADD CONSTRAINT fk_file_metadata_owner
                    FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE SET NULL
                    """);
        }
    }

    private String findOwnerConstraint(Connection connection) throws SQLException {
        String tableName = connection.getMetaData().storesUpperCaseIdentifiers()
                ? "file_metadata".toUpperCase(Locale.ROOT) : "file_metadata";
        try (ResultSet keys = connection.getMetaData().getImportedKeys(
                connection.getCatalog(), connection.getSchema(), tableName)) {
            return findOwnerConstraint(keys);
        }
    }

    private String findOwnerConstraint(ResultSet keys) throws SQLException {
        List<String> constraintNames = new ArrayList<>();
        while (keys.next()) {
            constraintNames.add(ownerConstraintName(keys));
        }
        return constraintNames.stream().filter(Objects::nonNull).findFirst()
                .orElseThrow(() -> new SQLException("file_metadata.owner_id 외래 키를 찾을 수 없습니다."));
    }

    private String ownerConstraintName(ResultSet key) throws SQLException {
        return "owner_id".equalsIgnoreCase(key.getString("FKCOLUMN_NAME")) ? key.getString("FK_NAME") : null;
    }

    private String quoteIdentifier(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }
}

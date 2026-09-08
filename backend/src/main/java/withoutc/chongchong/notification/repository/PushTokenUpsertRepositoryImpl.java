package withoutc.chongchong.notification.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ConnectionCallback;

class PushTokenUpsertRepositoryImpl implements PushTokenUpsertRepository {

    private static final String POSTGRESQL_UPSERT_SQL = """
            INSERT INTO push_tokens (
                user_id,
                installation_id,
                provider,
                token,
                platform,
                is_active,
                created_at,
                updated_at
            )
            VALUES (?, ?, ?, ?, ?, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            ON CONFLICT (installation_id)
            DO UPDATE SET
                user_id = EXCLUDED.user_id,
                provider = EXCLUDED.provider,
                token = EXCLUDED.token,
                platform = EXCLUDED.platform,
                is_active = true,
                updated_at = CURRENT_TIMESTAMP
            """;

    private static final String H2_UPSERT_SQL = """
            MERGE INTO push_tokens AS target
            USING (
                VALUES (?, ?, ?, ?, ?)
            ) AS source (user_id, installation_id, provider, token, platform)
            ON target.installation_id = source.installation_id
            WHEN MATCHED THEN
                UPDATE SET
                    user_id = source.user_id,
                    provider = source.provider,
                    token = source.token,
                    platform = source.platform,
                    is_active = true,
                    updated_at = CURRENT_TIMESTAMP
            WHEN NOT MATCHED THEN
                INSERT (
                    user_id,
                    installation_id,
                    provider,
                    token,
                    platform,
                    is_active,
                    created_at,
                    updated_at
                )
                VALUES (
                    source.user_id,
                    source.installation_id,
                    source.provider,
                    source.token,
                    source.platform,
                    true,
                    CURRENT_TIMESTAMP,
                    CURRENT_TIMESTAMP
                )
            """;

    private final JdbcTemplate jdbcTemplate;
    private final boolean h2Database;

    PushTokenUpsertRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.h2Database = isH2Database();
    }

    @Override
    public void upsert(
            Long userId,
            String installationId,
            String provider,
            String token,
            String platform
    ) {
        jdbcTemplate.update(
                h2Database ? H2_UPSERT_SQL : POSTGRESQL_UPSERT_SQL,
                userId,
                installationId,
                provider,
                token,
                platform
        );
    }

    private boolean isH2Database() {
        return Boolean.TRUE.equals(jdbcTemplate.execute(
                (ConnectionCallback<Boolean>)
                connection -> "H2".equals(connection.getMetaData().getDatabaseProductName())
        ));
    }
}

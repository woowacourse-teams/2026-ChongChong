ALTER TABLE auth_sessions
    DROP CONSTRAINT uk_auth_sessions_user_id;

CREATE INDEX idx_auth_sessions_user_id
    ON auth_sessions (user_id);

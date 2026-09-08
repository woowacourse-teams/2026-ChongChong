-- A push token row represents the current registration for one app installation.
-- The authenticated user may change when the same installation logs in again.

ALTER TABLE push_tokens
    DROP CONSTRAINT IF EXISTS uk_push_tokens_user_installation_id;

ALTER TABLE push_tokens
    ADD CONSTRAINT uk_push_tokens_installation_id UNIQUE (installation_id);

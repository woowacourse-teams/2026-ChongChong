ALTER TABLE web_push_subscriptions
    ADD COLUMN installation_id VARCHAR(255);

DELETE FROM web_push_subscriptions;

ALTER TABLE web_push_subscriptions
    ALTER COLUMN installation_id SET NOT NULL;

ALTER TABLE web_push_subscriptions
    DROP CONSTRAINT IF EXISTS uk_web_push_subscriptions_endpoint;

ALTER TABLE web_push_subscriptions
    ADD CONSTRAINT uk_web_push_subscriptions_user_installation_id UNIQUE (user_id, installation_id);

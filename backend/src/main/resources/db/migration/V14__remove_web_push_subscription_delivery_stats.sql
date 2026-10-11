ALTER TABLE web_push_subscriptions
    DROP COLUMN last_success_at;

ALTER TABLE web_push_subscriptions
    DROP COLUMN last_failure_at;

ALTER TABLE web_push_subscriptions
    DROP COLUMN last_error;

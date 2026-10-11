-- created_at is written only by the native registration upsert, which used the UTC
-- PostgreSQL session timezone. A post-creation Seoul updated_at is at least created_at + 9 hours,
-- so earlier updated_at values are UTC; clamp values before created_at and shift the rest.
UPDATE web_push_subscriptions
SET updated_at = CASE
        WHEN updated_at < created_at
            THEN created_at + INTERVAL '9' HOUR
        WHEN updated_at < created_at + INTERVAL '9' HOUR
            THEN updated_at + INTERVAL '9' HOUR
        ELSE updated_at
    END,
    created_at = created_at + INTERVAL '9' HOUR
WHERE created_at IS NOT NULL;

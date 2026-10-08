-- created_at is written only by the native registration upsert, which used the UTC
-- PostgreSQL session timezone. updated_at may be UTC from native SQL or Seoul time from JPA
-- Auditing, so shift suspect earlier values and keep it no earlier than corrected created_at.
UPDATE web_push_subscriptions
SET updated_at = CASE
        WHEN updated_at < created_at + INTERVAL '9' HOUR
            THEN GREATEST(
                updated_at + INTERVAL '9' HOUR,
                created_at + INTERVAL '9' HOUR
            )
        ELSE updated_at
    END,
    created_at = created_at + INTERVAL '9' HOUR
WHERE created_at IS NOT NULL;

-- Preserve an available audit timestamp when repairing legacy rows. Rows missing both
-- timestamps receive the migration time because their original audit times are unknown.
UPDATE users
SET created_at = COALESCE(created_at, updated_at, (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Seoul')),
    updated_at = COALESCE(updated_at, created_at, (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Seoul'))
WHERE created_at IS NULL OR updated_at IS NULL;

ALTER TABLE users
    ALTER COLUMN created_at SET NOT NULL,
    ALTER COLUMN updated_at SET NOT NULL;

UPDATE studies
SET created_at = COALESCE(created_at, updated_at, (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Seoul')),
    updated_at = COALESCE(updated_at, created_at, (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Seoul'))
WHERE created_at IS NULL OR updated_at IS NULL;

ALTER TABLE studies
    ALTER COLUMN created_at SET NOT NULL,
    ALTER COLUMN updated_at SET NOT NULL;

UPDATE study_members
SET created_at = COALESCE(created_at, updated_at, (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Seoul')),
    updated_at = COALESCE(updated_at, created_at, (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Seoul'))
WHERE created_at IS NULL OR updated_at IS NULL;

ALTER TABLE study_members
    ALTER COLUMN created_at SET NOT NULL,
    ALTER COLUMN updated_at SET NOT NULL;

UPDATE assignments
SET created_at = COALESCE(created_at, updated_at, (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Seoul')),
    updated_at = COALESCE(updated_at, created_at, (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Seoul'))
WHERE created_at IS NULL OR updated_at IS NULL;

ALTER TABLE assignments
    ALTER COLUMN created_at SET NOT NULL,
    ALTER COLUMN updated_at SET NOT NULL;

UPDATE assignment_reminders
SET created_at = COALESCE(created_at, updated_at, (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Seoul')),
    updated_at = COALESCE(updated_at, created_at, (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Seoul'))
WHERE created_at IS NULL OR updated_at IS NULL;

ALTER TABLE assignment_reminders
    ALTER COLUMN created_at SET NOT NULL,
    ALTER COLUMN updated_at SET NOT NULL;

UPDATE assignment_submissions
SET created_at = COALESCE(created_at, updated_at, (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Seoul')),
    updated_at = COALESCE(updated_at, created_at, (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Seoul'))
WHERE created_at IS NULL OR updated_at IS NULL;

ALTER TABLE assignment_submissions
    ALTER COLUMN created_at SET NOT NULL,
    ALTER COLUMN updated_at SET NOT NULL;

UPDATE notices
SET created_at = COALESCE(created_at, updated_at, (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Seoul')),
    updated_at = COALESCE(updated_at, created_at, (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Seoul'))
WHERE created_at IS NULL OR updated_at IS NULL;

ALTER TABLE notices
    ALTER COLUMN created_at SET NOT NULL,
    ALTER COLUMN updated_at SET NOT NULL;

UPDATE notice_recipients
SET created_at = COALESCE(created_at, updated_at, (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Seoul')),
    updated_at = COALESCE(updated_at, created_at, (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Seoul'))
WHERE created_at IS NULL OR updated_at IS NULL;

ALTER TABLE notice_recipients
    ALTER COLUMN created_at SET NOT NULL,
    ALTER COLUMN updated_at SET NOT NULL;

UPDATE notice_reminders
SET created_at = COALESCE(created_at, updated_at, (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Seoul')),
    updated_at = COALESCE(updated_at, created_at, (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Seoul'))
WHERE created_at IS NULL OR updated_at IS NULL;

ALTER TABLE notice_reminders
    ALTER COLUMN created_at SET NOT NULL,
    ALTER COLUMN updated_at SET NOT NULL;

UPDATE notifications
SET created_at = COALESCE(created_at, updated_at, (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Seoul')),
    updated_at = COALESCE(updated_at, created_at, (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Seoul'))
WHERE created_at IS NULL OR updated_at IS NULL;

ALTER TABLE notifications
    ALTER COLUMN created_at SET NOT NULL,
    ALTER COLUMN updated_at SET NOT NULL;

UPDATE auth_sessions
SET created_at = COALESCE(created_at, updated_at, (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Seoul')),
    updated_at = COALESCE(updated_at, created_at, (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Seoul'))
WHERE created_at IS NULL OR updated_at IS NULL;

ALTER TABLE auth_sessions
    ALTER COLUMN created_at SET NOT NULL,
    ALTER COLUMN updated_at SET NOT NULL;

UPDATE social_accounts
SET created_at = COALESCE(created_at, updated_at, (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Seoul')),
    updated_at = COALESCE(updated_at, created_at, (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Seoul'))
WHERE created_at IS NULL OR updated_at IS NULL;

ALTER TABLE social_accounts
    ALTER COLUMN created_at SET NOT NULL,
    ALTER COLUMN updated_at SET NOT NULL;

UPDATE notification_deliveries
SET created_at = COALESCE(created_at, updated_at, (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Seoul')),
    updated_at = COALESCE(updated_at, created_at, (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Seoul'))
WHERE created_at IS NULL OR updated_at IS NULL;

ALTER TABLE notification_deliveries
    ALTER COLUMN created_at SET NOT NULL,
    ALTER COLUMN updated_at SET NOT NULL;

UPDATE web_push_subscriptions
SET created_at = COALESCE(created_at, updated_at, (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Seoul')),
    updated_at = COALESCE(updated_at, created_at, (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Seoul'))
WHERE created_at IS NULL OR updated_at IS NULL;

ALTER TABLE web_push_subscriptions
    ALTER COLUMN created_at SET NOT NULL,
    ALTER COLUMN updated_at SET NOT NULL;

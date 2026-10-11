-- Unify creation and submission notifications under the NEW type.

ALTER TABLE notifications
    DROP CONSTRAINT IF EXISTS notifications_type_check;

UPDATE notifications
SET type = 'NEW'
WHERE type IN ('CREATED', 'SUBMITTED');

ALTER TABLE notifications
    ADD CONSTRAINT notifications_type_check
        CHECK (type IN ('REMIND', 'NEW'));

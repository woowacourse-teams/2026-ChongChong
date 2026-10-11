-- Allow notifications for notice/assignment events and assignment submissions.

ALTER TABLE notifications
    DROP CONSTRAINT IF EXISTS notifications_resource_type_check;

ALTER TABLE notifications
    ADD CONSTRAINT notifications_resource_type_check
        CHECK (resource_type IN ('NOTICE', 'ASSIGNMENT', 'ASSIGNMENT_SUBMISSION'));

ALTER TABLE notifications
    DROP CONSTRAINT IF EXISTS notifications_type_check;

ALTER TABLE notifications
    ADD CONSTRAINT notifications_type_check
        CHECK (type IN ('REMIND', 'CREATED', 'SUBMITTED'));

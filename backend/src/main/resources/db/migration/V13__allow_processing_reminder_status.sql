ALTER TABLE assignment_reminders
    DROP CONSTRAINT assignment_reminders_status_check;

ALTER TABLE assignment_reminders
    ADD CONSTRAINT assignment_reminders_status_check
        CHECK (status IN ('PENDING', 'PROCESSING', 'SENT', 'FAILED'));

ALTER TABLE notice_reminders
    DROP CONSTRAINT notice_reminders_status_check;

ALTER TABLE notice_reminders
    ADD CONSTRAINT notice_reminders_status_check
        CHECK (status IN ('PENDING', 'PROCESSING', 'SENT', 'FAILED'));

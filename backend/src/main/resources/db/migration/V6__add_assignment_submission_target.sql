ALTER TABLE assignments
    ADD COLUMN submission_target VARCHAR(255) NOT NULL DEFAULT 'MEMBERS_ONLY';

ALTER TABLE assignments
    ADD CONSTRAINT assignments_submission_target_check
        CHECK (submission_target IN ('MEMBERS_ONLY', 'MEMBERS_AND_LEADER'));

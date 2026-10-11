ALTER TABLE assignments
    ADD COLUMN submission_visibility VARCHAR(255) NOT NULL DEFAULT 'LEADER_ONLY';

ALTER TABLE assignments
    ADD CONSTRAINT assignments_submission_visibility_check
        CHECK (submission_visibility IN ('LEADER_ONLY', 'ALL_STUDY_MEMBERS'));

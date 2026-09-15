-- Use submitted_at as the single source of truth for assignment submission state.

ALTER TABLE assignment_submissions
    DROP COLUMN submitted;

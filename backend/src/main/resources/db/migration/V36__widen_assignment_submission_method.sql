-- The API accepts submission methods up to 10,000 characters, but the baseline column held only 255.
ALTER TABLE assignments
    ALTER COLUMN submission_method TYPE TEXT;

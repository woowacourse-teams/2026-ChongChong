ALTER TABLE file_metadata
    ADD COLUMN delete_claimed_at TIMESTAMP;

ALTER TABLE file_metadata
    ADD COLUMN delete_attempts INT NOT NULL DEFAULT 0;

ALTER TABLE file_metadata
    ADD CONSTRAINT ck_file_metadata_delete_attempts CHECK (delete_attempts >= 0);

CREATE INDEX idx_file_metadata_status_created
    ON file_metadata (status, created_at, id);

ALTER TABLE studies
    ADD COLUMN image_file_id BIGINT REFERENCES file_metadata (id);

ALTER TABLE studies
    ADD CONSTRAINT uk_studies_image_file_id UNIQUE (image_file_id);

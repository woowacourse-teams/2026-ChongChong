ALTER TABLE users
    ADD COLUMN profile_image_file_id BIGINT REFERENCES file_metadata (id);

ALTER TABLE users
    ADD CONSTRAINT uk_users_profile_image_file_id UNIQUE (profile_image_file_id);

ALTER TABLE project
    ADD COLUMN invite_token VARCHAR(36) UNIQUE,
    ADD COLUMN invite_token_expires_at TIMESTAMP;

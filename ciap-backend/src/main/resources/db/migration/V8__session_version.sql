-- Session revocation. Existing rows keep version 0; previously issued
-- tokens do not carry this claim and are rejected on next use.
ALTER TABLE users
    ADD COLUMN token_version INT NOT NULL DEFAULT 0,
    ADD COLUMN refresh_token_id VARCHAR(64) NULL;

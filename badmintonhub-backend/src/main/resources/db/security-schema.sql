-- Run against the application database after the existing users table has been created.
-- This script is applied manually; Spring SQL initialization is not enabled automatically.
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id BINARY(16) NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    user_id BINARY(16) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    revoked_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_refresh_tokens_hash UNIQUE (token_hash),
    INDEX idx_refresh_tokens_user (user_id),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB;

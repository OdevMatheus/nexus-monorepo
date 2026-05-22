CREATE TABLE users (
                       id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                       name                        VARCHAR(255) NOT NULL,
                       email                       VARCHAR(255) NOT NULL UNIQUE,
                       password_hash               VARCHAR(255) NOT NULL,
                       email_verified              BOOLEAN NOT NULL DEFAULT FALSE,
                       email_verification_token    VARCHAR(255),
                       email_token_expires_at      TIMESTAMPTZ,
                       refresh_token               VARCHAR(512),
                       refresh_token_expires_at    TIMESTAMPTZ,
                       created_at                  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                       updated_at                  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_verification_token ON users(email_verification_token)
    WHERE email_verification_token IS NOT NULL;
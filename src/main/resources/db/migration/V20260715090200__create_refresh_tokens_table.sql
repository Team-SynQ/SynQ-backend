CREATE TABLE refresh_tokens (
    token_id    BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users (user_id) ON DELETE CASCADE,
    token       VARCHAR(500) NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 평문 토큰이 아니라 해시를 저장한다(DB 유출 시 바로 재사용되는 것을 막기 위함).
-- user_id 에 UNIQUE 를 걸어서 발급을 delete+insert 가 아니라 원자적 upsert(ON CONFLICT)로 처리한다.
CREATE TABLE refresh_tokens (
    token_id    BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL UNIQUE REFERENCES users (user_id) ON DELETE CASCADE,
    token_hash  VARCHAR(500) NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

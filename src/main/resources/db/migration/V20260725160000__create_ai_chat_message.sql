CREATE TABLE ai_chat_message (
    id                      BIGSERIAL PRIMARY KEY,
    meeting_id              BIGINT NOT NULL REFERENCES meeting (id) ON DELETE CASCADE,
    user_id                 BIGINT NOT NULL REFERENCES users (user_id),
    linked_segment_id       BIGINT,
    client_request_id       UUID NOT NULL,
    question                TEXT NOT NULL,
    answer                  TEXT,
    status                  VARCHAR(20) NOT NULL
                            CHECK (status IN ('GENERATING', 'COMPLETED', 'FAILED')),
    source_refs             JSONB NOT NULL DEFAULT '[]'::jsonb,
    suggested_questions     JSONB NOT NULL DEFAULT '[]'::jsonb,
    error_code              VARCHAR(50),
    error_message           TEXT,
    created_at              TIMESTAMP NOT NULL DEFAULT now(),
    updated_at              TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (meeting_id, user_id, client_request_id)
);

CREATE INDEX idx_ai_chat_message_history
    ON ai_chat_message (meeting_id, user_id, created_at DESC, id DESC);

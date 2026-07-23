-- 회의 중 AI가 현재까지 이해한 최신 맥락. 회의별로 한 행만 유지한다.
CREATE TABLE meeting_live_context (
    id                  BIGSERIAL PRIMARY KEY,
    meeting_id          BIGINT NOT NULL UNIQUE REFERENCES meeting (id),
    rolling_summary     TEXT NOT NULL,
    current_topic       TEXT,
    decisions           JSONB NOT NULL DEFAULT '[]'::jsonb,
    action_items        JSONB NOT NULL DEFAULT '[]'::jsonb,
    open_questions      JSONB NOT NULL DEFAULT '[]'::jsonb,
    last_segment_id     BIGINT NOT NULL,
    last_sequence_index INTEGER NOT NULL,
    version             BIGINT NOT NULL DEFAULT 0,
    created_at          TIMESTAMP NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE ai_summary_job (
    id               UUID PRIMARY KEY,
    meeting_id       BIGINT NOT NULL REFERENCES meeting (id),
    status           VARCHAR(20) NOT NULL,
    retry_count      INTEGER NOT NULL DEFAULT 0,
    model_name       VARCHAR(100),
    prompt_version   VARCHAR(50) NOT NULL,
    error_message    TEXT,
    started_at       TIMESTAMPTZ,
    completed_at     TIMESTAMPTZ,
    created_at       TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_ai_summary_job_meeting_created
    ON ai_summary_job (meeting_id, created_at DESC);

CREATE UNIQUE INDEX uq_ai_summary_job_active
    ON ai_summary_job (meeting_id)
    WHERE status IN ('QUEUED', 'PROCESSING');

CREATE TABLE meeting_summary (
    id               BIGSERIAL PRIMARY KEY,
    meeting_id       BIGINT NOT NULL REFERENCES meeting (id),
    job_id           UUID NOT NULL REFERENCES ai_summary_job (id),
    version          INTEGER NOT NULL,
    overall_summary  TEXT NOT NULL,
    key_topics       JSONB NOT NULL DEFAULT '[]'::jsonb,
    decisions        JSONB NOT NULL DEFAULT '[]'::jsonb,
    action_items     JSONB NOT NULL DEFAULT '[]'::jsonb,
    open_questions   JSONB NOT NULL DEFAULT '[]'::jsonb,
    generated_at     TIMESTAMPTZ NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (meeting_id, version)
);

CREATE INDEX idx_meeting_summary_meeting_version
    ON meeting_summary (meeting_id, version DESC);

CREATE TABLE personal_summary (
    id                    BIGSERIAL PRIMARY KEY,
    meeting_id            BIGINT NOT NULL REFERENCES meeting (id),
    job_id                UUID NOT NULL REFERENCES ai_summary_job (id),
    user_id               BIGINT NOT NULL REFERENCES users (user_id),
    version               INTEGER NOT NULL,
    role                  VARCHAR(100),
    personal_summary      TEXT NOT NULL,
    key_points            JSONB NOT NULL DEFAULT '[]'::jsonb,
    my_action_items       JSONB NOT NULL DEFAULT '[]'::jsonb,
    follow_up_questions   JSONB NOT NULL DEFAULT '[]'::jsonb,
    generated_at          TIMESTAMPTZ NOT NULL,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (meeting_id, user_id, version)
);

CREATE INDEX idx_personal_summary_meeting_user_version
    ON personal_summary (meeting_id, user_id, version DESC);

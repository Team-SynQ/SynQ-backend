-- 대면 회의 세션. 호스트는 별도 FK가 아니라 meeting_participant.role='HOST' 로 판별한다.
CREATE TABLE meeting (
    id          BIGSERIAL PRIMARY KEY,
    project_id  BIGINT NOT NULL,
    title       VARCHAR(255) NOT NULL,
    status      VARCHAR(20) NOT NULL
                CHECK (status IN ('IN_PROGRESS', 'SUMMARIZING', 'SUMMARIZED', 'SUMMARY_FAILED')),
    started_at  TIMESTAMP,
    ended_at    TIMESTAMP,
    created_at  TIMESTAMP NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_meeting_project_id ON meeting (project_id);

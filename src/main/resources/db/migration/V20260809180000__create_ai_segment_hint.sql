-- 3-hint 는 사용자의 역할·관점에 따라 내용이 달라진다. 저장 단위가 세그먼트가 아니라
-- (meeting, segment, user) 인 이유이고, 같은 사용자가 다시 클릭하면 자기 행만 덮어쓴다.
CREATE TABLE ai_segment_hint (
    id            BIGSERIAL PRIMARY KEY,
    meeting_id    BIGINT NOT NULL REFERENCES meeting (id) ON DELETE CASCADE,
    segment_id    BIGINT NOT NULL REFERENCES transcript_segment (id) ON DELETE CASCADE,
    -- 힌트는 그 사용자에게만 보이는 개인 데이터라 사용자가 사라지면 같이 사라진다.
    user_id       BIGINT NOT NULL REFERENCES users (user_id) ON DELETE CASCADE,
    meaning       TEXT NOT NULL,
    my_impact     TEXT NOT NULL,
    team_question TEXT NOT NULL,
    created_at    TIMESTAMP NOT NULL DEFAULT now(),
    updated_at    TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (meeting_id, segment_id, user_id)
);

-- 회의 기록 화면이 (meeting_id, user_id) 로만 훑는다. meeting_id 는 segment_id 로 유도
-- 가능하지만, 정규화하면 이 조회마다 transcript_segment 조인이 붙어서 비정규화해 둔다.
CREATE INDEX idx_ai_segment_hint_lookup ON ai_segment_hint (meeting_id, user_id);

-- 온보딩에서 유저가 고르는 관심 관점(다중 선택)
CREATE TABLE user_perspectives (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT NOT NULL REFERENCES users (user_id) ON DELETE CASCADE,
    perspective  VARCHAR(30) NOT NULL
        CHECK (perspective IN (
            'SCHEDULE', 'SCOPE', 'DECISION', 'UX', 'TECH_RISK', 'COST_PERFORMANCE',
            'CUSTOMER_REACTION', 'OPERATION_ISSUE', 'ACTION_ITEM', 'TEAM_QUESTION'
        )),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, perspective)
);

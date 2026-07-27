-- 역할·관점 프로필 하나당 최대 3개까지 선택 가능한 관심 관점(다중 선택). 개수 제한(3개)은 애플리케이션에서 검증한다.
CREATE TABLE role_profile_perspectives (
    id               BIGSERIAL PRIMARY KEY,
    role_profile_id  BIGINT NOT NULL REFERENCES role_profiles (id) ON DELETE CASCADE,
    perspective      VARCHAR(30) NOT NULL
        CHECK (perspective IN (
            'SCHEDULE', 'SCOPE', 'DECISION', 'UX', 'TECH_RISK', 'COST_PERFORMANCE',
            'CUSTOMER_REACTION', 'OPERATION_ISSUE', 'ACTION_ITEM', 'TEAM_QUESTION'
        )),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (role_profile_id, perspective)
);

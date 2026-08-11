CREATE TABLE project_join_request_perspectives (
    id               BIGSERIAL PRIMARY KEY,
    join_request_id  BIGINT NOT NULL REFERENCES project_join_request (id) ON DELETE CASCADE,
    perspective      VARCHAR(30) NOT NULL
        CHECK (perspective IN (
            'SCHEDULE', 'SCOPE', 'DECISION', 'UX', 'TECH_RISK', 'COST_PERFORMANCE',
            'CUSTOMER_REACTION', 'OPERATION_ISSUE', 'ACTION_ITEM', 'TEAM_QUESTION'
        )),
    created_at       TIMESTAMP NOT NULL DEFAULT now(),
    updated_at       TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (join_request_id, perspective)
);

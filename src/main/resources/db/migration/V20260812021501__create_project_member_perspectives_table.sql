CREATE TABLE project_member_perspectives (
    id                 BIGSERIAL PRIMARY KEY,
    project_member_id  BIGINT NOT NULL REFERENCES project_member (id) ON DELETE CASCADE,
    perspective        VARCHAR(30) NOT NULL
        CHECK (perspective IN (
            'SCHEDULE', 'SCOPE', 'DECISION', 'UX', 'TECH_RISK', 'COST_PERFORMANCE',
            'CUSTOMER_REACTION', 'OPERATION_ISSUE', 'ACTION_ITEM', 'TEAM_QUESTION'
        )),
    created_at         TIMESTAMP NOT NULL DEFAULT now(),
    updated_at         TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (project_member_id, perspective)
);

CREATE INDEX idx_project_member_perspectives_member_id
    ON project_member_perspectives (project_member_id);

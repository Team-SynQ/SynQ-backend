CREATE TABLE project_join_request (
    id              BIGSERIAL PRIMARY KEY,
    project_id      BIGINT NOT NULL REFERENCES project (id) ON DELETE CASCADE,
    user_id         BIGINT NOT NULL REFERENCES users (user_id),
    status          VARCHAR(20) NOT NULL
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    setting_source  VARCHAR(20) NOT NULL
        CHECK (setting_source IN ('DEFAULT', 'ONBOARDING', 'PROJECT_CUSTOM')),
    role            VARCHAR(30) NOT NULL
        CHECK (role IN (
            'PLANNING_OPERATION', 'DESIGN_CONTENT', 'DEV_TECH', 'MARKETING_BRANDING',
            'SALES_CUSTOMER', 'DATA_RESEARCH', 'STRATEGY_MANAGEMENT', 'ETC'
        )),
    detail_role     VARCHAR(30),
    requested_at    TIMESTAMPTZ NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_project_join_request_project_status
    ON project_join_request (project_id, status, requested_at, id);

CREATE INDEX idx_project_join_request_user_id
    ON project_join_request (user_id);

CREATE UNIQUE INDEX uq_project_join_request_pending
    ON project_join_request (project_id, user_id)
    WHERE status = 'PENDING';

CREATE TABLE project_member (
    id          BIGSERIAL PRIMARY KEY,
    project_id  BIGINT NOT NULL REFERENCES project (id) ON DELETE CASCADE,
    user_id     BIGINT NOT NULL REFERENCES users (user_id),
    role        VARCHAR(10) NOT NULL CHECK (role IN ('OWNER', 'MEMBER')),
    joined_at   TIMESTAMP NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (project_id, user_id)
);

CREATE INDEX idx_project_member_project_id ON project_member (project_id);
CREATE INDEX idx_project_member_user_id ON project_member (user_id);

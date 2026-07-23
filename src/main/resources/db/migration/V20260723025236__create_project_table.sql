CREATE TABLE project (
    id                          BIGSERIAL PRIMARY KEY,
    owner_id                    BIGINT NOT NULL REFERENCES users (user_id),
    title                       VARCHAR(30) NOT NULL,
    description                 VARCHAR(500),
    created_at                  TIMESTAMP NOT NULL DEFAULT now(),
    updated_at                  TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_project_owner_id ON project (owner_id);

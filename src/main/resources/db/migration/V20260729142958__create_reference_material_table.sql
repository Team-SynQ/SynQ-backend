CREATE TABLE reference_material (
    id              BIGSERIAL PRIMARY KEY,
    project_id      BIGINT NOT NULL REFERENCES project (id) ON DELETE CASCADE,
    uploader_id     BIGINT NOT NULL REFERENCES users (user_id),
    type            VARCHAR(10) NOT NULL CHECK (type IN ('FILE', 'LINK')),
    name            VARCHAR(255) NOT NULL,
    url             VARCHAR(2000),
    file_size       BIGINT,
    file_extension  VARCHAR(10) CHECK (
        file_extension IS NULL OR file_extension IN ('PDF', 'DOCX', 'PPTX', 'TXT')
    ),
    status          VARCHAR(20) NOT NULL CHECK (
        status IN ('UPLOADING', 'AVAILABLE', 'READ_FAILED')
    ),
    deleted_at      TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now(),
    CHECK (
        (type = 'FILE' AND url IS NULL AND file_size IS NOT NULL AND file_extension IS NOT NULL)
        OR
        (type = 'LINK' AND url IS NOT NULL AND file_size IS NULL AND file_extension IS NULL)
    )
);

CREATE INDEX idx_reference_material_project_created
    ON reference_material (project_id, created_at DESC, id DESC);

ALTER TABLE document_chunk
    ADD CONSTRAINT fk_document_chunk_reference_material
    FOREIGN KEY (reference_material_id)
    REFERENCES reference_material (id)
    ON DELETE CASCADE;

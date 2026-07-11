-- 참고자료를 청킹한 조각과 임베딩 벡터.
--
-- reference_material_id 는 JPA 연관관계 없이 단순 BIGINT
-- ai/rag 가 reference 패키지를 모르게 해야 회의 트랜스크립트 청킹에 같은 코드를 재사용할 수 있다.
-- FK 제약(ON DELETE CASCADE)은 reference_material 테이블이 생긴 뒤 별도 마이그레이션으로 추가한다.
CREATE TABLE document_chunk (
    id                    BIGSERIAL PRIMARY KEY,
    reference_material_id BIGINT NOT NULL,
    chunk_index           INT NOT NULL,
    content               TEXT NOT NULL,
    -- 청킹·임베딩·저장이 한 트랜잭션이라 임베딩 없는 청크는 존재하지 않는다.
    -- NOT NULL 이 그 불변식을 DB 레벨에서 강제한다.
    embedding             vector(768) NOT NULL,
    -- 모델을 바꾸면 기존 벡터와 새 벡터를 같은 공간에서 비교할 수 없다.
    embedding_model       TEXT NOT NULL,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- 재처리 시 중복 청크를 DB 가 막는다.
    UNIQUE (reference_material_id, chunk_index)
);

CREATE INDEX idx_document_chunk_material
    ON document_chunk (reference_material_id);

-- 코사인 유사도 검색용. 검색 기능은 아직 없음.
CREATE INDEX idx_document_chunk_embedding
    ON document_chunk USING hnsw (embedding vector_cosine_ops);

-- 회의 전사를 청킹한 조각과 임베딩 벡터.
--
-- document_chunk 와 달리 meeting 에 FK 를 건다.
-- content 는 transcript_segment 와 중복 저장이다.
CREATE TABLE meeting_transcript_chunk (
    id              BIGSERIAL PRIMARY KEY,
    meeting_id      BIGINT NOT NULL REFERENCES meeting (id) ON DELETE CASCADE,
    -- 검색 스코프. 조인 필터링이 HNSW 인덱스 사용을 방해하므로 의도적으로 비정규화했다.
    project_id      BIGINT NOT NULL,
    chunk_index     INT NOT NULL,
    content         TEXT NOT NULL,
    -- 청킹·임베딩·저장이 한 트랜잭션이라 임베딩 없는 청크는 존재하지 않는다.
    embedding       vector(768) NOT NULL,
    -- 모델을 바꾸면 기존 벡터와 새 벡터를 같은 공간에서 비교할 수 없다.
    embedding_model TEXT NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- 재인덱싱 시 중복 청크를 DB 가 막는다.
    UNIQUE (meeting_id, chunk_index)
);

CREATE INDEX idx_meeting_transcript_chunk_embedding
    ON meeting_transcript_chunk USING hnsw (embedding vector_cosine_ops);

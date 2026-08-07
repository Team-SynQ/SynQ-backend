-- 회의 전사 인덱싱의 진행/결과. ai/rag 가 소유한다.
CREATE TABLE meeting_transcript_index_status (
    -- 회의당 한 행. 재인덱싱은 이 행을 갱신한다.
    meeting_id     BIGINT PRIMARY KEY REFERENCES meeting (id) ON DELETE CASCADE,
    project_id     BIGINT NOT NULL,
    -- PROCESSING 은 참고자료와 달리 실제로 쓴다. 인덱싱이 비동기라 서버가 죽으면
    -- 이 상태로 남은 행이 곧 미완료 목록이 된다.
    -- SKIPPED 는 녹음이 없는 회의다. 이게 없으면 정상 회의가 실패 목록에 섞인다.
    status         VARCHAR(20) NOT NULL CHECK (
        status IN ('PROCESSING', 'COMPLETED', 'FAILED', 'SKIPPED')
    ),
    -- 참고자료와 달리 사유를 컬럼에 남긴다. 이 테이블의 존재 이유가 화면 노출이 아니라
    -- 관리자 복구이기 때문이다.
    failure_reason TEXT,
    chunk_count    INT NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 복구 대상 조회용. FAILED 는 전체 회의 중 소수라 부분 인덱스로 둔다.
CREATE INDEX idx_meeting_transcript_index_status_failed
    ON meeting_transcript_index_status (status) WHERE status = 'FAILED';

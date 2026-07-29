-- STT 가 확정한 전사 세그먼트. 중간 전사(interim)는 저장하지 않고 확정분만 남긴다.
-- start_ms/end_ms 는 Soniox 스트림 기준이 아니라 meeting.started_at 을 0 점으로 하는 회의 절대 시각이다.
-- (호스트가 재연결하면 Soniox 타임스탬프가 0 으로 리셋되므로 서버가 오프셋을 더해 저장한다.)
CREATE TABLE transcript_segment (
    id             BIGSERIAL PRIMARY KEY,
    meeting_id     BIGINT NOT NULL REFERENCES meeting (id) ON DELETE CASCADE,
    sequence_index INTEGER NOT NULL,
    start_ms       INTEGER NOT NULL,
    end_ms         INTEGER NOT NULL,
    content        TEXT NOT NULL CHECK (char_length(content) > 0),
    -- 화자분리는 MVP 미구현. 확장성을 위해 컬럼만 두고 항상 NULL 로 저장한다.
    speaker_label  VARCHAR(50),
    created_at     TIMESTAMP NOT NULL DEFAULT now(),
    updated_at     TIMESTAMP NOT NULL DEFAULT now()
);

-- 재연결/중복 저장으로 순번이 어긋나는 것을 DB 레벨에서 막는다.
CREATE UNIQUE INDEX uq_transcript_segment_meeting_sequence
    ON transcript_segment (meeting_id, sequence_index);

-- 전사 조회는 항상 start_ms ASC, sequence_index ASC 로 정렬한다.
CREATE INDEX idx_transcript_segment_meeting_start
    ON transcript_segment (meeting_id, start_ms, sequence_index);

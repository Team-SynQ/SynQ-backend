-- 회의 오디오 원본 녹음 세그먼트. 호스트 WS 세션 하나(webm 스트림 하나)가 세그먼트 하나에 대응한다.
-- 재연결이 없으면 회의당 세그먼트 1개, 재연결이 있으면 여러 개가 생기고 순서대로 이어 재생한다.
CREATE TABLE meeting_recording_segment (
    id          BIGSERIAL PRIMARY KEY,
    meeting_id  BIGINT NOT NULL REFERENCES meeting (id) ON DELETE CASCADE,
    storage_key VARCHAR(255) NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_meeting_recording_segment_meeting_id ON meeting_recording_segment (meeting_id);

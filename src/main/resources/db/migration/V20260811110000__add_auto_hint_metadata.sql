-- Blue-Green 배포 중 구버전도 ai_segment_hint INSERT를 계속할 수 있으므로,
-- 신규 컬럼은 기본값 또는 NULL 허용으로 추가한다. 기존 수동 힌트는 MANUAL로 해석된다.
ALTER TABLE ai_segment_hint
    ADD COLUMN source VARCHAR(10) NOT NULL DEFAULT 'MANUAL',
    ADD COLUMN importance INTEGER NULL,
    ADD COLUMN trigger_reason TEXT NULL;

CREATE INDEX idx_ai_segment_hint_auto_lookup
    ON ai_segment_hint (meeting_id, source, segment_id);

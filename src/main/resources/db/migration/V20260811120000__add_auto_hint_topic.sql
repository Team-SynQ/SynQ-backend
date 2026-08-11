-- 기존 애플리케이션은 새 컬럼을 참조하지 않으므로 Blue-Green 배포 중에도 안전하다.
ALTER TABLE ai_segment_hint
    ADD COLUMN topic TEXT NULL;

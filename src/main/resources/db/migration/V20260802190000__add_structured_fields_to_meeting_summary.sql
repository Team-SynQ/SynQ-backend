-- Blue-Green 배포 중 구버전이 기존 컬럼을 계속 읽을 수 있도록 신규 컬럼만 추가한다.
ALTER TABLE meeting_summary
    ADD COLUMN one_line_summary TEXT,
    ADD COLUMN discussion_sections JSONB,
    ADD COLUMN tentative_directions JSONB,
    ADD COLUMN confirmation_items JSONB;

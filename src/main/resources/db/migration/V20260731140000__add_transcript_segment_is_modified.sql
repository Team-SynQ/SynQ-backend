-- 사용자가 오타/오인식 교정을 위해 직접 수정한 세그먼트인지 구분한다.
ALTER TABLE transcript_segment
    ADD COLUMN is_modified BOOLEAN NOT NULL DEFAULT false;

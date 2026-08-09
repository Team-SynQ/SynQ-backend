-- 회의 삭제 시 자식 데이터가 고아로 남지 않도록, 아직 CASCADE 가 없던 meeting FK 에 ON DELETE CASCADE 를 추가한다.
-- transcript_segment/meeting_transcript_chunk/meeting_transcript_index_status 는 이미 CASCADE 로 생성되어 있어 대상에서 제외한다.
ALTER TABLE meeting_participant
    DROP CONSTRAINT meeting_participant_meeting_id_fkey,
    ADD CONSTRAINT meeting_participant_meeting_id_fkey
        FOREIGN KEY (meeting_id) REFERENCES meeting (id) ON DELETE CASCADE;

ALTER TABLE meeting_live_context
    DROP CONSTRAINT meeting_live_context_meeting_id_fkey,
    ADD CONSTRAINT meeting_live_context_meeting_id_fkey
        FOREIGN KEY (meeting_id) REFERENCES meeting (id) ON DELETE CASCADE;

ALTER TABLE ai_summary_job
    DROP CONSTRAINT ai_summary_job_meeting_id_fkey,
    ADD CONSTRAINT ai_summary_job_meeting_id_fkey
        FOREIGN KEY (meeting_id) REFERENCES meeting (id) ON DELETE CASCADE;

ALTER TABLE meeting_summary
    DROP CONSTRAINT meeting_summary_meeting_id_fkey,
    ADD CONSTRAINT meeting_summary_meeting_id_fkey
        FOREIGN KEY (meeting_id) REFERENCES meeting (id) ON DELETE CASCADE;

ALTER TABLE personal_summary
    DROP CONSTRAINT personal_summary_meeting_id_fkey,
    ADD CONSTRAINT personal_summary_meeting_id_fkey
        FOREIGN KEY (meeting_id) REFERENCES meeting (id) ON DELETE CASCADE;

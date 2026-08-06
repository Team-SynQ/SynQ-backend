ALTER TABLE ai_summary_job
    ALTER COLUMN status TYPE VARCHAR(30);

ALTER TABLE ai_summary_job
    ADD COLUMN failed_personal_summary_count INTEGER NOT NULL DEFAULT 0;

ALTER TABLE ai_summary_job
    ADD CONSTRAINT chk_ai_summary_job_failed_personal_summary_count
        CHECK (failed_personal_summary_count >= 0);

ALTER TABLE ai_summary_job
    ALTER COLUMN status TYPE VARCHAR(30);

ALTER TABLE ai_summary_job
    ADD COLUMN failed_personal_summary_count INTEGER NOT NULL DEFAULT 0;

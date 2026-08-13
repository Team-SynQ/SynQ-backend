-- 회의 일시정지/재개 시간 동기화를 위한 컬럼. paused_at 이 non-null 이면 현재 일시정지 상태다.
-- active_seconds_snapshot 은 마지막 정지/재개 전환 시점까지 누적된 활성 시간(초)이고,
-- last_resumed_at 은 그 이후 다시 흐르기 시작한 기준 시각이다.
ALTER TABLE meeting
    ADD COLUMN paused_at TIMESTAMP NULL,
    ADD COLUMN last_resumed_at TIMESTAMP NULL,
    ADD COLUMN active_seconds_snapshot BIGINT NOT NULL DEFAULT 0;

-- 기존 회의는 시작 시각부터 지금까지 계속 활성 상태였던 것으로 백필한다.
UPDATE meeting SET last_resumed_at = started_at WHERE last_resumed_at IS NULL;

ALTER TABLE meeting ALTER COLUMN last_resumed_at SET NOT NULL;

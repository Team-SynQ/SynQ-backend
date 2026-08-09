-- 프로젝트당 진행 중인(IN_PROGRESS) 회의는 동시에 하나만 존재해야 한다.
-- 애플리케이션 레벨 존재 체크만으로는 동시 생성 요청 사이에 TOCTOU 레이스가 발생할 수 있어
-- DB 제약(partial unique index)으로 원자적으로 보장한다.
CREATE UNIQUE INDEX uq_meeting_project_active
    ON meeting (project_id)
    WHERE status = 'IN_PROGRESS';

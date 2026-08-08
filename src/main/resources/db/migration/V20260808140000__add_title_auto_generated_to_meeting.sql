-- 사용자가 제목을 직접 수정한 회의를 표시한다. true(기본값)면 생성 시 부여된 임시 제목이며,
-- 추후 AI 자동 제목 생성 기능이 이 값을 참고해 사용자가 수정한 제목은 덮어쓰지 않는다.
ALTER TABLE meeting ADD COLUMN title_auto_generated BOOLEAN NOT NULL DEFAULT true;

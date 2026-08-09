-- 회의 생성 시 동시 요청 레이스로 프로젝트당 IN_PROGRESS 회의가 중복 생성된 사례가 있어
-- 다음 마이그레이션(uq_meeting_project_active)이 적용되기 전에 정리한다.
-- 프로젝트별 가장 최근에 생성된 IN_PROGRESS 회의만 남기고 나머지는 SUMMARY_FAILED 처리한다.
UPDATE meeting m
SET status = 'SUMMARY_FAILED'
WHERE m.status = 'IN_PROGRESS'
  AND EXISTS (
    SELECT 1
    FROM meeting m2
    WHERE m2.project_id = m.project_id
      AND m2.status = 'IN_PROGRESS'
      AND (m2.created_at, m2.id) > (m.created_at, m.id)
  );

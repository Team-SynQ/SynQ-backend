-- 역할·관점을 유저당 1개(User.role/detail_role + user_perspectives)에서
-- 여러 개의 프로필(role_profiles)로 구조를 바꾼다. 아직 실제로 쓰인 적 없는 컬럼/테이블이라 바로 제거한다.
DROP TABLE IF EXISTS user_perspectives;
ALTER TABLE users DROP COLUMN IF EXISTS role;
ALTER TABLE users DROP COLUMN IF EXISTS detail_role;
ALTER TABLE users DROP COLUMN IF EXISTS onboarding_completed_at;

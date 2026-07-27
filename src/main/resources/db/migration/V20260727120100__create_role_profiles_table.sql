-- 유저가 여러 개 가질 수 있는 역할·관점 프로필. 그중 하나가 기본(is_default)이며,
-- 기본 프로필이 하나도 없으면(=한 번도 설정 안 함) 온보딩 대상이 된다.
CREATE TABLE role_profiles (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT NOT NULL REFERENCES users (user_id) ON DELETE CASCADE,
    role         VARCHAR(30) NOT NULL
        CHECK (role IN (
            'PLANNING_OPERATION', 'DESIGN_CONTENT', 'DEV_TECH', 'MARKETING_BRANDING',
            'SALES_CUSTOMER', 'DATA_RESEARCH', 'STRATEGY_MANAGEMENT', 'ETC'
        )),
    detail_role  VARCHAR(30),
    is_default   BOOLEAN NOT NULL DEFAULT false,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_role_profiles_user_id ON role_profiles (user_id);
-- 유저당 기본 프로필은 최대 하나만 존재해야 한다(부분 유니크 인덱스).
CREATE UNIQUE INDEX idx_role_profiles_user_default ON role_profiles (user_id) WHERE is_default = true;

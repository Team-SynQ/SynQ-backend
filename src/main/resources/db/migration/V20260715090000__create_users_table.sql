-- 소셜 로그인(GOOGLE/KAKAO/NAVER) + 개발 편의용 이메일 로그인(LOCAL)을 지원한다.
-- 계정 식별 기준은 (provider, provider_id) 이지 email 이 아니다.
-- 같은 이메일이라도 provider 가 다르면 별개 계정으로 취급한다(AUTH-04).
CREATE TABLE users (
    user_id                  BIGSERIAL PRIMARY KEY,
    name                     VARCHAR(20) NOT NULL,
    email                    VARCHAR(255) NOT NULL,
    -- LOCAL(dev 전용 이메일 로그인)에서만 사용. 소셜 로그인 유저는 NULL.
    password_hash            VARCHAR(255),
    provider                 VARCHAR(20) NOT NULL
        CHECK (provider IN ('LOCAL', 'GOOGLE', 'KAKAO', 'NAVER')),
    -- LOCAL 은 provider_id 가 없다.
    provider_id              VARCHAR(255),
    role                     VARCHAR(30)
        CHECK (role IS NULL OR role IN (
            'PLANNING_OPERATION', 'DESIGN_CONTENT', 'DEV_TECH', 'MARKETING_BRANDING',
            'SALES_CUSTOMER', 'DATA_RESEARCH', 'STRATEGY_MANAGEMENT', 'ETC'
        )),
    detail_role              VARCHAR(30),
    onboarding_completed_at  TIMESTAMPTZ,
    created_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- provider_id 가 NULL 인 행끼리는(LOCAL) 서로 충돌하지 않는다(Postgres UNIQUE 는 NULL을 서로 다른 값으로 취급).
    UNIQUE (provider, provider_id)
);

-- LOCAL(dev 전용) 계정끼리만 이메일 중복 가입을 막는다. 소셜 계정 간에는 이메일 중복 허용(AUTH-04).
CREATE UNIQUE INDEX idx_users_email_local ON users (email) WHERE provider = 'LOCAL';

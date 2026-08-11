ALTER TABLE project_member
    ADD COLUMN use_default BOOLEAN NOT NULL DEFAULT true,
    ADD COLUMN role_category VARCHAR(30),
    ADD COLUMN detail_role VARCHAR(30),
    ADD CONSTRAINT chk_project_member_role_category
        CHECK (role_category IS NULL OR role_category IN (
            'PLANNING_OPERATION', 'DESIGN_CONTENT', 'DEV_TECH', 'MARKETING_BRANDING',
            'SALES_CUSTOMER', 'DATA_RESEARCH', 'STRATEGY_MANAGEMENT', 'ETC'
        )),
    ADD CONSTRAINT chk_project_member_custom_role
        CHECK (use_default OR role_category IS NOT NULL);

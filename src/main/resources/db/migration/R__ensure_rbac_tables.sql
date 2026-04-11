-- 可重复：在 Python 已占库、或 V1 被 baseline 跳过导致未执行时，仍保证 Spring 所需 RBAC 表存在
CREATE TABLE IF NOT EXISTS roles (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(128) NOT NULL
);

CREATE TABLE IF NOT EXISTS permissions (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(128) NOT NULL
);

CREATE TABLE IF NOT EXISTS role_permissions (
    role_id BIGINT NOT NULL REFERENCES roles (id) ON DELETE CASCADE,
    permission_id BIGINT NOT NULL REFERENCES permissions (id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE IF NOT EXISTS app_users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 与 V4 一致；可重复执行，避免 baseline/repair 导致版本已记录但表未建时 Hibernate validate 失败
CREATE TABLE IF NOT EXISTS app_user_phones (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES app_users (id) ON DELETE CASCADE,
    mobile_cn VARCHAR(11) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_app_user_phones_user_mobile UNIQUE (user_id, mobile_cn),
    CONSTRAINT uq_app_user_phones_mobile UNIQUE (mobile_cn)
);

CREATE INDEX IF NOT EXISTS idx_app_user_phones_user ON app_user_phones (user_id);

CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL REFERENCES app_users (id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles (id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

INSERT INTO roles (code, name) VALUES
    ('ADMIN', '管理员'),
    ('ANALYST', '分析员'),
    ('VIEWER', '只读')
ON CONFLICT (code) DO NOTHING;

INSERT INTO permissions (code, name) VALUES
    ('USER_ADMIN', '用户与角色管理'),
    ('IMPORT_XLSX', '导入微信账单 xlsx'),
    ('BKP_TX_CRUD', '备份表明细增删改查'),
    ('ANALYTICS', '收支分析查询')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'ADMIN'
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code IN ('IMPORT_XLSX', 'BKP_TX_CRUD', 'ANALYTICS')
WHERE r.code = 'ANALYST'
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code = 'ANALYTICS'
WHERE r.code = 'VIEWER'
ON CONFLICT (role_id, permission_id) DO NOTHING;

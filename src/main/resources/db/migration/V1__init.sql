-- =============================================================================
-- PostgreSQL 全量初始化 —— 系统的唯一一份 DDL，已合并所有迁移逻辑。
-- 无外键，仅靠应用层业务逻辑维护引用完整性。
--
-- 启动方式（任选其一）：
--   方式一：启动应用，由 Flyway 自动执行（classpath:db/migration）。
--   方式二：空库或已有表时均可整文件执行（DDL 使用 IF NOT EXISTS）。
--   方式三：清空重建：DROP SCHEMA public CASCADE; CREATE SCHEMA public;
--           GRANT ALL ON SCHEMA public TO postgres; GRANT ALL ON SCHEMA public TO public;
--
-- 订单/流水：交易单号 trade_no 非空时全局唯一（部分唯一索引）。
-- =============================================================================

-- ========== RBAC ==========
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
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE IF NOT EXISTS app_users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    password_plain VARCHAR(256),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id)
);

-- ========== 自然人 / 号码（多对多）==========
CREATE TABLE IF NOT EXISTS phone_number (
    id BIGSERIAL PRIMARY KEY,
    mobile_cn VARCHAR(11) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS person (
    id BIGSERIAL PRIMARY KEY,
    display_name TEXT,
    phone_id BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS person_phone (
    id BIGSERIAL PRIMARY KEY,
    person_id BIGINT NOT NULL,
    phone_id  BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_person_phone_pair UNIQUE (person_id, phone_id)
);

CREATE INDEX IF NOT EXISTS idx_person_phone_phone ON person_phone (phone_id);

-- 登录账号绑定手机号（多对多）
CREATE TABLE IF NOT EXISTS user_phone (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    mobile_cn VARCHAR(11) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_user_phone_user_mobile UNIQUE (user_id, mobile_cn)
);

CREATE INDEX IF NOT EXISTS idx_user_phone_user ON user_phone (user_id);

-- ========== 微信用户（关联 person）==========
CREATE TABLE IF NOT EXISTS wechat_users (
    id BIGSERIAL PRIMARY KEY,
    wechat_nickname TEXT NOT NULL,
    person_id BIGINT,
    phone_id BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by TEXT,
    updated_by TEXT,
    extra_text TEXT,
    is_archived BOOLEAN NOT NULL DEFAULT FALSE
);

-- ========== 支付宝用户（关联 person）==========
CREATE TABLE IF NOT EXISTS alipay_users (
    id BIGSERIAL PRIMARY KEY,
    person_id BIGINT NOT NULL,
    alipay_nickname TEXT NOT NULL,
    mobile_cn VARCHAR(11),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by TEXT,
    updated_by TEXT,
    extra_text TEXT,
    is_archived BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_alipay_users_person ON alipay_users (person_id);

-- ========== 微信账单导入 / 明细 ==========
CREATE TABLE IF NOT EXISTS wechat_bill_imports (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    person_id BIGINT,
    phone_id BIGINT,
    mobile_cn VARCHAR(11),
    source_file TEXT NOT NULL,
    export_type TEXT,
    export_time TIMESTAMPTZ,
    range_start TIMESTAMPTZ,
    range_end TIMESTAMPTZ,
    total_count INTEGER,
    income_count INTEGER,
    income_amount NUMERIC(18, 4),
    expense_count INTEGER,
    expense_amount NUMERIC(18, 4),
    neutral_count INTEGER,
    neutral_amount NUMERIC(18, 4),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by TEXT,
    updated_by TEXT,
    extra_text TEXT,
    is_archived BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_wx_import_user ON wechat_bill_imports (user_id);

CREATE TABLE IF NOT EXISTS wechat_bill_transactions (
    id BIGSERIAL PRIMARY KEY,
    bill_import_id BIGINT NOT NULL,
    person_id BIGINT,
    phone_id BIGINT,
    mobile_cn VARCHAR(11),
    row_hash CHAR(64) NOT NULL,
    trade_time TEXT,
    trade_type TEXT,
    counterparty TEXT,
    product TEXT,
    income_expense TEXT,
    amount_yuan NUMERIC(18, 4),
    payment_method TEXT,
    status TEXT,
    trade_no TEXT,
    merchant_no TEXT,
    remark TEXT,
    source_file TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by TEXT,
    updated_by TEXT,
    extra_text TEXT,
    is_archived BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_wx_tx_import ON wechat_bill_transactions (bill_import_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_wx_tx_trade_no ON wechat_bill_transactions (trade_no)
    WHERE trade_no IS NOT NULL AND btrim(trade_no) <> '';

-- ========== 支付宝账单导入 / 明细 ==========
CREATE TABLE IF NOT EXISTS alipay_bill_imports (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    person_id BIGINT,
    phone_id BIGINT,
    mobile_cn VARCHAR(11),
    source_file TEXT NOT NULL,
    export_type TEXT,
    export_time TIMESTAMPTZ,
    range_start TIMESTAMPTZ,
    range_end TIMESTAMPTZ,
    total_count INTEGER,
    income_count INTEGER,
    income_amount NUMERIC(18, 4),
    expense_count INTEGER,
    expense_amount NUMERIC(18, 4),
    neutral_count INTEGER,
    neutral_amount NUMERIC(18, 4),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by TEXT,
    updated_by TEXT,
    extra_text TEXT,
    is_archived BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_ali_import_user ON alipay_bill_imports (user_id);

CREATE TABLE IF NOT EXISTS alipay_bill_transactions (
    id BIGSERIAL PRIMARY KEY,
    bill_import_id BIGINT NOT NULL,
    person_id BIGINT,
    phone_id BIGINT,
    mobile_cn VARCHAR(11),
    row_hash CHAR(64) NOT NULL,
    trade_time TEXT,
    trade_type TEXT,
    counterparty TEXT,
    product TEXT,
    income_expense TEXT,
    amount_yuan NUMERIC(18, 4),
    payment_method TEXT,
    status TEXT,
    trade_no TEXT,
    merchant_no TEXT,
    remark TEXT,
    source_file TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by TEXT,
    updated_by TEXT,
    extra_text TEXT,
    is_archived BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_ali_tx_import ON alipay_bill_transactions (bill_import_id);
CREATE INDEX IF NOT EXISTS idx_ali_tx_person_hash ON alipay_bill_transactions (person_id, row_hash);
CREATE UNIQUE INDEX IF NOT EXISTS uq_ali_tx_trade_no ON alipay_bill_transactions (trade_no)
    WHERE trade_no IS NOT NULL AND btrim(trade_no) <> '';

-- ========== 备份表明细（可编辑）==========
CREATE TABLE IF NOT EXISTS bkp_wechat_bill_transactions (
    id BIGSERIAL PRIMARY KEY,
    source_tx_id BIGINT,
    bill_import_id BIGINT NOT NULL,
    bill_channel VARCHAR(16) NOT NULL DEFAULT 'WECHAT',
    row_hash CHAR(64),
    trade_time TEXT,
    trade_type TEXT,
    counterparty TEXT,
    product TEXT,
    income_expense TEXT,
    amount_yuan NUMERIC(18, 4),
    payment_method TEXT,
    status TEXT,
    trade_no TEXT,
    merchant_no TEXT,
    remark TEXT,
    source_file TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by TEXT,
    updated_by TEXT,
    extra_text TEXT,
    is_archived BOOLEAN NOT NULL DEFAULT FALSE,
    app_user_id BIGINT
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_bkp_source_per_channel ON bkp_wechat_bill_transactions (bill_channel, source_tx_id)
    WHERE source_tx_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_bkp_tx_import ON bkp_wechat_bill_transactions (bill_import_id);
CREATE INDEX IF NOT EXISTS idx_bkp_channel ON bkp_wechat_bill_transactions (bill_channel);
CREATE UNIQUE INDEX IF NOT EXISTS uq_bkp_trade_no ON bkp_wechat_bill_transactions (trade_no)
    WHERE trade_no IS NOT NULL AND btrim(trade_no) <> '';

COMMENT ON TABLE bkp_wechat_bill_transactions IS '账单明细备份表：仅允许在此增删改';

-- ========== 用户可绑定多个手机号 ==========
CREATE TABLE IF NOT EXISTS app_user_phones (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    mobile_cn VARCHAR(11) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_app_user_phones_user_mobile UNIQUE (user_id, mobile_cn)
);

CREATE INDEX IF NOT EXISTS idx_app_user_phones_user ON app_user_phones (user_id);

-- ========== 手机绑定申请（第二个及后续手机号需管理员审核）==========
CREATE TABLE IF NOT EXISTS phone_bind_request (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    mobile_cn VARCHAR(11) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    reject_reason VARCHAR(512),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    reviewed_at TIMESTAMPTZ,
    reviewed_by_user_id BIGINT
);

CREATE INDEX IF NOT EXISTS idx_phone_bind_request_user ON phone_bind_request (user_id);
CREATE INDEX IF NOT EXISTS idx_phone_bind_request_status ON phone_bind_request (status);
CREATE UNIQUE INDEX IF NOT EXISTS uq_phone_bind_pending_user_mobile
    ON phone_bind_request (user_id, mobile_cn)
    WHERE status = 'PENDING';

-- ========== RBAC 种子数据 ==========
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

-- ADMIN：所有权限
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'ADMIN'
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- ANALYST：导入、备份表、分析
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code IN ('IMPORT_XLSX', 'BKP_TX_CRUD', 'ANALYTICS')
WHERE r.code = 'ANALYST'
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- VIEWER：仅分析
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code = 'ANALYTICS'
WHERE r.code = 'VIEWER'
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- ========== 数据回填（已有数据修复）==========
-- 将 app_user_phones 中的号码同步到 phone_number（避免分析页手机号下拉缺失）
INSERT INTO phone_number (mobile_cn, created_at)
SELECT DISTINCT u.mobile_cn, NOW()
FROM app_user_phones u
WHERE NOT EXISTS (SELECT 1 FROM phone_number p WHERE p.mobile_cn = u.mobile_cn);

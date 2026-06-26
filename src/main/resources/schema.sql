-- =============================================================================
-- PostgreSQL 初始化（最终版本）：不使用 Flyway，由 Spring SQL init 执行（classpath:schema.sql）
--
-- 说明：
-- - 仅创建当前应用确实在用/依赖的表
-- - 表名按“通俗易懂”统一：users / roles / permissions / user_roles / role_permissions
-- - 账号可绑定多个手机号：user_phones + phone_bind_request
-- - 微信账单相关表统一改名：
--     wechat_users            -> bill_users
--     wechat_bill_imports     -> bill_import_record
--     wechat_bill_transactions-> bill_import_data
-- - wx_phone_session 保留（对接微信小程序手机号授权）
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

CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(11) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    password_plain VARCHAR(256),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 兼容已存在但缺列的库（CREATE TABLE IF NOT EXISTS 不会补列）
ALTER TABLE users ADD COLUMN IF NOT EXISTS password_plain VARCHAR(256);

CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id)
);

-- ========== 账号绑定手机号（一个账号多个手机号）==========
CREATE TABLE IF NOT EXISTS user_phones (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    mobile_cn VARCHAR(11) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_user_phones_user_mobile UNIQUE (user_id, mobile_cn)
);

CREATE INDEX IF NOT EXISTS idx_user_phones_user ON user_phones (user_id);

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

-- ========== 号码维表 / person（后续扩展、同时当前导入链路依赖）==========
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

-- ========== 微信/支付宝用户（共用表，channel 区分）==========
CREATE TABLE IF NOT EXISTS bill_users (
    id BIGSERIAL PRIMARY KEY,
    channel VARCHAR(16) NOT NULL DEFAULT 'WECHAT',
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

CREATE INDEX IF NOT EXISTS idx_bill_users_person ON bill_users (person_id);
CREATE INDEX IF NOT EXISTS idx_bill_users_phone ON bill_users (phone_id);
CREATE INDEX IF NOT EXISTS idx_bill_users_channel ON bill_users (channel);

-- ========== 账单导入记录（共用表，channel 区分）==========
CREATE TABLE IF NOT EXISTS bill_import_record (
    id BIGSERIAL PRIMARY KEY,
    channel VARCHAR(16) NOT NULL DEFAULT 'WECHAT',
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

CREATE INDEX IF NOT EXISTS idx_bill_import_record_user ON bill_import_record (user_id);
CREATE INDEX IF NOT EXISTS idx_bill_import_record_channel ON bill_import_record (channel);

-- ========== 账单导入明细数据（共用表，channel 区分）==========
CREATE TABLE IF NOT EXISTS bill_import_data (
    id BIGSERIAL PRIMARY KEY,
    channel VARCHAR(16) NOT NULL DEFAULT 'WECHAT',
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

CREATE INDEX IF NOT EXISTS idx_bill_import_data_import ON bill_import_data (bill_import_id);
CREATE INDEX IF NOT EXISTS idx_bill_import_data_person_hash ON bill_import_data (person_id, row_hash);
CREATE UNIQUE INDEX IF NOT EXISTS uq_bill_import_data_trade_no ON bill_import_data (trade_no)
    WHERE trade_no IS NOT NULL AND btrim(trade_no) <> '';

-- ========== 备份表明细（可编辑）==========
CREATE TABLE IF NOT EXISTS bkp_bill_import_data (
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

CREATE UNIQUE INDEX IF NOT EXISTS uq_bkp_source_per_channel ON bkp_bill_import_data (bill_channel, source_tx_id)
    WHERE source_tx_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_bkp_import ON bkp_bill_import_data (bill_import_id);
CREATE INDEX IF NOT EXISTS idx_bkp_channel ON bkp_bill_import_data (bill_channel);
CREATE UNIQUE INDEX IF NOT EXISTS uq_bkp_trade_no ON bkp_bill_import_data (trade_no)
    WHERE trade_no IS NOT NULL AND btrim(trade_no) <> '';

COMMENT ON TABLE bkp_bill_import_data IS '账单明细备份表：仅允许在此增删改';

-- ========== 微信小程序手机号授权会话 ==========
CREATE TABLE IF NOT EXISTS wx_phone_session (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(128) NOT NULL UNIQUE,
    openid VARCHAR(128),
    session_key TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_wx_phone_session_expires_at ON wx_phone_session (expires_at);

-- ========== 学海：经典读物 / 章节 / 点赞收藏 ==========
CREATE TABLE IF NOT EXISTS xuehai_book (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(512) NOT NULL,
    author VARCHAR(256),
    summary TEXT,
    cover_storage_path TEXT,
    main_file_storage_path TEXT,
    main_file_original_name TEXT,
    like_count INT NOT NULL DEFAULT 0,
    favorite_count INT NOT NULL DEFAULT 0,
    content_type VARCHAR(32) NOT NULL DEFAULT 'other',
    status VARCHAR(32) NOT NULL DEFAULT 'ongoing',
    tags VARCHAR(512),
    word_count INT NOT NULL DEFAULT 0,
    avg_score DOUBLE PRECISION NOT NULL DEFAULT 0,
    rating_count INT NOT NULL DEFAULT 0,
    read_count INT NOT NULL DEFAULT 0,
    publish_at TIMESTAMPTZ,
    recommend_weight INT NOT NULL DEFAULT 0,
    created_by_user_id BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_xuehai_book_created_at ON xuehai_book (created_at DESC);

CREATE TABLE IF NOT EXISTS xuehai_chapter (
    id BIGSERIAL PRIMARY KEY,
    book_id BIGINT NOT NULL REFERENCES xuehai_book (id) ON DELETE CASCADE,
    title VARCHAR(512) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    file_storage_path TEXT NOT NULL,
    file_original_name TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_xuehai_chapter_book ON xuehai_chapter (book_id, sort_order);

CREATE TABLE IF NOT EXISTS xuehai_book_like (
    user_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL REFERENCES xuehai_book (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, book_id)
);

CREATE INDEX IF NOT EXISTS idx_xuehai_book_like_book ON xuehai_book_like (book_id);

CREATE TABLE IF NOT EXISTS xuehai_book_favorite (
    user_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL REFERENCES xuehai_book (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, book_id)
);

CREATE INDEX IF NOT EXISTS idx_xuehai_book_favorite_book ON xuehai_book_favorite (book_id);

-- 学海 v0.2 扩展字段（已有库增量）
ALTER TABLE xuehai_book ADD COLUMN IF NOT EXISTS content_type VARCHAR(32) NOT NULL DEFAULT 'other';
ALTER TABLE xuehai_book ADD COLUMN IF NOT EXISTS status VARCHAR(32) NOT NULL DEFAULT 'ongoing';
ALTER TABLE xuehai_book ADD COLUMN IF NOT EXISTS tags VARCHAR(512);
ALTER TABLE xuehai_book ADD COLUMN IF NOT EXISTS word_count INT NOT NULL DEFAULT 0;
ALTER TABLE xuehai_book ADD COLUMN IF NOT EXISTS avg_score DOUBLE PRECISION NOT NULL DEFAULT 0;
ALTER TABLE xuehai_book ADD COLUMN IF NOT EXISTS rating_count INT NOT NULL DEFAULT 0;
ALTER TABLE xuehai_book ADD COLUMN IF NOT EXISTS read_count INT NOT NULL DEFAULT 0;
ALTER TABLE xuehai_book ADD COLUMN IF NOT EXISTS publish_at TIMESTAMPTZ;
ALTER TABLE xuehai_book ADD COLUMN IF NOT EXISTS recommend_weight INT NOT NULL DEFAULT 0;

UPDATE xuehai_book SET publish_at = created_at WHERE publish_at IS NULL;

CREATE TABLE IF NOT EXISTS xuehai_bookshelf (
    user_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL REFERENCES xuehai_book (id) ON DELETE CASCADE,
    added_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    pinned BOOLEAN NOT NULL DEFAULT FALSE,
    pin_order INT NOT NULL DEFAULT 0,
    last_read_chapter_id BIGINT,
    last_read_at TIMESTAMPTZ,
    PRIMARY KEY (user_id, book_id)
);

CREATE INDEX IF NOT EXISTS idx_xuehai_bookshelf_user ON xuehai_bookshelf (user_id, last_read_at DESC NULLS LAST);

CREATE TABLE IF NOT EXISTS xuehai_book_rating (
    user_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL REFERENCES xuehai_book (id) ON DELETE CASCADE,
    score INT NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, book_id)
);

CREATE INDEX IF NOT EXISTS idx_xuehai_book_rating_book ON xuehai_book_rating (book_id);

CREATE TABLE IF NOT EXISTS xuehai_read_history (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL REFERENCES xuehai_book (id) ON DELETE CASCADE,
    chapter_id BIGINT REFERENCES xuehai_chapter (id) ON DELETE SET NULL,
    progress DOUBLE PRECISION NOT NULL DEFAULT 0,
    read_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_xuehai_read_history_user ON xuehai_read_history (user_id, read_at DESC);

CREATE TABLE IF NOT EXISTS xuehai_read_progress (
    user_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL REFERENCES xuehai_book (id) ON DELETE CASCADE,
    chapter_id BIGINT REFERENCES xuehai_chapter (id) ON DELETE SET NULL,
    offset_pos INT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, book_id)
);

-- ========== RBAC 种子数据 ==========
INSERT INTO roles (code, name) VALUES
    ('MASTER', '超级管理员'),
    ('ADMIN', '管理员'),
    ('USER', '普通用户')
ON CONFLICT (code) DO NOTHING;

INSERT INTO permissions (code, name) VALUES
    ('RBAC_ADMIN', '权限与角色管理（仅 master）'),
    ('PHONE_BIND_REVIEW', '手机号审核（仅 master）'),
    ('VIEW_ALL_BILLS', '查看全量账单数据（admin/master）'),
    ('IMPORT_XLSX', '导入账单'),
    ('ANALYTICS', '收支分析查询'),
    ('BKP_TX_CRUD', '备份表明细增删改查')
ON CONFLICT (code) DO NOTHING;

-- MASTER：所有权限
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'MASTER'
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- ADMIN：查看全量数据 + 导入 + 分析
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code IN ('VIEW_ALL_BILLS', 'IMPORT_XLSX', 'ANALYTICS')
WHERE r.code = 'ADMIN'
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- USER：导入 + 分析（只能看自己绑定手机号范围的数据）
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code IN ('IMPORT_XLSX', 'ANALYTICS')
WHERE r.code = 'USER'
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- 将 user_phones 中的号码同步到 phone_number（避免分析页手机号下拉缺失）
INSERT INTO phone_number (mobile_cn, created_at)
SELECT DISTINCT u.mobile_cn, NOW()
FROM user_phones u
WHERE NOT EXISTS (SELECT 1 FROM phone_number p WHERE p.mobile_cn = u.mobile_cn);


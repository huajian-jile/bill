-- 备份表明细渠道（微信 / 支付宝）；原始明细 id 按渠道唯一
ALTER TABLE bkp_wechat_bill_transactions
    ADD COLUMN IF NOT EXISTS bill_channel VARCHAR(16) NOT NULL DEFAULT 'WECHAT';

ALTER TABLE bkp_wechat_bill_transactions DROP CONSTRAINT IF EXISTS bkp_wechat_bill_transactions_source_tx_id_key;

CREATE UNIQUE INDEX IF NOT EXISTS uq_bkp_source_per_channel
    ON bkp_wechat_bill_transactions (bill_channel, source_tx_id)
    WHERE source_tx_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_bkp_channel ON bkp_wechat_bill_transactions (bill_channel);

-- 支付宝原始导入表（结构与微信侧一致，便于同一套导入解析）
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

CREATE TABLE IF NOT EXISTS alipay_bill_transactions (
    id BIGSERIAL PRIMARY KEY,
    bill_import_id BIGINT NOT NULL,
    person_id BIGINT,
    phone_id BIGINT,
    mobile_cn VARCHAR(11),
    row_hash VARCHAR(64) NOT NULL,
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

CREATE INDEX IF NOT EXISTS idx_alipay_tx_import ON alipay_bill_transactions (bill_import_id);
CREATE INDEX IF NOT EXISTS idx_alipay_tx_person_hash ON alipay_bill_transactions (person_id, row_hash);

COMMENT ON TABLE alipay_bill_transactions IS '支付宝账单原始导入表（仅存储，界面主要展示备份表）';

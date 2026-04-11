-- 业务侧可编辑的备份表；不修改 Python 导入的 wechat_bill_transactions
CREATE TABLE IF NOT EXISTS bkp_wechat_bill_transactions (
    id BIGSERIAL PRIMARY KEY,
    source_tx_id BIGINT UNIQUE,
    bill_import_id BIGINT NOT NULL,
    row_hash VARCHAR(64),
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

-- 外键在 V3 中追加（确保 V1 已创建 app_users）

CREATE INDEX IF NOT EXISTS idx_bkp_tx_import ON bkp_wechat_bill_transactions (bill_import_id);
CREATE INDEX IF NOT EXISTS idx_bkp_tx_source ON bkp_wechat_bill_transactions (source_tx_id);

COMMENT ON TABLE bkp_wechat_bill_transactions IS '账单明细备份表：仅允许在此增删改，不直接改 wechat_bill_transactions';
COMMENT ON COLUMN bkp_wechat_bill_transactions.source_tx_id IS '对应原始导入明细 id，可空表示手工新增行';

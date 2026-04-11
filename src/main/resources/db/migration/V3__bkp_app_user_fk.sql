-- V1 已创建 app_users 后再挂外键（避免与 V2 顺序/基线问题冲突）
DO $$
BEGIN
    IF to_regclass('public.app_users') IS NOT NULL
       AND to_regclass('public.bkp_wechat_bill_transactions') IS NOT NULL THEN
        IF NOT EXISTS (
            SELECT 1
            FROM pg_constraint
            WHERE conname = 'fk_bkp_wechat_bill_transactions_app_user'
        ) THEN
            ALTER TABLE bkp_wechat_bill_transactions
                ADD CONSTRAINT fk_bkp_wechat_bill_transactions_app_user
                    FOREIGN KEY (app_user_id) REFERENCES app_users (id) ON DELETE SET NULL;
        END IF;
    END IF;
END;
$$;

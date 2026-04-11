-- 支付宝导入与微信导入共用「自然人/账号」维度：user_id 存 wechat_users.id。
-- 若误建为 REFERENCES alipay_users，会导致插入时 500（键在 alipay_users 中不存在）。
ALTER TABLE alipay_bill_imports DROP CONSTRAINT IF EXISTS alipay_bill_imports_user_id_fkey;

ALTER TABLE alipay_bill_imports
    ADD CONSTRAINT alipay_bill_imports_user_id_fkey
    FOREIGN KEY (user_id) REFERENCES wechat_users (id);

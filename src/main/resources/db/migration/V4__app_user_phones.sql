-- 用户可绑定多个手机号（登录账号即手机号，登录后自动写入一条绑定；可再增绑，后续可接短信验证）
CREATE TABLE IF NOT EXISTS app_user_phones (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES app_users (id) ON DELETE CASCADE,
    mobile_cn VARCHAR(11) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_app_user_phones_user_mobile UNIQUE (user_id, mobile_cn),
    CONSTRAINT uq_app_user_phones_mobile UNIQUE (mobile_cn)
);

CREATE INDEX IF NOT EXISTS idx_app_user_phones_user ON app_user_phones (user_id);

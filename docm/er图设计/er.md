erDiagram
  %% ========== RBAC ==========
  app_users ||--o{ user_roles : has
  roles ||--o{ user_roles : assigned_to
  roles ||--o{ role_permissions : grants
  permissions ||--o{ role_permissions : allowed

  %% ========== 登录账号绑定手机号 ==========
  app_users ||--o{ app_user_phones : binds

  %% ========== 手机号审核队列 ==========
  app_users ||--o{ phone_bind_request : submits
  app_users ||--o{ phone_bind_request : reviews

  %% ========== 号码维表 / 自然人 ==========
  phone_number ||--o{ person : primary_phone
  person ||--o{ person_phone : has
  phone_number ||--o{ person_phone : links

  %% ========== 导入用户维度（wechat_users 也承载支付宝，靠 channel 区分） ==========
  phone_number ||--o{ wechat_users : phone
  person ||--o{ wechat_users : person

  %% ========== 导入批次 ==========
  wechat_users ||--o{ wechat_bill_imports : imports
  person ||--o{ wechat_bill_imports : person
  phone_number ||--o{ wechat_bill_imports : phone

  %% ========== 原始交易明细 ==========
  wechat_bill_imports ||--o{ wechat_bill_transactions : contains
  person ||--o{ wechat_bill_transactions : person
  phone_number ||--o{ wechat_bill_transactions : phone

  %% ========== 备份表交易明细（可编辑） ==========
  wechat_bill_imports ||--o{ bkp_wechat_bill_transactions : backup_of_import
  app_users ||--o{ bkp_wechat_bill_transactions : edited_by

  %% ========== 微信手机号授权会话 ==========
  wx_phone_session ||--|| wx_phone_session : "independent"

  %% ---- table definitions (for readability only) ----
  app_users {
    BIGSERIAL id PK
    VARCHAR username UK
    VARCHAR password_hash
    VARCHAR password_plain "nullable"
    BOOLEAN enabled
    TIMESTAMPTZ created_at
  }

  roles {
    BIGSERIAL id PK
    VARCHAR code UK
    VARCHAR name
  }

  permissions {
    BIGSERIAL id PK
    VARCHAR code UK
    VARCHAR name
  }

  user_roles {
    BIGINT user_id
    BIGINT role_id
  }

  role_permissions {
    BIGINT role_id
    BIGINT permission_id
  }

  app_user_phones {
    BIGSERIAL id PK
    BIGINT user_id
    VARCHAR mobile_cn
    TIMESTAMPTZ created_at
  }

  phone_bind_request {
    BIGSERIAL id PK
    BIGINT user_id
    VARCHAR mobile_cn
    VARCHAR status
    TIMESTAMPTZ created_at
    TIMESTAMPTZ reviewed_at "nullable"
    BIGINT reviewed_by_user_id "nullable"
    VARCHAR reject_reason "nullable"
  }

  phone_number {
    BIGSERIAL id PK
    VARCHAR mobile_cn UK
    TIMESTAMPTZ created_at
  }

  person {
    BIGSERIAL id PK
    TEXT display_name "nullable"
    BIGINT phone_id "nullable"
    TIMESTAMPTZ created_at
  }

  person_phone {
    BIGSERIAL id PK
    BIGINT person_id
    BIGINT phone_id
    TIMESTAMPTZ created_at
  }

  wechat_users {
    BIGSERIAL id PK
    VARCHAR channel
    TEXT wechat_nickname
    BIGINT person_id "nullable"
    BIGINT phone_id "nullable"
    BOOLEAN is_archived
    TIMESTAMPTZ created_at
    TIMESTAMPTZ updated_at
  }

  wechat_bill_imports {
    BIGSERIAL id PK
    VARCHAR channel
    BIGINT user_id
    BIGINT person_id "nullable"
    BIGINT phone_id "nullable"
    VARCHAR mobile_cn "nullable"
    TEXT source_file
    BOOLEAN is_archived
    TIMESTAMPTZ created_at
    TIMESTAMPTZ updated_at
  }

  wechat_bill_transactions {
    BIGSERIAL id PK
    VARCHAR channel
    BIGINT bill_import_id
    BIGINT person_id "nullable"
    BIGINT phone_id "nullable"
    VARCHAR mobile_cn "nullable"
    CHAR row_hash
    TEXT trade_time "nullable"
    TEXT counterparty "nullable"
    NUMERIC amount_yuan "nullable"
    TEXT trade_no "nullable"
    BOOLEAN is_archived
    TIMESTAMPTZ created_at
    TIMESTAMPTZ updated_at
  }

  bkp_wechat_bill_transactions {
    BIGSERIAL id PK
    BIGINT source_tx_id "nullable"
    BIGINT bill_import_id
    VARCHAR bill_channel
    BIGINT app_user_id "nullable"
    BOOLEAN is_archived
    TIMESTAMPTZ created_at
    TIMESTAMPTZ updated_at
  }

  wx_phone_session {
    BIGSERIAL id PK
    VARCHAR code UK
    VARCHAR session_key
    VARCHAR openid "nullable"
    TIMESTAMPTZ expires_at
    TIMESTAMPTZ created_at
  }

# 数据库说明

- **唯一直接可执行的整库 SQL**：`migration/V1__init.sql`（建表 + RBAC 种子；可 psql 整文件执行，也可由 Flyway 在启动时执行）。
- `flyway_schema_history` 由 Flyway 自动维护。
- 若曾用过旧版多版本迁移与本 `V1` 冲突，请**备份数据**后新建库或清空 `public` 与 `flyway_schema_history`，再启动应用。
- `wechat_bill_transactions`、`alipay_bill_transactions`、`bkp_wechat_bill_transactions` 对 `trade_no` 使用**部分唯一索引**（非空且非空白时唯一）。

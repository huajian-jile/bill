# 代码 Review 导读（按目录/结构）

本项目为 **Spring Boot 3 + PostgreSQL（schema.sql 初始化）** 的后端，配套 **Vue3 + Element Plus + Vite** 的前端（构建产物输出到 `src/main/resources/static`），核心场景是：**账单导入 → 分析看板/分组看板 → 管理员用户与手机号审核**。

> 目标：帮助你 review 代码时“先看结构，再看调用链”，并能快速定位每个类/文件的职责边界。

---

## 目录总览（建议 Review 顺序）

- `pom.xml`：后端依赖与插件（Spring Boot / JPA / Security / MyBatis-Plus / JWT / POI）。
- `src/main/resources/application.yaml`：端口、数据源、SQL 初始化、JWT、bootstrap 管理员、微信配置。
- `src/main/resources/schema.sql`：数据库结构初始化（不使用 Flyway）。
- `src/main/java/org/example/bill/`：后端代码（按 `config/ security/ web/ service/ domain/ repo/ mapper/ util` 分层）。
- `frontend/`：前端源码（Vue3 SPA），`vite build` 输出到后端的 `static`。

---

## 后端（`src/main/java/org/example/bill`）

### 入口

- `BillApplication`
  - Spring Boot 启动类。
  - 关注点：仅负责启动，不承载业务逻辑。

---

### `config/`（配置与启动初始化）

- `SecurityConfig`
  - 配置 Spring Security FilterChain：
    - `POST /api/auth/login`、`/api/auth/register` 放行
    - 静态资源 `/index.html`、`/assets/**` 放行
    - 其它 `/api/**` 均需登录（JWT）
  - 注册 `JwtAuthFilter`（解析 `Authorization: Bearer <token>` 写入 SecurityContext）。

- `DataInitializer`
  - 启动时按 `app.bootstrap.*` 做**种子管理员**：
    - 若管理员不存在：创建 `MASTER` 角色用户，写入 `passwordHash`
    - 绑定管理员手机号：写 `phone_number` 与 `user_phones`（用于分析下拉与权限范围）

---

### `security/`（JWT 与认证）

- `AppUserDetailsService`
  - Spring Security 登录时加载用户（JPA：`AppUserRepository`）。
  - 注意：这里返回的 `UserDetails` 不携带权限；权限由 JWT 的 `authorities` claim 负责（见 `JwtService`）。

- `JwtService`
  - 负责创建/解析 JWT：
    - claim：`uid`（用户 id）、`authorities`（形如 `ROLE_ADMIN,PERM_USER_ADMIN,...` 的字符串）
  - 权限来源：`AppUser.roles -> Role.permissions`。

- `JwtAuthFilter`
  - 每次请求解析 `Authorization` 头并把 `authorities` 写入 `SecurityContext`。
  - 特点：只做“信任 token 并写入上下文”，不查库。

---

### `web/`（Controller：API 边界）

#### 认证

- `AuthController`（`/api/auth`）
  - `POST /register`：注册（账号=手机号），默认 `USER` 角色；并自动绑定手机号（写入 `user_phones`）。
  - `POST /login`：仅支持 11 位手机号；登录后 `ensureLoginPhoneBound` 确保 username 本身也在绑定表中。

- `AuthResponseMapper`
  - 登录/注册返回体 `LoginResponse` 的组装器：生成 JWT + authorities + 当前用户已绑定手机号列表（写入 localStorage 用于前端路由 gating）。

#### 错误处理

- `RestExceptionHandler`
  - 统一把常见异常映射为 JSON `{ message }`：
    - 401：`BadCredentialsException`
    - 403：`AccessDeniedException`
    - 400：`IllegalArgumentException`、校验异常
    - 其它：可在控制台看堆栈

#### “我”的上下文

- `MeContextController`（`/api/me`）
  - `GET /bill-phones`：分析页手机号下拉：
    - 普通用户：返回**自己绑定手机号**对应的 `phone_number` 选项
    - 管理员：返回系统 `phone_number` 全量（便于跨账号查看分析）
  - `GET /has-import`：当前用户是否有导入记录（用于前端登录后跳转策略）。

- `MePhoneController`（`/api/me/phones`）
  - 当前用户的已绑定手机号列表（来自 `user_phones`）。
  - `POST /me/phones`：
    - 若当前用户已绑定手机号数为 0：直接绑定
    - 第二个及以后：进入待审核队列（见 `PhoneBindQueueService`）

- `MePhoneBindQueueController`（`/api/me/phone-bind-requests`）
  - 用户查看自己的“绑定申请与审核记录”（待审/已通过/已拒绝），并能看到拒绝理由。

#### 分析（看板/分组看板）

- `AnalyticsController`（`/api/analytics`）
  - 所有接口只要求 `isAuthenticated()`（已登录即可），**数据范围由 `AnalyticsScopeService` 控制**：
    - 普通用户：只能查询自己绑定的手机号范围
    - 管理员：可不限制或使用任意 `phoneId/phoneIds`
  - 关键接口：
    - `/day`、`/day-detail`：某日汇总/明细
    - `/month`：月度日历格
    - `/by-type`：收入/支出/中性分类汇总
    - `/real`：真实收支（剔除同日同金额一收一支）
    - `/by-counterparty`、`/by-counterparty-detail`：分组看板（按交易对方聚合 + 明细抽屉）

#### 导入

- `BillImportController`（`/api/import`）
  - `POST /wechat`：上传微信 xlsx/csv
  - `POST /alipay`：上传支付宝 csv

#### 备份表（可编辑明细）

- `BkpTransactionController`（`/api/bkp/transactions`）
  - 备份表 CRUD（受 `PERM_BKP_TX_CRUD` 限制）。

- `BkpRestoreController`（`/api/bkp/restore`）
  - 从原始交易表恢复到备份表（按日/月/年/全量），受 `PERM_BKP_TX_CRUD` 限制。

#### 管理端（用户/审核）

- `web/admin/UserAdminController`（`/api/admin/users`）
  - 用户与角色管理：列表、创建、删除、改密、启停用、角色变更、解绑手机号等。
  - 统一权限：`PERM_RBAC_ADMIN`（仅 master）。

- `web/admin/PhoneBindAdminController`（`/api/admin/phone-bind-requests`）
  - 手机号审核（仅 master）：
    - `GET /`：待审核列表（PENDING）
    - `POST /{id}/approve`：通过（会写绑定表）
    - `POST /{id}/reject`：拒绝（**必须传理由** `{reason}`）
    - `GET /history`：历史记录（APPROVED/REJECTED）含拒绝理由
  - 统一权限：`PERM_PHONE_BIND_REVIEW`。

#### 微信登录（手机号授权）

- `WxAuthController`（`/api/wx/login`）
  - 小程序侧：code + encryptedData + iv → 登录/注册并返回 JWT。

---

### `service/`（业务服务：调用链核心）

> 推荐 review 时从 Controller 进入到 Service，再下沉到 Repo/Mapper。

#### RBAC / 用户

- `AdminUserService`
  - 管理员操作 AppUser：创建、删除、改密、改角色、启停用。
  - 规则：不能删除当前登录账号；不能删除唯一 MASTER；创建时会确保登录手机号自动绑定。

- `AuthCredentialRules`
  - 抽出的“密码规则”校验（长度、非空等）。

#### 手机号绑定与审核

- `UserPhoneService`
  - 写入与读取 `user_phones`（账号 ↔ 手机号，多对多）。
  - `ensureLoginPhoneBound`：把 username（手机号）补到绑定表中。
  - `addPhone/removePhone/listMobiles`：绑定管理。
  - **关键点**：会调用 `BillImportLinkageService.ensurePhoneNumberRow`，保证 `phone_number` 里也存在手机号（用于分析下拉）。

- `PhoneBindQueueService`
  - “第二个及后续手机号”审核队列（表 `phone_bind_request`）：
    - `requestBindOrApproveDirect`：首个手机号直接绑定，否则创建 PENDING
    - `approve`：通过 → 调 `UserPhoneService.addPhone`，并写 `reviewedAt/reviewedBy`
    - `reject`：拒绝 → 写 `rejectReason`（必填）+ reviewed 字段
    - `listPendingAll/listProcessedHistory/listAllForUser`：待审/历史/用户全量查询

#### 导入链路

- `WechatXlsxImportService`
  - 解析微信 xlsx/csv、支付宝 csv（两者共用部分 CSV 逻辑）。
  - 导入时通过 `BillImportLinkageService.ensurePhoneAndPersonLinked` 建立 `phone_number/person/person_phone/wechat_users.phone_id` 等关联。

- `BillImportLinkageService`
  - 处理“手机号 → phone_number → person → person_phone → wechat_users(或alipay) 的 phone/person 外键”这一条链路。
  - `ensurePhoneNumberRow`：仅确保 `phone_number` 里有手机号（绑定成功就可出现在分析下拉）。
  - `ensurePhoneAndPersonLinked`：导入时补全 person/phone 关联。

#### 分析链路

- `AnalyticsScopeService`
  - 把前端传入的 `phoneId/phoneIds` 转成 `wechat_users.id` 列表。
  - 规则：
    - 普通用户只能用自己绑定的手机号范围
    - 管理员可不限制（返回 null 表示“不限用户”）

- `AnalyticsService`
  - 基于交易表（微信/支付宝）做聚合、明细、对比等计算。
  - `counterpartyBoard/counterpartyDetail`：分组看板的核心服务。
  - `RefundPairFinder`：真实收支剔除“同日同金额一收一支”的配对算法。

#### 备份表

- `BkpTransactionService`、`BkpRestoreService`
  - 备份表的查询/CRUD/恢复逻辑，对应 `BkpTransactionController` / `BkpRestoreController`。

#### 微信手机号授权

- `WxAuthService`
  - 用微信 `code` 换 `session_key`，解密手机号，创建/查找 `AppUser` 并返回 JWT。
  - `WxPhoneSessionRepository` 存 5 分钟一次性会话（`wx_phone_session` 表）。

---

### `domain/`（实体：DB 映射层）

项目同时存在 **JPA 实体**与 **MyBatis-Plus 实体**，review 时重点关注“这个实体归属于哪一套持久层”。

- JPA（Spring Data JPA `repo/` 使用）
  - `AppUser`、`Role`、`Permission`、`AppUserPhone`、`WechatBillImport/Transaction`（以及部分 bkp 实体）、`WxPhoneSession` 等

- MyBatis-Plus（`mapper/` 使用）
  - `PhoneNumber`、`Person`、`PersonPhone`、`WechatUser`、`PhoneBindQueue` 等（以及部分导入/交易 mapper 对应实体）

> 注意：不要把 JPA 实体直接交给 MyBatis-Plus 的 `BaseMapper` 使用，否则可能出现表名/字段推导错误（例如把 `AppUser` 推成 `app_user`）。

---

### `repo/`（JPA Repository）

- `AppUserRepository`：用户查找/统计（含“拥有某角色的用户数”等）。
- `RoleRepository`：按 code 查角色、列出角色。
- `AppUserPhoneRepository`：账号绑定手机号表。
- `WechatBillImportRepository`、`WechatBillTransactionRepository`：交易与导入记录（分析/导入服务用）。
- `WxPhoneSessionRepository`：微信手机号授权会话。
- `BkpWechatBillTransactionRepository` 等：备份表相关。

---

### `mapper/`（MyBatis/MyBatis-Plus Mapper）

- `PhoneNumberMapper`：`phone_number` 表（分析下拉、person 绑定链路等）。
- `PhoneBindQueueMapper`：`phone_bind_request` 审核队列。
- `RoleMapper/PermissionMapper/UserRoleMapper`：RBAC 查询/关联（若使用 MP 方式加载角色权限）。
- `WechatUserMapper`：`wechat_users`（phone_id/person_id 关联、按 phoneId 查询最早用户等）。
- `WechatBillTransactionMapper` 等：部分批量操作/自定义 SQL（与 repo 并存）。

---

### `util/`（工具）

常见：手机号格式化、账号校验、时间解析、hash 计算等（例如 `PhoneUtil/AccountUsernameUtil/TradeTimeUtil/RowHashUtil`）。

---

## 数据库初始化（`src/main/resources/schema.sql`）

- `schema.sql`
  - 项目 DDL：RBAC、用户、账号↔手机号、绑定申请、号码维表/person、账单导入记录/明细、备份表、微信小程序手机号会话等。

---

## 前端（`frontend/src`）

### 基础设施

- `main.js`
  - Vue 应用入口：挂载 App、注册 router、引入 Element Plus 等。

- `api.js`
  - Axios 实例：`baseURL='/api'`，请求前注入 `Bearer token`，401 自动跳回登录页。

- `router.js`
  - 路由与导航守卫：
    - 未登录 → `/login`
    - `requiresAdmin` 的页面需要 `PERM_USER_ADMIN`
    - 普通用户未绑手机号时，仍允许访问 `/analytics*` 与 `/phones`（其它页面会被引导去绑定页）

### Layout 与导航

- `views/Layout.vue`
  - 顶部“账单分析/备忘录/其他工具”导航与下拉菜单。
  - 启动时拉取手机号列表写入 localStorage：
    - admin → `/me/bill-phones`（全量）
    - 普通用户 → `/me/phones`（自己绑定）

### 认证页

- `views/Login.vue`
  - 登录/注册 UI。
  - 登录成功后保存 token/authorities/phones，并根据 `/me/has-import` 决定跳转导入或分析。

### 核心业务页面

- `views/ImportXlsx.vue`
  - 上传微信/支付宝账单文件，调用 `/api/import/wechat|alipay`。

- `views/AnalyticsHub.vue`
  - 分析看板：按日/月/年/全部 + 渠道 + 手机号（单选/多选）查询。
  - 调用 `/api/me/bill-phones` 作为下拉选项；调用 `/api/analytics/*` 获取数据。

- `views/CounterpartyGroupBoard.vue`
  - 分组看板：按交易对方聚合的图表 + 列表 + 抽屉明细。
  - 调用：
    - `/api/analytics/by-counterparty`
    - `/api/analytics/by-counterparty-detail`
    - `/api/me/bill-phones`（手机号下拉）

- `views/PhoneBind.vue`
  - “绑定手机号”页：
    - 已绑定号码列表：`GET /api/me/phones`
    - 新增绑定：`POST /api/me/phones`（返回 `immediate|pending_review`）
    - 申请与审核记录：`GET /api/me/phone-bind-requests`（含拒绝理由）

### 管理端页面

- `views/AdminUsers.vue`
  - 用户管理：列表、创建、删除、改密、角色、启停用、解绑手机号等（`/api/admin/users/*`）。

- `views/PhoneBindApprovals.vue`
  - 手机号审核：
    - 待审列表：`GET /api/admin/phone-bind-requests`
    - 历史记录：`GET /api/admin/phone-bind-requests/history`
    - 拒绝：弹窗要求填写理由 → `POST /reject {reason}`

### 组件与工具

- `components/AnalyticsDetailTable.vue`：明细表复用组件（抽屉明细、日明细等）。
- `components/MonthDailyCalendarGrid.vue`：月视图日历格渲染。
- `utils/incomeExpense.js`：收支类型相关前端小工具。

---

## 关键调用链（从入口到落库）

### 1) 注册/登录 → JWT → 前端 session

1. `Login.vue` → `POST /api/auth/login`（或 register）
2. `AuthController` 登录成功后 → `AuthResponseMapper.toLoginResponse`
3. `JwtService.createToken` 把 `roles/permissions` 编进 token 的 `authorities`
4. 前端 `api.js` 在每次请求加 `Authorization: Bearer ...`

### 2) 绑定手机号（第二个起走审核）→ 记录 + 拒绝理由

1. `PhoneBind.vue` → `POST /api/me/phones`
2. `MePhoneController.add` → `PhoneBindQueueService.requestBindOrApproveDirect`
3. 若非首个手机号：写入 `phone_bind_request(PENDING)`
4. 管理员在 `PhoneBindApprovals.vue`：
   - 通过：`POST /api/admin/phone-bind-requests/{id}/approve` → 写绑定表 + 更新 status
   - 拒绝：`POST /.../{id}/reject {reason}` → 写 `reject_reason` + 更新 status
5. 用户在 `PhoneBind.vue` 的“申请与审核记录”通过 `GET /api/me/phone-bind-requests` 看到结果与理由

### 3) 导入账单 → 建立 phone/person/wechat_user 关联 → 可分析

1. `ImportXlsx.vue` 上传 → `POST /api/import/wechat|alipay`
2. `WechatXlsxImportService.importXlsx/importCsv`
3. `BillImportLinkageService.ensurePhoneAndPersonLinked`
4. 写入 `phone_number/person/person_phone/wechat_users.phone_id` 与导入/交易数据
5. 分析页通过 `MeContextController.billPhones` 获取手机号下拉，进而做范围控制

---

## Review 关注点（建议检查）

- **持久层混用边界**：JPA 实体不要直接用 MyBatis-Plus `BaseMapper` 查询（表名/字段推断风险）。
- **权限/范围**：
  - “是否能看”由 `@PreAuthorize` 决定（多数接口已改为只要登录）
  - “能看多少数据”由 `AnalyticsScopeService` 限定手机号范围决定
- **手机号三张表的定位**：
  - `user_phones`：登录账号绑定的手机号（权限范围）
  - `phone_number`：系统统一号码维表（分析下拉、person/wechat_user 关联）
  - `phone_bind_request`：审核队列与历史记录（含拒绝理由）


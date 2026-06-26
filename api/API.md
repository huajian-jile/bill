# 账单系统 API 文档

> 所有路径前缀: `/api`
> 认证方式: `Authorization: Bearer <token>` 请求头
> `channel` 参数可选值: `wechat` / `alipay` / `merged`

---

## 1. 认证 `/api/auth`

### POST `/auth/register` — 注册
**认证:** 否

**请求体:**
```json
{
  "mobile": "string",       // 11位大陆手机号，作为账号
  "password": "string",     // 密码，1-128字符
  "confirmPassword": "string" // 确认密码，须与 password 一致
}
```

**响应:** `200`
```json
{
  "token": "string",
  "username": "string",
  "authorities": ["string"],
  "phones": ["string"]
}
```

---

### POST `/auth/login` — 登录
**认证:** 否

**请求体:**
```json
{
  "mobile": "string",   // 手机号（11位）或旧账号（10位数字，兼容旧账号）
  "password": "string"
}
```

**响应:** `200`
```json
{
  "token": "string",
  "username": "string",
  "authorities": ["string"],
  "phones": ["string"]
}
```

---

## 2. 账户与个人 `/api/me`

### GET `/me/linked-wechat-user-id` — 获取绑定的 wechat_user.id
**认证:** 是

**响应:** `200`
```json
{
  "wechatUserId": "long"
}
```

---

### GET `/me/bill-phones` — 分析页手机号下拉
**认证:** 是

**响应:** `200`
```json
[{
  "id": "long",
  "mobileCn": "string"
}]
```

---

### GET `/me/has-import` — 是否有导入记录
**认证:** 是

**响应:** `200`
```json
{
  "hasImport": "boolean"
}
```

---

## 3. 手机号绑定 `/api/me/phones`

### GET `/me/phones` — 已绑定手机号列表
**认证:** 是

**响应:** `200`
```json
["string"]   // 手机号数组，如 ["19194998142"]
```

---

### POST `/me/phones` — 申请绑定手机号
**认证:** 是

**请求体:**
```json
{ "mobile": "string" }
```

**响应:** `200`
```json
{ "status": "immediate | pending_review" }
```

---

## 4. 绑定请求 `/api/me/phone-bind-requests`

### GET `/me/phone-bind-requests` — 我的绑定请求列表
**认证:** 是

**响应:** `200`
```json
[{
  "id": "long",
  "mobileCn": "string",
  "status": "string",
  "createdAt": "instant",
  "reviewedAt": "instant?",
  "rejectReason": "string?"
}]
```

---

## 5. 人员-手机号关联 `/api/me/person-phones`

### GET `/me/person-phones/linkable-phones` — 可关联手机号
**认证:** 是

**响应:** `200`
```json
[{
  "id": "long",
  "label": "string"
}]
```

---

### GET `/me/person-phones/links` — 已有关联列表
**认证:** 是

**响应:** `200`
```json
[{
  "linkId": "long",
  "personId": "long",
  "personLabel": "string",
  "phoneId": "long",
  "mobileCn": "string"
}]
```

---

### POST `/me/person-phones` — 新建关联
**认证:** 是

**请求体:**
```json
{ "personId": "long", "phoneId": "long" }
```

**响应:** `200` — `201`

---

### DELETE `/me/person-phones/{linkId}` — 删除关联
**认证:** 是

**路径参数:** `linkId: long`

**响应:** `204`

---

## 6. 账单导入 `/api/import`

### POST `/import/wechat` — 导入微信账单
**认证:** 是

**Content-Type:** `multipart/form-data`

**参数:**
| 参数 | 类型 | 说明 |
|------|------|------|
| `file` | MultipartFile | .xlsx 文件 |
| `mobileCn` | String | 手机号 |

**响应:** `200`
```json
{
  "id": "long",
  "userId": "long",
  "mobileCn": "string",
  "sourceFile": "string",
  "channel": "WECHAT",
  "totalCount": "int",
  "incomeCount": "int",
  "expenseCount": "int",
  "neutralCount": "int",
  "incomeAmount": "decimal",
  "expenseAmount": "decimal",
  "neutralAmount": "decimal",
  "exportTime": "instant?",
  "rangeStart": "instant?",
  "rangeEnd": "instant?",
  "createdAt": "instant"
}
```

---

### POST `/import/alipay` — 导入支付宝账单
**认证:** 是

**Content-Type:** `multipart/form-data`

**参数:**
| 参数 | 类型 | 说明 |
|------|------|------|
| `file` | MultipartFile | .csv 文件（GBK编码） |
| `mobileCn` | String | 手机号 |

**响应:** 同上，但 `channel` 为 `"ALIPAY"`

---

## 7. 分析看板 `/api/analytics`

> 所有分析接口支持: `phoneId`(Long) / `phoneIds`(String, 逗号分隔) / `channel`(wechat/alipay/merged)

---

### GET `/analytics/day` — 某日收支汇总
**认证:** 是

**参数:**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `date` | LocalDate | 是 | 日期，格式 `YYYY-MM-DD` |
| `phoneId` | Long | 否 | 手机号ID |
| `phoneIds` | String | 否 | 多手机号ID，逗号分隔 |
| `channel` | String | 否 | 默认 `wechat` |

**响应:** `200`
```json
{
  "date": "localdate",
  "incomeTotal": "bigdecimal",
  "expenseTotal": "bigdecimal",
  "neutralTotal": "bigdecimal",
  "incomeCount": "int",
  "expenseCount": "int",
  "neutralCount": "int"
}
```

---

### GET `/analytics/day-detail` — 某日收支明细
**认证:** 是

**参数:**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `date` | LocalDate | 是 | 目标日期 |
| `compareDate` | LocalDate | 否 | 对比日期 |
| `phoneId` | Long | 否 | 手机号ID |
| `phoneIds` | String | 否 | 多手机号ID |
| `channel` | String | 否 | 默认 `wechat` |

**响应:** `200`
```json
{
  "day": {
    "date": "localdate",
    "incomeTotal": "bigdecimal",
    "expenseTotal": "bigdecimal",
    "neutralTotal": "bigdecimal",
    "grandTotal": "bigdecimal",
    "incomeCount": "int",
    "expenseCount": "int",
    "neutralCount": "int",
    "incomeTransactions": [{  // TransactionBriefDto
      "id": "long",
      "tradeTime": "string",
      "tradeType": "string",
      "counterparty": "string",
      "product": "string",
      "incomeExpense": "string",
      "amountYuan": "bigdecimal",
      "paymentMethod": "string",
      "remark": "string",
      "billChannel": "string"
    }],
    "expenseTransactions": ["TransactionBriefDto[]"],
    "neutralTransactions": ["TransactionBriefDto[]"]
  },
  "compareDay": { /* 同上结构，可为 null */ }
}
```

---

### GET `/analytics/rolling-income-expense` — 近30天收支明细
**认证:** 是

**参数:**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `endDate` | LocalDate | 是 | 截止日期 |
| `phoneId` | Long | 否 | 手机号ID |
| `phoneIds` | String | 否 | 多手机号ID |
| `channel` | String | 否 | 默认 `wechat` |

**响应:** `200`
```json
{
  "rangeStart": "localdate",
  "rangeEnd": "localdate",
  "incomeTransactions": ["TransactionBriefDto[]"],
  "expenseTransactions": ["TransactionBriefDto[]"]
}
```

---

### GET `/analytics/month` — 某月每日收支
**认证:** 是

**参数:**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `year` | int | 是 | 年份 |
| `month` | int | 是 | 月份 1-12 |
| `phoneId` | Long | 否 | 手机号ID |
| `phoneIds` | String | 否 | 多手机号ID |
| `channel` | String | 否 | 默认 `wechat` |

**响应:** `200`
```json
[{
  "date": "localdate",
  "incomeTotal": "bigdecimal",
  "expenseTotal": "bigdecimal",
  "neutralTotal": "bigdecimal",
  "incomeGrowthPercent": "bigdecimal?",
  "expenseGrowthPercent": "bigdecimal?",
  "incomeCount": "int",
  "expenseCount": "int",
  "neutralCount": "int"
}]
```

---

### GET `/analytics/by-type` — 按类型汇总
**认证:** 是

**参数:**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `type` | String | 是 | `income` / `expense` / `neutral` |
| `from` | LocalDate | 否 | 开始日期 |
| `to` | LocalDate | 否 | 结束日期 |
| `phoneId` | Long | 否 | 手机号ID |
| `phoneIds` | String | 否 | 多手机号ID |
| `channel` | String | 否 | 默认 `wechat` |

**响应:** `200`
```json
{
  "type": "string",
  "from": "localdate?",
  "to": "localdate?",
  "totalAmount": "bigdecimal",
  "transactionCount": "long"
}
```

---

### GET `/analytics/real` — 真实收支
**认证:** 是

**参数:**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `from` | LocalDate | 否 | 开始日期 |
| `to` | LocalDate | 否 | 结束日期 |
| `phoneId` | Long | 否 | 手机号ID |
| `phoneIds` | String | 否 | 多手机号ID |
| `channel` | String | 否 | 默认 `wechat` |

**响应:** `200`
```json
{
  "from": "localdate?",
  "to": "localdate?",
  "realIncome": "bigdecimal",
  "realExpense": "bigdecimal",
  "realNeutral": "bigdecimal",
  "excludedPairTransactionCount": "long",
  "excludedTransactionIds": ["long[]"]
}
```

---

### GET `/analytics/by-counterparty` — 按交易对手汇总看板
**认证:** 是

**参数:**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `from` | LocalDate | 否 | 开始日期 |
| `to` | LocalDate | 否 | 结束日期 |
| `phoneId` | Long | 否 | 手机号ID |
| `phoneIds` | String | 否 | 多手机号ID |
| `channel` | String | 否 | 默认 `wechat` |
| `excludeRefundPairs` | boolean | 否 | 默认 `false` |

**响应:** `200`
```json
{
  "groups": [{
    "counterparty": "string",
    "incomeTotal": "bigdecimal",
    "expenseTotal": "bigdecimal",
    "neutralTotal": "bigdecimal",
    "incomeCount": "int",
    "expenseCount": "int",
    "neutralCount": "int",
    "lastTradeTime": "string?"
  }],
  "grandIncomeTotal": "bigdecimal",
  "grandExpenseTotal": "bigdecimal",
  "grandNeutralTotal": "bigdecimal"
}
```

---

### GET `/analytics/by-counterparty-detail` — 某对手详细交易
**认证:** 是

**参数:**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `counterparty` | String | 是 | 对手方名称 |
| `from` | LocalDate | 否 | 开始日期 |
| `to` | LocalDate | 否 | 结束日期 |
| `phoneId` | Long | 否 | 手机号ID |
| `phoneIds` | String | 否 | 多手机号ID |
| `channel` | String | 否 | 默认 `wechat` |
| `excludeRefundPairs` | boolean | 否 | 默认 `false` |

**响应:** `200`
```json
["TransactionBriefDto[]"]
```

---

## 8. 导入记录查询 `/api/wechat-imports`
**需:** `PERM_ANALYTICS`

### GET `/wechat-imports`
**参数:** `wechatUserId?: long`

**响应:** `200`
```json
[{
  "id": "long",
  "userId": "long",
  "personId": "long?",
  "phoneId": "long?",
  "mobileCn": "string",
  "sourceFile": "string",
  "channel": "string",
  "totalCount": "int",
  "incomeCount": "int",
  "expenseCount": "int",
  "neutralCount": "int",
  "incomeAmount": "bigdecimal",
  "expenseAmount": "bigdecimal",
  "neutralAmount": "bigdecimal",
  "exportTime": "instant?",
  "rangeStart": "instant?",
  "rangeEnd": "instant?",
  "createdAt": "instant"
}]
```

---

## 9. 微信用户查询 `/api/wechat-users`
**需:** `PERM_ANALYTICS`

### GET `/wechat-users`
**响应:** `200`
```json
[{
  "id": "long",
  "wechatNickname": "string",
  "channel": "string",
  "phoneId": "long?",
  "personId": "long?",
  "mobileCn": "string?",
  "archived": "boolean"
}]
```

---

## 10. 原始交易查询 `/api/original/transactions`
**需:** `PERM_ANALYTICS`

### GET `/original/transactions`
**参数:** `wechatUserId?: long`

**响应:** `200` — `TransactionBriefDto[]`

---

## 11. 备份事务 CRUD `/api/bkp/transactions`
**需:** `PERM_BKP_TX_CRUD`

### GET `/bkp/transactions` — 分页搜索
**参数:**
| 参数 | 类型 | 说明 |
|------|------|------|
| `channel` | String | 渠道筛选 |
| `billImportId` | Long | 导入批次ID |
| `tradeTimeFrom` | String | 交易时间起 |
| `tradeTimeTo` | String | 交易时间止 |
| `tradeType` | String | 交易类型 |
| `counterparty` | String | 交易对手 |
| `incomeExpense` | String | 收/支/中性 |
| `amountMin` | BigDecimal | 最小金额 |
| `amountMax` | BigDecimal | 最大金额 |
| `page` | int | 页码（默认0） |
| `size` | int | 每页条数（默认20） |
| `sort` | String | 排序字段（默认 tradeTime） |
| `direction` | String | `asc` / `desc`（默认 desc） |

**响应:** `200`
```json
{
  "content": [{
    "id": "long",
    "sourceTxId": "long?",
    "billImportId": "long",
    "billChannel": "string",
    "rowHash": "string?",
    "tradeTime": "string",
    "tradeType": "string",
    "counterparty": "string",
    "product": "string",
    "incomeExpense": "string",
    "amountYuan": "bigdecimal",
    "paymentMethod": "string",
    "status": "string",
    "tradeNo": "string",
    "merchantNo": "string",
    "remark": "string",
    "sourceFile": "string",
    "extraText": "string?",
    "archived": "boolean",
    "createdAt": "instant",
    "updatedAt": "instant"
  }],
  "totalElements": "long",
  "totalPages": "int",
  "number": "int",
  "size": "int"
}
```

---

### GET `/bkp/transactions/{id}` — 获取单条
**响应:** `200` — 同上 content 项结构

### POST `/bkp/transactions` — 新建
**请求体:**
```json
{
  "sourceTxId": "long?",
  "billImportId": "long",
  "billChannel": "string?",
  "rowHash": "string?",
  "tradeTime": "string",
  "tradeType": "string",
  "counterparty": "string?",
  "product": "string?",
  "incomeExpense": "string",
  "amountYuan": "bigdecimal",
  "paymentMethod": "string?",
  "status": "string?",
  "tradeNo": "string?",
  "merchantNo": "string?",
  "remark": "string?",
  "sourceFile": "string?",
  "extraText": "string?",
  "archived": "boolean?"
}
```
**响应:** `201`

### PUT `/bkp/transactions/{id}` — 更新
**请求体:** 同 POST
**响应:** `200`

### DELETE `/bkp/transactions/{id}` — 删除
**响应:** `204`

---

## 12. 备份恢复 `/api/bkp/restore`
**需:** `PERM_BKP_TX_CRUD`

### POST `/bkp/restore/wechat/day?date=YYYY-MM-DD`
### POST `/bkp/restore/wechat/month?year=2026&month=4`
### POST `/bkp/restore/wechat/year?year=2026`
### POST `/bkp/restore/wechat/all`
### POST `/bkp/restore/alipay/day?date=YYYY-MM-DD`
### POST `/bkp/restore/alipay/month?year=2026&month=4`
### POST `/bkp/restore/alipay/year?year=2026`
### POST `/bkp/restore/alipay/all`

**响应:** `200`
```json
{
  "restoredCount": "int",
  "message": "string"
}
```

---

## 13. 用户管理 `/api/admin/users`
**需:** `PERM_USER_ADMIN`

### GET `/admin/users/roles` — 所有角色码
**响应:** `200`
```json
["string"]   // 如 ["PERM_USER_ADMIN", "PERM_ANALYTICS", "PERM_BKP_TX_CRUD"]
```

---

### GET `/admin/users` — 用户列表
**响应:** `200`
```json
[{
  "id": "long",
  "username": "string",
  "passwordPlain": "string?",  // 仅创建时返回
  "enabled": "boolean",
  "roleCodes": ["string"],
  "boundPhones": ["string"]
}]
```

---

### POST `/admin/users` — 创建用户
**请求体:**
```json
{
  "username": "string",
  "password": "string",
  "roleCodes": ["string"]
}
```
**响应:** `201`

---

### DELETE `/admin/users/{id}` — 删除用户
**响应:** `204`

---

### PUT `/admin/users/{id}/password` — 修改密码
**请求体:**
```json
{ "password": "string" }
```
**响应:** `200`

---

### PUT `/admin/users/{id}/roles` — 更新角色
**请求体:**
```json
{ "roleCodes": ["string"] }
```
**响应:** `200`

---

### DELETE `/admin/users/{id}/phones?mobile=string` — 解绑手机号
**响应:** `204`

---

### PATCH `/admin/users/{id}/enabled` — 启用/禁用
**请求体:**
```json
{ "enabled": "boolean" }
```
**响应:** `200`

---

## 14. 手机号审核 `/api/admin/phone-bind-requests`
**需:** `PERM_USER_ADMIN`

### GET `/admin/phone-bind-requests` — 待审核列表
**响应:** `200`
```json
[{
  "id": "long",
  "userId": "long",
  "username": "string",
  "mobileCn": "string",
  "createdAt": "instant"
}]
```

---

### GET `/admin/phone-bind-requests/history` — 历史记录
**响应:** `200`
```json
[{
  "id": "long",
  "userId": "long",
  "username": "string",
  "mobileCn": "string",
  "createdAt": "instant",
  "status": "string",
  "reviewedAt": "instant",
  "reviewedByUsername": "string",
  "rejectReason": "string?"
}]
```

---

### POST `/admin/phone-bind-requests/{id}/approve` — 审核通过
**响应:** `200`

---

### POST `/admin/phone-bind-requests/{id}/reject` — 审核拒绝
**请求体:**
```json
{ "reason": "string?" }
```
**响应:** `200`

---

## 附录：通用 DTO 结构

### TransactionBriefDto
```json
{
  "id": "long",
  "tradeTime": "string",
  "tradeType": "string",
  "counterparty": "string",
  "product": "string",
  "incomeExpense": "string",
  "amountYuan": "bigdecimal",
  "paymentMethod": "string",
  "remark": "string",
  "billChannel": "string"
}
```

### PhoneOptionDto
```json
{ "id": "long", "mobileCn": "string" }
```

### PersonOptionDto
```json
{ "id": "long", "label": "string" }
```

# 学海 · 阅读产品功能规格

> **所属项目**：`C:\downloadforwork\othertool\ower\bill`（账单系统内嵌模块，非 SkillChange）  
> **文档状态**：v0.2 — 已确认产品决策（见 §0）  
> **终端**：首版仅 **H5**（现有 `createWebHashHistory` + Vue 3 + Element Plus）  
> **实现参考**：`frontend/src/views/XuehaiBookshelf.vue`、`XuehaiBIDashboard.vue`，后端 `XuehaiController`（`/api/xuehai`）

---

## 0. 已确认决策（2025-05）

| 项 | 决策 |
|----|------|
| 内容类型 | **均可**：网文、教育讲义、技能读物、社区长文等，统一进书库，用分类/标签区分 |
| 书架 vs 收藏 | **分开**：书架 = 正在追/要读完；收藏 = 想读备选，可不在书架 |
| 未登录 | **不可阅读**：无游客试读；进入学海阅读相关能力须先登录（与 bill 全局 `token` 门禁一致） |
| 终端 | **仅 H5**；不做微信小程序（推送、订阅消息首版不做） |
| 榜单 UI | **封面网格**（双列/多列卡片 + 可选角标排名），不用左侧 1～50 序号长列表 |
| 福利 Tab | **不要**；底部 4 Tab：书城、分类、书架、我的 |
| 付费 | **全部免费**；章节无 VIP/解锁 |
| 评分 | **要**；书籍展示均分，登录用户可打分（首版可不做书评长文） |
| 作者名 | **随意**；后台/上传填什么展示什么，可空则「佚名」 |
| 宿主 | 嵌入 **bill** 项目，账号与 `POST /api/auth/login` 共用 |

---

## 1. 产品定位

| 维度 | 说明 |
|------|------|
| 产品名 | **学海** |
| 一句话 | 榜单发现 → 详情 → 登录后阅读 → 书架/收藏/历史沉淀 |
| 对标 | 番茄小说的信息架构；实现形态为 bill 内 H5 子模块 |
| 非目标（首版） | 小程序、听书、付费章节、作者分成、签到福利 Tab、UGC 审核流水线 |

---

## 2. 与 bill 项目集成

### 2.1 技术栈（沿用）

- 前端：`bill/frontend` — Vue 3、Vue Router（Hash）、Element Plus、`src/api.js`（Bearer Token）
- 后端：`bill` Spring Boot — 已有 `@RequestMapping("/api/xuehai")`
- 登录：`localStorage.token`；`router.beforeEach` 已对白名单外路由拦登录；学海路由保持 `path.startsWith('/xuehai')` 在手机绑定豁免内（见 `router.js`）

### 2.2 路由规划（由现状演进）

| 现状 | 目标（H5 Hash） |
|------|-----------------|
| `/xuehai` → `XuehaiBookshelf.vue`（上传+全站书列表） | 书城首页或重定向；管理上传可迁到「我的-管理」或独立 `/xuehai/admin` |
| `/xuehai/bi` → BI 大屏 | 保留，管理员/运营用 |
| （无） | `/xuehai/rank/:type`、`/xuehai/book/:id`、`/xuehai/read/...`、`/xuehai/bookshelf`、`/xuehai/favorites`、`/xuehai/me` 等 |

### 2.3 与现有 API 的差异（待开发）

当前后端侧重：**书籍 CRUD、章节文件、点赞 toggle、收藏 toggle（单按钮）**。  
规格要求新增/拆分：

- **书架** `Bookshelf`（与收藏表分离）
- **榜单** `RankList`（推荐/完本/新书）
- **阅读进度 / 历史**
- **评分** `BookRating`（用户×书 唯一分）
- 正文接口 **强制登录**，去掉匿名 `listBooks` 若产品要求连榜单都需登录 — **建议：榜单/搜索可登录后可见，未登录仅能见登录页**（与「不可读」一致：未登录跳转 `/login`）

---

## 3. 信息架构（4 Tab，无福利）

```
┌─────────┬─────────┬─────────┬─────────┐
│  书城   │  分类   │  书架   │  我的   │
└─────────┴─────────┴─────────┴─────────┘
```

| Tab | 职责 |
|-----|------|
| **书城** | 搜索、Banner、三榜入口、继续阅读、猜你喜欢 |
| **分类** | 体裁筛选（网文/教育/…）、标签、排序 |
| **书架** | 已加入书架的书 + 更新角标 + 管理 |
| **我的** | 收藏列表入口、阅读历史、设置、账号、关于 |

**阅读器**：全屏二级页，不占 Tab。  
**收藏**：不在底部 Tab；在「我的」进入「我的收藏」，与书架逻辑分离。

---

## 4. 书城（首页）

### 4.1 结构

1. 顶栏：搜索  
2. Banner（运营，可选）  
3. **继续阅读**（有进度时 1 张横卡）  
4. **榜单入口**：推荐榜 | 完本榜 | 新书榜（点击进入 §5 榜单页）  
5. 猜你喜欢 / 编辑推荐（双列封面流）

### 4.2 书目卡片（通用）

| 字段 | 说明 |
|------|------|
| 封面 | 3:4，`GET /api/xuehai/books/{id}/cover` |
| 书名 | 2 行省略 |
| 作者 | 任意字符串，空则佚名 |
| 标签 | 最多 2 个 |
| 评分 | 均分，如 `9.2`（首版展示，见 §6） |
| 状态 | 连载 / 完结 |

---

## 5. 榜单体系

### 5.1 首版三榜

| type | 名称 | 规则 |
|------|------|------|
| `recommend` | 推荐榜 | 编辑权重 + 阅读量 + 评分（首版可人工序为主） |
| `finished` | 完本榜 | 仅已完结，按近 7 日阅读量 |
| `new_book` | 新书榜 | 上架 ≤30 天 |

### 5.2 榜单页 UI（已确认：封面网格）

- 顶 Tab：推荐 | 完本 | 新书  
- **双列封面网格**；左上角可选小角标 `1` `2` `3`…  
- 下拉刷新、上拉分页（20 条/页）  
- 点击封面 → 书籍详情  

---

## 6. 书籍详情

- 封面、书名、作者、分类、字数、连载状态、简介  
- **评分**：展示 `avgScore`、`ratingCount`；已登录且未评过 → 1～5 星或 10 分制提交（**仅打分，书评二期**）  
- 目录：章节列表（正/倒序）  
- 相似推荐（横滑 6 本）  

### 6.1 操作（均需登录）

| 按钮 | 行为 |
|------|------|
| 开始阅读 / 继续阅读 | 阅读器；定位云端进度 |
| 加入书架 | 写入 `Bookshelf`；重复提示「已在书架」 |
| 收藏 | 写入 `Favorite`；**不自动加入书架** |
| 从书架移除 / 取消收藏 | 分别在书架页、收藏页操作 |

**未登录**：访问 `/xuehai/*` 时由路由守卫跳转 `/login`，**不提供试读**。

---

## 7. 书架 · 收藏 · 历史

### 7.1 书架（Tab）

| 含义 | 用户主动「加入书架」= 要追、要看完 |
|------|-------------------------------------|
| 展示 | 封面网格；连载角标；最近章节；未读章数（有章节的书） |
| 更新 | 有新章节 → 红点 / 「更新」 |
| 排序 | 默认最近阅读时间；可切换添加时间、书名 |
| 管理 | 多选删除、置顶（建议上限 9） |
| 与收藏 | 在书架可「仅移出书架」；收藏状态不变 |
| 与历史 | 移出书架 **不删** 历史 |

### 7.2 收藏（我的 → 我的收藏）

| 含义 | 备选、想读；**不必**在书架 |
|------|---------------------------|
| 展示 | 封面列表/网格，无「未读章数」也可 |
| 操作 | 取消收藏；可一键「加入书架」 |
| 与点赞 | 现有 `like` 可保留为互动；与收藏、书架三者独立 |

> **与现状**：`XuehaiBookshelf.vue` 上 `favorite/toggle` 对应的是「收藏」语义；书架需 **新表 + 新接口**，首页改为书城而非「全站书架+上传」。

### 7.3 阅读历史（我的 → 阅读历史）

- 每次退出阅读器或切章上报  
- 含 **未加书架** 的书  
- 单删、清空；保留 200 条或 90 天（实现时二选一）  

### 7.4 阅读进度

- 字段：`bookId`, `chapterId`, `offset`, `updatedAt`  
- 多端：登录用户以云端 `updatedAt` 较大者为准  

---

## 8. 阅读器

- 章节切换、目录、上下滚动 + 左右点击区翻章  
- 字号、背景（白/护眼/夜间）、行距可选  
- 退出自动存进度 → 历史 + 进度表  
- **全部章节免费**，无 `lockType` / 广告解锁  

---

## 9. 我的

```
头像 + 昵称（bill 账号）
├── 我的收藏
├── 阅读历史
├── 阅读设置（字号、背景、翻页方式）
├── 消息（可选，首版可仅系统公告列表）
├── 账号与安全（改密等，复用 bill）
├── 意见反馈
├── 关于学海 / 协议
└── 退出登录
```

快捷入口「书架」→ 切到底部 Tab「书架」。  
**上传书籍/章节**：建议从普通用户首页挪到管理员菜单或 `/xuehai/admin`（保留现有上传能力，避免书城首页过于工具化）。

---

## 10. 搜索

- 书名、作者模糊搜  
- 本地最近搜索 10 条  
- 无结果 → 展示推荐榜前几名  

---

## 11. 评分（首版）

| 项 | 说明 |
|----|------|
| 展示 | 详情页、卡片角标显示均分（一位小数） |
| 提交 | 每用户每书 1 次，可允许改分（待实现时定） |
| 不参与 | 长书评、回复楼（二期） |
| 榜单 | 推荐榜算法可纳入 `avgScore` 权重 |

---

## 12. 内容模型

- `Book.contentType` 可选：`novel` / `edu` / `article` / `other`（筛选用，不限制上传）  
- `Book.author`：自由文本  
- `Chapter`：全部 `isFree = true`（字段可省略）  

---

## 13. 数据模型（目标）

### 13.1 内容

| 实体 | 主要字段 |
|------|----------|
| `Book` | id, title, author, coverUrl, intro, contentType, categoryId, tags[], wordCount, status, avgScore, ratingCount, publishAt |
| `Chapter` | id, bookId, title, sortOrder, filePath, wordCount |
| `RankEntry` | type, bookId, rank, score, periodDate |

### 13.2 用户（须 userId）

| 实体 | 说明 |
|------|------|
| `Bookshelf` | userId, bookId, addedAt, pinned, lastReadChapterId, lastReadAt |
| `Favorite` | userId, bookId, createdAt（**独立于书架**） |
| `BookLike` | 保留现有点赞逻辑即可 |
| `BookRating` | userId, bookId, score, updatedAt |
| `ReadHistory` | userId, bookId, chapterId, progress, readAt |
| `ReadProgress` | userId, bookId, chapterId, offset, updatedAt |

---

## 14. API 草案（在现有 `/api/xuehai` 上扩展）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/ranks/{type}` | 榜单，分页 |
| GET | `/books` | 书城/分类列表（query: category, contentType, sort） |
| GET | `/books/{id}` | 详情含评分、是否在书架/收藏 |
| GET | `/books/{id}/chapters` | 目录 |
| GET | `/chapters/{id}/content` 或现有 `file/inline` | **401 未登录** |
| POST | `/books/{id}/rating` | `{ score: 1-5 }` |
| POST | `/bookshelf` | `{ bookId }` |
| DELETE | `/bookshelf/{bookId}` | |
| GET | `/bookshelf` | 我的书架 |
| POST | `/favorites` | `{ bookId }`（与现有 toggle 可对齐） |
| DELETE | `/favorites/{bookId}` | |
| GET | `/favorites` | 我的收藏 |
| GET | `/history` | 阅读历史 |
| PUT | `/progress` | 上报进度 |
| GET | `/progress/{bookId}` | |
| POST | `/books/{id}/like/toggle` | 保留 |
| GET | `/stats` | 保留 BI |

**鉴权**：除登录注册外，学海读正文、阅读器、书架/收藏/评分/进度 **均需 Bearer Token**。

---

## 15. MVP 清单（bill 内 H5）

- [ ] 4 Tab 壳 + 路由  
- [ ] 书城 + 三榜（封面网格）  
- [ ] 分类筛选（含 contentType）  
- [ ] 详情 + 评分 + 加书架 / 收藏（分离）  
- [ ] 阅读器 + 进度 + 历史  
- [ ] 我的（收藏、历史、设置）  
- [ ] 未登录不可进入学海（或不可读正文，与路由统一）  
- [ ] 搜索  
- [ ] 迁移现有上传入口到管理位  

### V1.1

- [ ] 热门榜、章节更新标记优化、消息列表  

### V2

- [ ] 书评、听书、离线下载  

---

## 16. 页面路由（Hash）

| 路径 | 页面 |
|------|------|
| `#/xuehai` | 书城 |
| `#/xuehai/rank/:type` | 榜单 |
| `#/xuehai/category` | 分类 |
| `#/xuehai/book/:id` | 详情 |
| `#/xuehai/read/:bookId/:chapterId` | 阅读器 |
| `#/xuehai/bookshelf` | 书架 Tab |
| `#/xuehai/favorites` | 我的收藏 |
| `#/xuehai/search` | 搜索 |
| `#/xuehai/me` | 我的 |
| `#/xuehai/me/history` | 历史 |
| `#/xuehai/bi` | 管理 BI（已有） |

---

## 17. 文件索引

| 说明 | 路径 |
|------|------|
| 本文档 | `bill/docs/XUEHAI_READING_APP_SPEC.md` |
| 现有书架页 | `bill/frontend/src/views/XuehaiBookshelf.vue` |
| 现有 BI | `bill/frontend/src/views/XuehaiBIDashboard.vue` |
| 路由 | `bill/frontend/src/router.js` |
| 后端 | `bill/src/main/java/org/example/bill/web/XuehaiController.java` |

---

*v0.2 — 产品决策已锁定；下一步可写 `XUEHAI_UI_SPEC.md` 或 OpenAPI 细表。*

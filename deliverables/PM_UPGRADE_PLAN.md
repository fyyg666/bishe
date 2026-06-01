# 图书馆管理系统 V2.0 — 项目管理专业化升级方案

> **制定日期**: 2026-06-02  
> **代码扫描基准**: 2026-06-02 实际代码  
> **扫描范围**: 所有后端Entity + schema.sql + Flyway迁移 + 前端API函数 + Vue视图  
> **方法**: 逐文件Grep比对，不依赖历史报告

---

## 一、代码现状 — 好消息

你在多次迭代中已经修复了大量问题。旧报告（5月15日/6月1日）提到的以下问题**当前代码中已不存在**：

| 旧报告声称的问题 | 实际状态 | 代码证据 |
|---|---|---|
| Book `totalCount` 映射断裂 | ✅ 已修复 | `@TableField("total_quantity")` |
| Announcement `created_at` 列名错误 | ✅ 已修复 | `@TableField("create_time")` |
| VolunteerService `reviewer_id` 等3处映射 | ✅ 已修复 | 显式 `@TableField` 全部正确 |
| BorrowRecordMapper `BORROWED` 状态值 | ✅ 已修复 | 统一使用 `'BORROWING'` |
| BorrowRecordMapper `created_at` 列名 | ✅ 已修复 | 统一使用 `create_time` |
| BookServiceImpl `@CacheEvict beforeInvocation` | ✅ 已修复 | 三个注解全部移除 |
| Nginx `return 301` 死代码 | ✅ 已修复 | 整行删除 |
| `user.js` logout 未 await | ✅ 已修复 | `async function` + `await apiLogout()` |
| BorrowDetail 路由缺 `requiresAuth` | ✅ 已修复 | `meta: { requiresAuth: true }` |
| 默认密码 `123456` 硬编码 | ✅ 已修复 | 随机12位强密码生成 |
| 注册接口缺验证码 | ✅ 已修复 | `validateCaptcha()` 调用 |
| SeatReservation 缺 `seatId`/`roomId` | ✅ 已修复 | `seat.getRoomId()` / `seat.getId()` |
| 前端签到/签退UI | ✅ 已实现 | `handleCheckIn` / `handleCheckOut` |

**你的历史修复率非常高。以下只列出当前代码中实际存在的问题。**

---

## 二、当前代码中实际存在的问题

### 🔴 P0 — 会导致运行时崩溃

| # | 问题 | 文件 | 后果 |
|---|------|------|------|
| **1** | **V2.3.0 Flyway迁移破坏了Compensation实体** — 迁移脚本把 `compensation_order` 表的 `create_time`/`update_time` 重命名为 `created_at`/`updated_at`，但 `Compensation.java` 没有 `@TableField`，MyBatis-Plus 默认映射回 `create_time`/`update_time` | `V2.3.0__Schema_Quality_Refactor_To_S_Plus.sql:44-45` + `Compensation.java:58-60` | **赔偿功能完全不可用**，SELECT/INSERT 全部 `Unknown column` |
| **2** | **README 指导使用的 `schema.sql` 完全过时** — 缺22张表（branch, borrow_rule, notification, book_reservation, marc_record 等）、8个列、4处列名不匹配 | `schema.sql` + `README.md:49` | 按 README 初始化会**全系统崩溃** |

### 🟠 P1 — 安全或功能高风险

| # | 问题 | 文件 | 后果 |
|---|------|------|------|
| **3** | JWT密钥有硬编码默认值 `library-dev-jwt-secret-key-2026-min-32-chars`，无启动时强制检查 | `application.yml:86` | 忘记设环境变量时JWT可被伪造 |
| **4** | VolunteerService乐观锁被 `@TableField(exist = false)` 静默禁用 | `VolunteerService.java:78-80` | 并发修改冲突时数据可能不一致 |
| **5** | MySQL和MinIO凭据有默认回退值 | `application.yml:23,111-112` | 配置遗漏时的安全漏洞 |

### 🟡 P2 — 代码质量

| # | 问题 | 影响 |
|---|------|------|
| **6** | 14个前端API函数定义了但从未被任何视图/Store使用 | 包体积浪费、代码审查噪音 |
| **7** | `BookCategory.java` 和 `ReadingRoom.java` 的 `createTime` 用 `String` 而非 `LocalDateTime` | 类型不安全 |
| **8** | 所有视图的错误处理仅 `ElMessage.error` 弹一闪消息，无内联重试 | 网络故障时用户体验差 |

### 🟢 P3 — 文档/技术债务

| # | 问题 |
|---|------|
| **9** | `schema.sql` 应删除或同步到Flyway基准版本 |
| **10** | 前端组件 `TableSkeleton.vue`、`ImageLazyLoad.vue` 从未被导入 |

---

## 三、修复方案

### Sprint 1（本周）: 2个P0 + 文档修复 — 约6小时

| 任务 | 耗时 | 做法 |
|------|:--:|------|
| **P0-1**: 修复Compensation实体 | 1h | `Compensation.java` 加 `@TableField("created_at")` + `@TableField("updated_at")`，运行V2.3.0后验证 |
| **P0-2**: 删除或更新 `schema.sql` | 1h | 删除 `schema.sql`，README改为指向Flyway |
| **P3-1**: 整理项目文档 | 2h | 审查报告归档到 `deliverables/reviews/` |
| 环境一致性验证 | 2h | 跑一遍完整启动流程确认无残留问题 |

### Sprint 2（第2-3周）: P1修复 — 约4小时

| 任务 | 耗时 |
|------|:--:|
| **P1-1**: JWT密钥启动时强制检查 | 1h |
| **P1-2**: VolunteerService乐观锁修复 | 0.5h |
| **P1-3**: 凭据默认值加固 | 0.5h |
| **P2-1**: 清理14个死代码API函数 | 1h |
| **P2-2**: BookCategory/ReadingRoom类型统一 | 1h |

### Sprint 3（第4-5周）: P2 UX完善 — 约3小时

| 任务 | 耗时 |
|------|:--:|
| 核心视图增加内联错误横幅+重试按钮 | 2h |
| 前端死组件清理 | 1h |

---

## 四、之前PM方案中仍然有效的部分

以下框架性内容无需修改，继续使用：
- **Scrum-Lite框架**设计合理（小团队+论文截止日期）
- **三级质量门禁**体系（个人自检→CI自动化→Sprint Review）
- **Definition of Done** 7项检查标准
- **文档管理重构方案**（`deliverables/backlog/`、`deliverables/reviews/`）

---

*本文件基于2026-06-02实际代码扫描结果，非历史报告。*

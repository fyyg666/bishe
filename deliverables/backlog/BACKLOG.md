# 产品待办列表 (Product Backlog)

> 版本: v2.0 | 基于: 2026-06-02 实际代码扫描  
> 旧报告中的16个问题已验证为已修复，以下仅列出当前代码中实际存在的问题。

---

## 统计概览

| 优先级 | 数量 | 来源 |
|:------:|:----:|------|
| P0 — 运行时崩溃 | 2 | V2.3.0迁移 + schema.sql过时 |
| P1 — 安全/功能高风险 | 3 | JWT密钥 + 乐观锁 + 凭据 |
| P2 — 代码质量 | 3 | 死代码 + 类型 + UX |
| P3 — 文档/技术债务 | 2 | schema.sql清理 + 死组件 |
| **合计** | **10** | — |

---

## P0 — 阻塞性

| ID | 任务 | 文件 | Sprint | 状态 |
|----|------|------|:------:|:----:|
| STORY-001 | Compensation实体V2.3.0列名修复 | `Compensation.java` + 验证 | Sprint 1 | ⬜ |
| STORY-002 | 删除或同步 schema.sql + 更新 README | `schema.sql`, `README.md` | Sprint 1 | ⬜ |

**STORY-001 详细说明**：
- 问题：`V2.3.0` Flyway迁移将 `compensation_order.create_time` 重命名为 `created_at`，但 `Compensation.java` 无 `@TableField` 仍映射 `create_time`
- 修复：在 `Compensation.java:58-60` 添加 `@TableField("created_at")` 和 `@TableField("updated_at")`
- 验证：启动应用后执行赔偿查询，确认无SQL错误

---

## P1 — 高优先级

| ID | 任务 | 文件 | Sprint | 状态 |
|----|------|------|:------:|:----:|
| STORY-003 | JWT密钥启动时强制检查（拒绝默认值） | `JwtUtils.java`, `application.yml` | Sprint 2 | ⬜ |
| STORY-004 | VolunteerService乐观锁修复 | `VolunteerService.java:78-80` | Sprint 2 | ⬜ |
| STORY-005 | 数据库/MinIO凭据加固 | `application.yml` | Sprint 2 | ⬜ |

---

## P2 — 中等

| ID | 任务 | 文件 | Sprint | 状态 |
|----|------|------|:------:|:----:|
| STORY-006 | 清理14个前端死代码API函数 | `api/book.js`, `borrow.js`, `reader.js` 等 | Sprint 2 | ⬜ |
| STORY-007 | BookCategory/ReadingRoom类型统一 | `BookCategory.java`, `ReadingRoom.java` | Sprint 2 | ⬜ |
| STORY-008 | 核心视图增加内联错误横幅+重试 | `SeatList.vue`, `BookList.vue` 等 | Sprint 3 | ⬜ |

---

## P3 — 技术债务

| ID | 任务 | Sprint | 状态 |
|----|------|:------:|:----:|
| STORY-009 | 删除未使用的组件 `TableSkeleton.vue`、`ImageLazyLoad.vue` | Sprint 3 | ⬜ |
| STORY-010 | 审查报告归档整理 | Sprint 1 | ⬜ |

---

## 已修复问题清单（旧报告误报）

以下16个问题在旧报告中标记为缺陷，但**当前代码中已不存在**：

| 旧报告来源 | 原称问题 | 验证方式 |
|-----------|---------|---------|
| BA #11.1 | Book totalCount映射 | `@TableField("total_quantity")` 存在 |
| BA #11.2 | Announcement created_at | `@TableField("create_time")` 存在 |
| BA #3 | VolunteerService 3处映射 | 显式`@TableField`全部正确 |
| BA #4 | BorrowRecordMapper状态值 | 统一`'BORROWING'` |
| BA #5 | BorrowRecordMapper列名 | 统一`create_time` |
| CR #C-1 | CacheEvict beforeInvocation | 三个注解全部移除 |
| CR #C-2 | Nginx return 301 | 整行删除 |
| CR #H-1 | user.js logout | `await apiLogout()` |
| CR #H-3 | BorrowDetail路由 | `requiresAuth: true` |
| CR #H-4 | 默认密码123456 | 随机生成 |
| CR #H-2 | 注册验证码 | `validateCaptcha()` |
| BA #6 | SeatReservation seatId | 显式设置已完成 |
| FA #P0-1 | 签到签退UI | `handleCheckIn/Out` 已实现 |

> BA = backend-module-analysis, CR = comprehensive-code-review, FA = frontend-module-analysis

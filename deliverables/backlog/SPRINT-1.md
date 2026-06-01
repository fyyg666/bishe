# Sprint 1 计划

> **Sprint目标**: 修复2个P0崩溃问题 + 环境整理  
> **时间**: 2026-06-02 ~ 2026-06-08 (1周)  
> **承诺**: 3个任务，约4小时

---

## Sprint Backlog

| ID | 任务 | P | 预计耗时 | 状态 |
|----|------|:--:|:------:|:----:|
| STORY-001 | Compensation实体V2.3.0列名修复 | P0 | 1h | ⬜ |
| STORY-002 | 删除过时schema.sql + 更新README | P0 | 1h | ⬜ |
| STORY-010 | 审查报告归档整理 | P3 | 2h | ⬜ |

---

## 任务详情

### STORY-001: Compensation实体V2.3.0列名修复

**问题**: Flyway `V2.3.0` 迁移将 `compensation_order` 表的 `create_time`→`created_at`、`update_time`→`updated_at`，但 `Compensation.java:58-60` 没有 `@TableField` 注解，MyBatis-Plus 默认映射 `create_time`/`update_time`（已不存在）。

**修复**: 在 `Compensation.java` 添加：
```java
@TableField("created_at")
private LocalDateTime createTime;

@TableField("updated_at")
private LocalDateTime updateTime;
```

**验证**: 启动应用 → 创建赔偿单 → 查询赔偿列表 → 确认无SQL错误

### STORY-002: 删除过时schema.sql

**问题**: `schema.sql` 缺22张表、多列和多处列名不匹配，README第49行指引用户执行此文件。

**修复**: 
1. 删除 `backend/src/main/resources/schema.sql`
2. README数据库初始化改为指向Flyway自动迁移
3. 确认 `application.yml` Flyway配置正常

### STORY-010: 文档归档

清理项目根目录散落的7份历史报告，归档到 `deliverables/reviews/`。

---

## 每日站会

| 日期 | 完成 | 计划 | 障碍 |
|------|------|------|------|
| 6/2 | Sprint Planning | — | — |
| 6/3 | | | |
| 6/4 | | | |
| 6/5 | | | |
| 6/6 | | | |
| 6/7 | | | |
| 6/8 | Sprint Review + Retro | | |

---

## Sprint Review 检查单

- [ ] STORY-001: 赔偿功能可正常使用
- [ ] STORY-002: README初始化路径正确
- [ ] STORY-010: 根目录清理完成
- [ ] 全链路启动验证通过

---

*注：之前方案中列出的旧报告问题（Book映射、CacheEvict、Nginx等）已全部修复，本Sprint只需要处理2个真正存在的代码问题。*

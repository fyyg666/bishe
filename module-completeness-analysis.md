# 图书馆管理系统 V2.0 - 模块功能完整性分析报告

> 分析日期: 2026-05-15  
> 分析范围: 全部后端Controller/Service/Entity + 前端View/API/Store/Router/DB Schema

---

## 一、架构全景总览

```
┌─────────────────────────────────────────────────────────────┐
│                    前端 (Vue 3 + Element Plus)                │
│  Router(Vue Router) → Views(14个页面) → Stores(Pinia,4个)   │
│       → API(axios,10个模块) → utils(request/auth)           │
└──────────────────────┬──────────────────────────────────────┘
                       │ HTTP REST API (context-path: /api/v1)
┌──────────────────────┴──────────────────────────────────────┐
│                    后端 (Spring Boot 3.2.5)                   │
│  Controller(11个) → Service Interface(13个) → Impl(17个)    │
│       → Mapper(11个) → MyBatis → MySQL 8.0                   │
│  JWT Security | Redis Cache | Redisson Lock | Caffeine L1    │
└──────────────────────┬──────────────────────────────────────┘
                       │
┌──────────────────────┴──────────────────────────────────────┐
│                  数据库 (library_system)                      │
│  14张表: sys_user, book, book_category, borrow_record,       │
│  reading_room, seat, seat_reservation, credit_log,           │
│  announcement, volunteer_service, compensation_order,        │
│  statistics_daily, sys_operation_log, sys_config             │
└─────────────────────────────────────────────────────────────┘
```

---

## 二、模块功能完整性检查

### ✅ 2.1 用户认证模块

| 功能 | 后端 | 前端 | 状态 |
|------|:----:|:----:|:----:|
| 用户登录 (JWT双Token) | ✅ | ✅ | 完整 |
| 用户注册 | ✅ | ✅ | 完整 |
| Token刷新 | ✅ | ✅ | 完整 |
| 退出登录(Token黑名单) | ✅已修复 | ✅ | 完整 |
| 密码修改 | ✅ | 可能缺失 | 需确认 |
| 验证码 | ✅ | ✅ | 完整 |
| 账户锁定(5次失败) | ✅ | N/A | 完整 |
| 密码强度校验 | ✅在Service | 前端无展示 | ⚠️ 前端缺失提示 |

### ✅ 2.2 图书管理模块

| 功能 | 后端 | 前端 | 状态 |
|------|:----:|:----:|:----:|
| 图书列表(分页+搜索) | ✅ | ✅ | 完整 |
| 图书详情 | ✅ | ✅ | 完整 |
| 新增图书 | ✅ | ✅ | 完整 |
| 编辑图书 | ✅ | ✅ | 完整 |
| 删除图书 | ✅ | ✅ | 完整 |
| ISBN去重校验 | ✅ | ✅ | 完整 |
| 热门图书推荐 | ✅ | 未在前端展示 | ⚠️ 前端缺失 |
| 新书推荐 | ❌Mapper有SQL | 无 | ❌ 后端无API暴露 |
| 分类管理 | ❌无Controller/Service | 前端硬编码4个分类 | ❌ 完全缺失 |
| 图书库存管理 | ✅ | ✅ | 完整 |

### ⚠️ 2.3 借阅管理模块

| 功能 | 后端 | 前端 | 状态 |
|------|:----:|:----:|:----:|
| 借阅图书 | ✅ | ✅ | 完整 |
| 归还图书 | ✅ | ✅ | 完整 |
| 续借图书 | ✅ | ✅ | 完整 |
| 借阅记录列表(我的) | ✅ | ✅ | 完整 |
| 所有借阅记录(管理员) | ✅ | 未明确 | ⚠️ 前端可能缺失 |
| 逾期检查 | ✅定时任务 | 仅展示状态 | 可接受 |
| 罚款金额展示 | ✅ | 前端未展示 | ⚠️ 前端缺失 |
| 借阅限制(5本/人) | ✅ | 前端无提示 | ⚠️ 前端缺失 |

### ⚠️ 2.4 座位预约模块

| 功能 | 后端 | 前端 | 状态 |
|------|:----:|:----:|:----:|
| 查看座位列表 | ✅ | ✅ | 完整 |
| 预约座位 | ✅有Bug | ✅ | ⚠️ 运行时可能崩溃 |
| 取消预约 | ✅ | ✅ | 完整 |
| 签到 | ✅ | ❌ 前端无UI | ⚠️ 流程断裂 |
| 签退 | ✅ | ❌ 前端无UI | ⚠️ 流程断裂 |
| 我的预约列表 | ✅ | ✅ | 完整 |
| 可视化座位地图 | ✅ | ✅SeatMap.vue | 完整 |
| 违约检测(3次封禁72h) | ✅ | ✅ | 完整 |
| 阅览室管理 | ❌ 仅查询 | ❌ 前端无管理界面 | ❌ 完全缺失 |

### ✅ 2.5 信用积分模块

| 功能 | 后端 | 前端 | 状态 |
|------|:----:|:----:|:----:|
| 查看积分 | ✅ | ⚠️Dashboard格式不匹配 | ⚠️ Bug(见下) |
| 积分日志 | ✅ | ✅CreditView.vue | 完整 |
| 积分规则 | N/A | ✅前端硬编码 | 可接受 |
| 积分等级(铜/银/金/白金) | ✅Constant定义 | 前端无展示 | ⚠️ 前端缺失 |
| 借阅奖励/归还奖励 | ✅ | ✅ | 完整 |
| 逾期扣分 | ✅ | ✅ | 完整 |
| 签到加分 | ✅ | ✅ | 完整 |

### ✅ 2.6 公告管理模块

| 功能 | 后端 | 前端 | 状态 |
|------|:----:|:----:|:----:|
| 公告列表(分页) | 🔴列名映射全错 | ✅ | 🔴 运行时崩溃 |
| 公告详情 | 🔴列名映射全错 | ✅ | 🔴 运行时崩溃 |
| 新增公告 | 🔴列名映射全错 | ✅ | 🔴 运行时崩溃 |
| 编辑公告 | 🔴列名映射全错 | ✅ | 🔴 运行时崩溃 |
| 删除公告 | 🔴列名映射全错 | ✅ | 🔴 运行时崩溃 |
| 发布公告 | 🔴列名映射全错 | ✅ | 🔴 运行时崩溃 |
| 最新公告 | 🔴列名映射全错 | ✅Dashboard | 🔴 运行时崩溃 |

### ✅ 2.7 志愿服务模块

| 功能 | 后端 | 前端 | 状态 |
|------|:----:|:----:|:----:|
| 志愿服务申请 | ✅ | ✅ | 完整 |
| 志愿服务列表 | ✅ | ✅ | 完整 |
| 审核志愿服务 | ✅ | 未明确 | ⚠️ 前端可能缺失 |
| 志愿服务统计 | ✅ | 未明确 | ⚠️ 前端可能缺失 |
| 志愿时长统计 | ✅ | 未明确 | ⚠️ 前端可能缺失 |

### ✅ 2.8 统计分析模块

| 功能 | 后端 | 前端 | 状态 |
|------|:----:|:----:|:----:|
| 综合概览 | ✅ | ⚠️仅管理员可见 | 设计如此 |
| 借阅趋势(ECharts) | ✅ | ✅ | 完整 |
| 图书统计 | ✅ | 未使用该API | ⚠️ 前端未展示 |
| 读者统计 | ✅ | 未使用该API | ⚠️ 前端未展示 |
| 座位统计 | ⚠️仅管理员 | ⚠️非管理员走catch | 可接受 |
| 月度统计 | ✅ | Statistics.vue | 完整 |
| 分类分布 | ✅ | Statistics.vue | 完整 |
| 热门图书排行 | ✅ | 未展示 | ⚠️ 前端缺失 |
| 座位热力图 | ✅ | 未展示 | ⚠️ 前端缺失 |

### ⚠️ 2.9 读者管理模块

| 功能 | 后端 | 前端 | 状态 |
|------|:----:|:----:|:----:|
| 读者列表(分页) | ✅ | ✅ | 完整 |
| 读者详情 | ✅ | 未明确 | 可接受 |
| 读者禁用/启用 | ✅ | 未明确 | ⚠️ 前端可能缺失 |
| 读者信息编辑 | ✅ | 未明确 | ⚠️ 前端可能缺失 |

### ✅ 2.10 赔偿管理模块

| 功能 | 后端 | 前端 | 状态 |
|------|:----:|:----:|:----:|
| 创建赔偿订单 | ✅ | ✅ | 完整 |
| 赔偿列表 | ✅ | ✅ | 完整 |
| 现金赔偿 | ✅ | 未明确 | ⚠️ 前端可能缺失 |
| 积分抵扣赔偿 | ✅ | 未明确 | ⚠️ 前端可能缺失 |
| 志愿服务抵扣赔偿 | ✅ | 未明确 | ⚠️ 前端可能缺失 |
| 取消赔偿 | ✅ | 未明确 | ⚠️ 前端可能缺失 |

---

## 三、发现的关键问题

### 🔴 Critical: SeatReservation 插入将失败（运行时崩溃）

**现象**: `SeatServiceImpl.createReservation()` 创建预约记录时未设置 `roomId` 和 `seatId` 字段，但数据库 `seat_reservation` 表中这两个字段为 `NOT NULL`。

**影响**: 任何座位预约操作将抛出 `java.sql.SQLException` 并回滚事务。

**代码**:
- `SeatServiceImpl.java` 第396-410行: 未设置 roomId 和 seatId
- `schema.sql` 第152-153行: `room_id BIGINT NOT NULL`, `seat_id BIGINT NOT NULL`

**修复建议**: 根据 `request.getSeatNumber()` 查询 `seat` 表获取对应的 `room_id` 和 `seat_id`。

### 🔴 Critical: Book实体 totalCount 字段与数据库列不匹配

**现象**: Book实体字段 `totalCount` 通过MyBatis-Plus默认映射为 `total_count`，但数据库列为 `total_quantity`。

**影响**: 
1. 新增图书时 `totalCount` 值不会被写入数据库（写入不存在的列）
2. 查询时 `totalCount` 始终为null
3. 更新图书库存时总量计算错误

**代码**:
- `Book.java` 第59行: `private Integer totalCount;`（无@TableField注解）
- `schema.sql` 第68行: `total_quantity INT DEFAULT 1`

**修复建议**: 在 `totalCount` 字段上添加 `@TableField("total_quantity")` 注解。

### 🟠 High: Dashboard积分数据显示为0

**现象**: Dashboard.vue 调用 `/credits` 后端返回整数积分（如100），但前端代码 `res.data.score` 试图访问对象的 `score` 属性，导致 `undefined` → `0`。

**影响**: Dashboard上"我的积分"始终显示0。

**代码**:
- `Dashboard.vue` 第284行: `stats.value.creditScore = res.data.score || 0`
- `CreditController.java` 第54行: `return ApiResponse.success(credit);`（credit是Integer）

**修复建议**: 改为 `stats.value.creditScore = res.data || 0`

### 🟠 High: Dashboard座位统计数据对非管理员永远为0

**现象**: `loadSeatStats()` 调用 `/statistics/seats` 需要ADMIN/LIBRARIAN角色。对于普通读者，API返回403，catch块忽略异常，座位统计保持为0。

**代码**:
- `Dashboard.vue` 第254行: `promises.push(loadSeatStats())` - 所有用户都会调用
- `StatisticsController.java` 第112行: `@PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")`

**修复建议**: 为普通用户提供公开的座位统计数据接口，或在前端判断角色后决定是否调用。

### 🟠 High: BorrowRecord 非DB字段未通过关联查询填充

**现象**: `BorrowRecord` 实体中的 `username`, `bookTitle`, `bookIsbn` 均为 `@TableField(exist = false)`。默认的 MyBatis-Plus 查询（`selectPage`, `selectById`）不会进行JOIN查询，这些字段始终为null。

**影响**: 借阅记录列表和详情中，用户名、图书名、ISBN为空。

**代码**:
- `BorrowRecord.java` 第36-37, 47-48, 54-56行: 三个字段标记为 `@TableField(exist = false)`
- `BorrowRecordMapper.xml` - 需确认是否有自定义JOIN查询

**修复建议**: 在Mapper XML或@Select注解中编写JOIN查询，或用MyBatis-Plus的 `@Results` 注解。

### 🟡 Medium: 数据库表 `sys_operation_log` 无对应Java代码

**现象**: `schema.sql` 定义了 `sys_operation_log` 表但无对应的 Entity/Mapper/Service。

**影响**: 该表永远不会被写入数据，仅作为"日志表设计"存在于论文中。

**修复建议**: 实现 OperationLog AOP 切面将请求日志写入数据库，或将其从schema中移除。

### 🟡 Medium: 前端BookList分类硬编码

**现象**: `BookList.vue` 第29-45行硬编码了4个分类选项（文学/科技/历史/艺术），而后端数据库中实际有8个分类。

**影响**: 搜索筛选时只能选4个分类，与数据库实际数据不符。

**修复建议**: 添加 `/categories` API接口，前端动态加载分类。

### 🟡 Medium: Cache配置中缺少 `announcements` 缓存

**现象**: `AnnouncementController` 使用 `@CacheEvict(value = "announcements")`，但 `CaffeineConfig.java` 中配置的缓存名称为 `{"books", "readers", "hotBooks", "userSessions", "statistics"}`。

**影响**: 公告缓存失效操作无实际效果。

**修复建议**: 在CaffeineConfig中添加 `announcements` 缓存配置。

### 🟢 Low: 前端部分功能为"开发中"

| 位置 | 功能 | 状态 |
|------|------|:----:|
| BookList.vue | 导出功能 | 提示"功能开发中" |
| BorrowList.vue | 详情功能 | 提示"功能开发中" |
| 热门图书推荐 | 前端展示 | 缺失 |
| 图书分类管理 | 前端管理界面 | 缺失 |
| 阅览室管理 | 前端管理界面 | 缺失 |
| Dashboard credit level | 等级展示 | 缺失 |

---

## 三（补充）. 后端分析新增发现的重大问题

以下是在初步审查后，通过深度分析又发现的关键问题：

### 🔴 Critical: Announcement 实体列名映射全错

| 层面 | 字段 | 说明 |
|------|------|------|
| schema.sql | `create_time DATETIME`, `update_time DATETIME` | 数据库真实列名 |
| Announcement.java | `@TableField(value = "created_at")` | ❌ 实体映射到了错误的列名 |
| DB 实际 | `create_time` 存在, `created_at` **不存在** | |

**后果**: 公告的 INSERT/UPDATE/SELECT 全部因列名不匹配而失败。**公告模块代码完整但完全不可用。**

### 🔴 Critical: VolunteerService 字段映射全部错误

| 实体字段 | @TableField 映射值 | DB 实际列名 | 结果 |
|---------|-------------------|------------|:----:|
| `reviewerId` | `"approved_by"` | `reviewer_id` | ❌ |
| `reviewTime` | `"approved_at"` | `review_time` | ❌ |
| `reviewRemark` | `"remark"` | `review_remark` | ❌ |

**后果**: 志愿服务的审核操作全部无法正确存储/查询。

### 🔴 Critical: BorrowRecordMapper 状态值不匹配

Mapper 中的多个 `@Select` 查询硬编码了 `status = 'BORROWED'`，但系统和数据库存储的状态值是 `'BORROWING'`。导致：
- `selectCurrentBorrows` 永远返回空列表
- `countCurrentBorrows` 永远返回0

### 🟠 High: BorrowRecordMapper 使用错误的列名

多个 @Select SQL 中使用了 `ORDER BY created_at DESC` 和 `DATE(created_at)`，但数据库列名是 `create_time`。这些查询会在运行时抛出 SQL 异常。

### 🟠 High: 6 项 README 承诺功能完全缺失

1. **密码强度校验** - 无任何实现代码
2. **新书推荐 API** - Mapper有SQL但无Controller暴露
3. **图书分类管理 CRUD** - 无Controller/Service
4. **阅览室管理 CRUD** - 只有查询
5. **座位管理 CRUD** - 无添加/删除/修改API
6. **积分等级查询（铜牌/银牌/金牌/白金）** - 有常量无API

### 🟡 Medium: 其他问题

- `CompensationServiceImpl.processCreditPayment()` 硬编码 `"DAMAGE"` 而非根据 `compType` 动态选择 LOST/DAMAGE
- `SeatRedisCache` 类已实现但未被任何地方调用
- `AccountLockServiceImpl.isLockedByUsername()` 返回 false（未真正实现）

---

## 四、数据库表与实体映射完整性

| 表名 | 对应Entity | 对应Mapper | 状态 |
|------|:----------:|:----------:|:----:|
| sys_user | User.java | UserMapper.java | ✅ |
| book | Book.java | BookMapper.java | ⚠️ `totalCount`字段名不匹配 |
| book_category | BookCategory.java | BookCategoryMapper.java | ✅ |
| borrow_record | BorrowRecord.java | BorrowRecordMapper.java | ⚠️ 非DB字段未关联查询 |
| reading_room | ReadingRoom.java | ReadingRoomMapper.java | ✅ |
| seat | Seat.java | SeatMapper.java | ✅ |
| seat_reservation | SeatReservation.java | SeatReservationMapper.java | 🔴 插入时缺room_id/seat_id |
| credit_log | CreditLog.java | CreditLogMapper.java | ✅ |
| announcement | Announcement.java | AnnouncementMapper.java | ✅ |
| volunteer_service | VolunteerService.java | VolunteerServiceMapper.java | ✅ |
| compensation_order | Compensation.java | CompensationMapper.java | ✅ |
| statistics_daily | StatisticsDaily.java | StatisticsDailyMapper.java | ✅ |
| sys_operation_log | ❌无Entity | ❌无Mapper | 🟡 死表 |
| sys_config | ❌无Entity | ❌无Mapper | ⚠️ 通过JdbcTemplate直接访问 |

---

## 五、流程链完整性

### 5.1 用户借书流程
```
Login → 搜索图书 → 查看图书详情 → 借阅 → 查看借阅记录 → 归还/续借
  ✅      ✅         ✅            ✅     ✅              ✅
```

### 5.2 座位预约流程
```
Login → 查看座位地图 → 选择座位 → 预约 → 签到 → 签退 → 取消
  ✅      ✅            ✅        🔴    ❌    ❌    ✅
```
**瓶颈**: 预约座位时roomId/seatId未设置，运行时会崩溃；签到/签退前端无UI，流程断裂

### 5.3 信用积分流程
```
借书+5分 → 按时还+1分 → 逾期每天-5分 → 签到+1分 → 查看积分日志
  ✅         ✅            ✅             ✅          ✅
```
**瓶颈**: Dashboard显示积分始终为0（格式不匹配）

### 5.4 管理端流程
```
管理员登录 → 图书管理(CRUD) → 读者管理 → 公告管理 → 统计分析 → 赔偿管理
  ✅          ✅               ⚠️部分缺失   ⚠️部分缺失    ✅        ⚠️部分缺失
```

---

## 六、总结

### 严重问题（必须修复）
1. **🔴 Announcement列名全错** - `created_at`→`create_time`，所有CRUD失败
2. **🔴 VolunteerService字段映射全错** - 3个review字段列名全部不匹配
3. **🔴 Book.totalCount字段不匹配** - 映射到total_count而非total_quantity
4. **🔴 BorrowRecordMapper状态值'BORROWED'** - 应为'BORROWING'，查询永远返回0
5. **🔴 SeatReservation插入失败** - createReservation未设roomId/seatId
6. **🔴 签到/签退前端无UI** - 用户无法完成座位流程
7. **🟠 Dashboard积分显示为0** - res.data.score应为res.data
8. **🟠 BorrowRecordMapper用错列名created_at** - 应为create_time
9. **🟠 BorrowRecord非DB字段未填充** - username/bookTitle/bookIsbn始终为null
10. **🟠 Dashboard座位统计403** - 非管理员用户调用无效

### 次要问题（建议修复）
11. **🟡 6项README功能完全缺失** - 密码强度、新书推荐、分类CRUD、阅览室CRUD、座位CRUD、积分等级API
12. **🟡 sys_operation_log无代码** - 死表需清理或实现
13. **🟡 分类硬编码** - 前后端分类数据不一致
14. **🟡 Announcements缓存不生效** - 缓存名未注册
15. **🟡 Compensation硬编码DAMAGE** - 丢失/损坏不区分
16. **🟢 多个"开发中"功能** - 导出、详情、热门图书展示等
17. **🟢 信用等级前端展示缺失** - 铜/银/金/白金额度
18. **🟢 死代码** - TableSkeleton/ImageLazyLoad组件未使用

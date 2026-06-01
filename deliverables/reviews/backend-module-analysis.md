# 图书馆管理系统 V2.0 后端模块分析报告

**分析日期**: 2026-05-15  
**分析范围**: `backend/src/main/java/com/library/system`  
**目标**: 发现缺失功能、未完成的业务流、断裂的调用链

---

## 目录

1. [Auth 认证模块](#1-auth-认证模块)
2. [Book 图书模块](#2-book-图书模块)
3. [Borrow 借阅模块](#3-borrow-借阅模块)
4. [Seat 座位模块](#4-seat-座位模块)
5. [Credit 信用模块](#5-credit-信用模块)
6. [Compensation 赔偿模块](#6-compensation-赔偿模块)
7. [Volunteer 志愿服务模块](#7-volunteer-志愿服务模块)
8. [Announcement 公告模块](#8-announcement-公告模块)
9. [Reader 读者管理模块](#9-reader-读者管理模块)
10. [Statistics 统计模块](#10-statistics-统计模块)
11. [跨模块的实体-Schema 字段映射断裂问题](#11-跨模块的实体-schema-字段映射断裂问题)
12. [README 功能覆盖检查](#12-readme-功能覆盖检查)
13. [汇总：缺失功能和断裂链条](#13-汇总缺失功能和断裂链条)

---

## 1. Auth 认证模块

### 调用链
```
AuthController → AuthService → AuthServiceImpl → UserMapper → sys_user
```

### Controller 覆盖度
| 端点 | 状态 | 备注 |
|------|------|------|
| POST /auth/login | ✅ 完整 | 含验证码校验、账号锁定检查 |
| POST /auth/register | ✅ 完整 | 生成读者卡号 |
| POST /auth/refresh | ✅ 完整 | 双 Token 刷新 |
| POST /auth/logout | ✅ 完整 | Token 黑名单 |
| GET /auth/info | ✅ 完整 | 获取当前用户信息 |

### 功能遗漏
1. **密码强度校验**：README 提到了密码强度校验，但 AuthServiceImpl.register() 和 ReaderServiceImpl.registerReader() 中没有任何密码强度检查逻辑（如长度、大小写、特殊字符）。
2. **管理员手动解锁**：AccountLockService.unlockAccount() 已实现，但没有对应的 Controller 端点暴露此功能。
3. **账户状态 LOCKED 处理不一致**：`AuthServiceImpl.login()` 中检查 `accountLockService.isLocked()`（Redis 级别），但 `User.STATUS.LOCKED` 状态只在 `buildUserInfo()` 中映射了状态值 `2`，没有在任何业务逻辑中写入 DB 的 `status` 字段。
4. **验证码功能不完整**：`validateCaptcha()` 已实现，但系统中没有看到验证码生成端点（CaptchaController 存在但未读取），前端无法获取验证码。

### 断裂链条
- **AuthService.changePassword() 未在 AuthController 暴露**：该方法虽在 AuthService 接口/实现中完成，但 AuthController 无对应端点，而是通过 ReaderController.changePassword() 间接提供。这属于设计冗余而非断裂。

---

## 2. Book 图书模块

### 调用链
```
BookController → BookService → BookServiceImpl → BookMapper → book
```

### Controller 覆盖度
| 端点 | 状态 | 备注 |
|------|------|------|
| GET /books | ✅ 完整 | 分页查询，支持 keyword/categoryId |
| GET /books/hot | ✅ 完整 | 热门图书 |
| GET /books/{id} | ✅ 完整 | 图书详情 |
| POST /books | ✅ 完整 | 新增（含 ISBN 校验） |
| PUT /books/{id} | ✅ 完整 | 更新 |
| DELETE /books/{id} | ✅ 完整 | 删除 |
| GET /books/check-isbn | ✅ 完整 | ISBN 去重检查 |

### 功能遗漏
1. **缺少"新书推荐"端点**：BookMapper.xml 中已实现 `selectNewBooks` SQL，`BookService` 接口/实现及 `BookController` 均**未暴露**此功能。虽然有冗余的 `StatisticsController.getHotBooks` 但"新书推荐"完全没有入口。
2. **BookCategory CRUD 完全缺失**：`book_category` 表有 Mapper 和 Entity，但**没有** Controller、Service 或 API 用于 CRUD 管理图书分类。Admin 无法添加/编辑/删除分类。
3. **分类列表查询缺失**：前端需要获取所有分类以下拉选择，但系统没有提供 `GET /categories` 端点。

### 断裂链条
- **BookServiceImpl.createBook() 使用了不存在的字段**：`book.setLocation(...)` 但 Book 实体中 `location` 标记为 `@TableField(exist = false)`，存储时会静默忽略。
- **BookServiceImpl.createBook() 调用了错误的字段名**：`book.setTotalCount()` 和 `book.setAvailableCount()` 但实体映射问题见[第 11 章](#11-跨模块的实体-schema-字段映射断裂问题)。

---

## 3. Borrow 借阅模块

### 调用链
```
BorrowController → BorrowService → BorrowServiceImpl → BorrowRecordMapper → borrow_record
                                                      → BookMapper → book
                                                      → UserMapper → sys_user
```

### Controller 覆盖度
| 端点 | 状态 | 备注 |
|------|------|------|
| POST /borrows | ✅ 完整 | 借书（含信用分、逾期、上限检查） |
| POST /borrows/{id}/return | ✅ 完整 | 还书（含逾期计算、寒假排除） |
| POST /borrows/{id}/renew | ✅ 完整 | 续借（含次数限制） |
| GET /borrows/my | ✅ 完整 | 我的借阅列表 |
| GET /borrows | ✅ 完整 | 全部借阅（管理员） |
| GET /borrows/{id} | ✅ 完整 | 借阅详情（含归属检查） |
| GET /borrows/check-overdue | ✅ 完整 | 逾期检查 |

### 关键 BUG - Mapper 状态值不一致

这是整个系统中**最严重的断裂问题**：

| 位置 | 使用的状态值 | 说明 |
|------|-------------|------|
| Constants.BorrowStatus.BORROWING | `"BORROWING"` | 常量定义 |
| BorrowServiceImpl | `"BORROWING"` | 业务层全部使用 BORROWING |
| BorrowRecordMapper.selectCurrentBorrows | `'BORROWED'` | ❌ **使用了 BORROWED** |
| BorrowRecordMapper.selectOverdueRecords | `'BORROWED'` | ❌ **使用了 BORROWED** |
| BorrowRecordMapper.countCurrentBorrows | `'BORROWED'` | ❌ **使用了 BORROWED** |
| BorrowRecordMapper XML selectOverdueRecords | `'BORROWING'` | ✅ XML 使用 BORROWING |

**后果**：Java 注解中的 `@Select("...status = 'BORROWED'...")` 查询条件与其他代码用的 `'BORROWING'` 不一致，这些 Mapper 方法会在运行时返回**零条记录**，导致：
- `selectCurrentBorrows` 永远返回空列表
- `selectOverdueRecords` 永远返回空列表（仅注解版本，XML 版本正常）
- `countCurrentBorrows` 永远返回 0

### 其他问题
1. **BorrowRecordMapper SQL 使用了错误的列名**：`ORDER BY created_at DESC` 但 DB schema 中的列名是 `create_time`。这会直接导致 SQL 运行时异常。
2. **BorrowRecord entity 中 bookTitle/bookIsbn/isbn/username 均标记为 @TableField(exist = false)**：但这些字段的值在 BorrowServiceImpl.createBorrowRecord() 中被设置。由于这些字段不存储到 DB，查询返回时这些字段为空，除非有专门的关联查询。
3. **没有管理员筛选特定用户的借阅记录**：只支持全部查询和个人查询，缺少 `GET /borrows?userId={id}` 查询指定用户的借阅。

---

## 4. Seat 座位模块

### 调用链
```
SeatController → SeatService → SeatServiceImpl → SeatReservationMapper → seat_reservation
                           → SeatReservationService → SeatReservationServiceImpl → ReadingRoomMapper → reading_room
                                                                                        → SeatMapper → seat
```

### Controller 覆盖度
| 端点 | 状态 | 备注 |
|------|------|------|
| GET /seats | ✅ 完整 | 座位列表（含可用性） |
| POST /seats/reserve | ✅ 完整 | 预约（含冲突检查、每日限制） |
| POST /seats/cancel/{id} | ✅ 完整 | 取消（含 2 小时限制） |
| POST /seats/checkin/{id} | ✅ 完整 | 签到（含 15 分钟提前/30 分钟迟到） |
| POST /seats/checkout/{id} | ✅ 完整 | 签退 |
| GET /seats/my | ✅ 完整 | 我的预约 |
| GET /seats/check-availability | ✅ 完整 | 可用性检查 |

### 功能遗漏
1. **阅读室(ReadingRoom) CRUD 完全缺失**：README 声称支持"阅览室管理"，但只有 `getReadingRooms()` 查询方法，没有创建、更新、删除阅览室的 API。Admin 无法管理阅览室。
2. **座位(Seat) CRUD 完全缺失**：没有添加、删除、修改座位的 API。Admin 无法配置座位布局。
3. **无"可视化选座"支持数据**：Seat 实体有 rowNum/colNum 字段（标记为 DB 列），但 schema.sql 中 seat 表没有这两个列。可视化选座缺少行列坐标数据。
4. **缺少 `GET /seats/rooms` 端点**：虽然 SeatService 中有 getReadingRooms() 方法，但 Controller 没有暴露阅览室列表查询接口。

### 断裂链条
- **SeatReservation entity 字段与实际存储不符**：
  - `seatNumber` 存为 `@TableField(exist = false)`（非 DB 列），但 `countConflictingReservations` 等查询通过 `JOIN seat ON sr.seat_id = s.id` 动态获取
  - `area` 标记为 `@TableField(exist = false)`（非 DB 列），`createReservation()` 设置了 `area` 但不会被持久化
  - `source` 标记为 `@TableField(exist = false)`（非 DB 列），也不会被持久化
  - `cancelReason` 标记为 `@TableField(exist = false)`（非 DB 列）
- **SeatReservation 的 roomId/seatId 字段**：DB schema 中有 `room_id` 和 `seat_id` 列，但 `createReservation()` **从未设置** roomId 和 seatId，只有 seatNumber。这导致 seat_id 和 room_id 始终为 NULL。
- **SeatRedisCache 类存在但未被使用**：`SeatRedisCache` 已实现（get/update/evict 座位状态），但没有任何 SeatService 或 SeatReservationService 调用它。

---

## 5. Credit 信用模块

### 调用链
```
CreditController → CreditService → CreditServiceImpl → CreditLogMapper → credit_log
                                                       → UserMapper → sys_user
```

### Controller 覆盖度
| 端点 | 状态 | 备注 |
|------|------|------|
| GET /credits | ✅ 完整 | 我的积分 |
| GET /credits/user/{id} | ✅ 完整 | 指定用户积分（管理员） |
| GET /credits/logs | ✅ 完整 | 我的积分日志 |
| GET /credits/logs/user/{id} | ✅ 完整 | 指定用户日志（管理员） |

### 功能遗漏
1. **积分等级系统缺失**：README 明确说"等级：铜牌/银牌/金牌/白金"。Constants.Credit 中含阈值常量（BRONZE=60, SILVER=120, GOLD=180, PLATINUM=240），但**没有任何 API 或 Service 方法返回用户等级**。前端无法获取当前等级信息。
2. **CreditLog 实体到 DTO 转换字段不一致**：
   - CreditLog entity 字段: `creditChange`, `creditBalance`, `changeType`, `remark`
   - CreditLogResponse DTO 字段: `changeValue`, `balance`, `type`, `description`
   - CreditServiceImpl.convertToResponse() 调用 `log.getChangeValue()`, `log.getBalance()`, `log.getType()`, `log.getDescription()`
   - CreditLog entity 中有兼容方法 (`getChangeValue()`, `getBalance()`, `getType()`, `getDescription()`) 做桥接
   - 但 DTO builder 直接用的 `changeValue` 等字段名，这些值来自 entity 的桥接方法 **✅ 功能正常，但代码不够直观**

### 积分事件触发检查
| 事件 | 触发位置 | 状态 |
|------|---------|------|
| 借书+5 | BorrowServiceImpl.borrowBook() | ✅ |
| 按时还+1 | BorrowServiceImpl.returnBook() | ✅ |
| 提前还+2 | BorrowServiceImpl.returnBook() | ✅ |
| 逾期每天-5 | BorrowServiceImpl.returnBook() | ✅ |
| 签到+1 | SeatServiceImpl.checkIn() | ✅ |
| 志愿服务审核通过+10/小时 | VolunteerServiceImpl.reviewVolunteer() | ✅ |
| 预约未签到-2 | ScheduledTaskServiceImpl.checkExpiredReservations() | ✅ |
| 损坏-50 | CompensationServiceImpl.processCreditPayment() | ✅ |
| 丢失-100 | 未触发 | ❌ **丢失扣分从未被调用** |

---

## 6. Compensation 赔偿模块

### 调用链
```
CompensationController → CompensationService → CompensationServiceImpl → CompensationMapper → compensation_order
```

### Controller 覆盖度
| 端点 | 状态 | 备注 |
|------|------|------|
| POST /compensations | ✅ 完整 | 创建赔偿单（管理员） |
| GET /compensations | ✅ 完整 | 分页查询 |
| GET /compensations/{id} | ✅ 完整 | 详情 |
| POST /compensations/{id}/pay/cash | ✅ 完整 | 现金赔付 |
| POST /compensations/{id}/pay/credit | ✅ 完整 | 积分抵扣 |
| POST /compensations/{id}/pay/volunteer | ✅ 完整 | 志愿服务抵扣 |
| POST /compensations/{id}/cancel | ✅ 完整 | 取消 |

### 问题
1. **丢失扣分从未被触发**：Constants.Credit.LOST_PENALTY (= -100) 和 DAMAGE_PENALTY (= -50) 定义了丢失和损坏的扣分值，但 `CompensationServiceImpl` 的 `processCreditPayment()` 硬编码了 `"DAMAGE"` 作为 changeType，没有根据 `compensation.getCompType()` 动态选择 LOST/DAMAGE 的扣分值。
2. **赔偿后未同步更新图书状态**：赔付处理后未将图书标记为"已丢失/已报废"等状态。
3. **ErrorCode 使用不准确**：`getCompensationById()` 和 `getValidCompensation()` 中使用 `ErrorCode.INTERNAL_ERROR` 来表示"赔偿记录不存在"，应使用专门的错误码。

---

## 7. Volunteer 志愿服务模块

### 调用链
```
VolunteerController → VolunteerService → VolunteerServiceImpl → VolunteerServiceMapper → volunteer_service
```

### Controller 覆盖度
| 端点 | 状态 | 备注 |
|------|------|------|
| GET /volunteers | ✅ 完整 | 分页查询 |
| GET /volunteers/my | ✅ 完整 | 我的服务记录 |
| GET /volunteers/{id} | ✅ 完整 | 详情 |
| POST /volunteers | ✅ 完整 | 申请 |
| PUT /volunteers/{id} | ✅ 完整 | 更新 |
| POST /volunteers/{id}/cancel | ✅ 完整 | 取消 |
| POST /volunteers/{id}/review | ✅ 完整 | 审核 |
| GET /volunteers/pending | ✅ 完整 | 待审核列表 |
| DELETE /volunteers/{id} | ✅ 完整 | 删除 |
| GET /volunteers/stats | ✅ 完整 | 个人统计 |

### 问题
无重大断裂，实现完整。

---

## 8. Announcement 公告模块

### 调用链
```
AnnouncementController → AnnouncementService → AnnouncementServiceImpl → AnnouncementMapper → announcement
```

### Controller 覆盖度
| 端点 | 状态 | 备注 |
|------|------|------|
| GET /announcements | ✅ 完整 | 分页查询 |
| GET /announcements/{id} | ✅ 完整 | 详情 |
| GET /announcements/latest | ✅ 完整 | 最新公告 |
| POST /announcements | ✅ 完整 | 新增 |
| PUT /announcements/{id} | ✅ 完整 | 更新 |
| POST /announcements/{id}/publish | ✅ 完整 | 发布 |
| DELETE /announcements/{id} | ✅ 完整 | 删除 |

### 问题
- Announcement entity 使用了 `created_at`/`updated_at` 列名（通过 `@TableField(value = "created_at")`），但 schema.sql 中使用的是 `create_time`/`update_time`。这会导致**插入/查询失败**。

---

## 9. Reader 读者管理模块

### 调用链
```
ReaderController → ReaderService → ReaderServiceImpl → UserMapper → sys_user
```

### Controller 覆盖度
| 端点 | 状态 | 备注 |
|------|------|------|
| GET /readers | ✅ 完整 | 分页列表 |
| GET /readers/{id} | ✅ 完整 | 详情 |
| GET /readers/me | ✅ 完整 | 当前用户信息 |
| POST /readers | ✅ 完整 | 注册读者 |
| PUT /readers/{id} | ✅ 完整 | 更新信息 |
| POST /readers/{id}/password | ✅ 完整 | 修改密码 |
| DELETE /readers/{id} | ✅ 完整 | 删除 |
| POST /readers/{id}/reset-password | ✅ 完整 | 重置密码 |
| POST /readers/{id}/status | ✅ 完整 | 启用/禁用 |

### 问题
无重大断裂。

---

## 10. Statistics 统计模块

### 调用链
```
StatisticsController → StatisticsService → StatisticsServiceImpl → 所有 Mapper + JdbcTemplate
```

### Controller 覆盖度
| 端点 | 状态 | 备注 |
|------|------|------|
| GET /statistics/overview | ✅ 完整 |
| GET /statistics/borrows | ✅ 完整 |
| GET /statistics/books | ✅ 完整 |
| GET /statistics/readers | ✅ 完整 |
| GET /statistics/seats | ✅ 完整 |
| GET /statistics/borrow-trend | ✅ 完整 |
| GET /statistics/hot-books | ✅ 完整 | 与 /books/hot 冗余 |
| GET /statistics/category-distribution | ✅ 完整 |
| GET /statistics/seat-heatmap | ✅ 完整 |
| GET /statistics/monthly | ✅ 完整 |

### 问题
1. **StatisticsDaily 数据从未被用户端查询**：定时任务 `aggregateDailyStats()` 每日生成统计汇总写入 `statistics_daily` 表，但 Controller 中没有任何端点提供日统计数据的查询。

---

## 11. 跨模块的实体-Schema 字段映射断裂问题

### 11.1 Book 实体 - `totalCount` vs `total_quantity`

| 层面 | 字段名 | 值 |
|------|--------|---|
| schema.sql | `total_quantity INT DEFAULT 1` | DB 中有该列 |
| Book.java | `private Integer totalCount` | ⚠️ 无 @TableField 注解，按规则映射为 `total_count` |
| DB 实际列 | - | `total_count` **不存在**，应为 `total_quantity` |

**后果**：`BookServiceImpl.createBook()` 中 `book.setTotalCount(request.getTotalCount())` 写入 `total_count`（DB 无此列），会抛出 **SQL 异常**。

### 11.2 Announcement 实体 - `created_at` vs `create_time`

| 层面 | 字段 |
|------|------|
| schema.sql | `create_time DATETIME`, `update_time DATETIME` |
| Announcement.java | `@TableField(value = "created_at")` → 写入 `created_at` |
| DB 实际列 | `create_time`, `update_time` |

**后果**：公告 CRUD 操作的 INSERT/UPDATE 会因列名不匹配失败。

### 11.3 BorrowRecord Mapper - `created_at` vs `create_time`

BorrowRecordMapper.java 中的多个 `@Select` 语句使用了 `ORDER BY created_at DESC` 或 `DATE(created_at)`，但 schema 中的列名为 `create_time`。

### 11.4 SeatReservation 缺少 seat_id 和 room_id 赋值

`schema.sql` 中 `seat_reservation` 表包含 `room_id` 和 `seat_id` 列，但 `SeatServiceImpl.createReservation()` 只设置了 `seatNumber`（非 DB 列）**从未设置** `roomId` 和 `seatId`。

### 11.5 Seat 实体的额外字段

Seat.java 包含 `rowNum` 和 `colNum` 字段（映射为 `row_num`, `col_num`），但 schema.sql 的 `seat` 表**没有**这两个列。

### 11.6 VolunteerService 实体字段映射

VolunteerService.java 中 `reviewerId` 映射为 `@TableField("approved_by")`，`reviewTime` 映射为 `@TableField("approved_at")`，`reviewRemark` 映射为 `@TableField("remark")`，但 schema.sql 中的对应列名分别是 `reviewer_id`, `review_time`, `review_remark`。**列名全部不匹配**。

---

## 12. README 功能覆盖检查

| README 声称的功能 | 实现状态 | 备注 |
|------------------|---------|------|
| **JWT 双 Token** | ✅ | Access 2h + Refresh 7d |
| **账户锁定（5次失败/15分钟）** | ✅ | Redis 分布式锁定 |
| **密码强度校验** | ❌ **缺失** | 无任何密码强度检查代码 |
| **图书 CRUD** | ✅ | |
| **ISBN 去重校验** | ✅ | |
| **分类管理** | ⚠️ **半实现** | 有预设分类，无管理 API |
| **热门/新书推荐** | ⚠️ **半实现** | 热门有 API，新书无 API |
| **借书/还书/续借** | ✅ | |
| **借阅限制（5本/人/30天）** | ✅ | |
| **逾期罚款** | ✅ | 含寒暑假排除 |
| **阅览室管理** | ❌ **缺失** | 仅有查询，无 CRUD |
| **可视化选座** | ⚠️ | 后端列出座位，但缺少行列坐标 |
| **签到/签退** | ✅ | |
| **违约检测** | ✅ | 3次违约封禁72小时 |
| **积分制度（初始100分）** | ✅ | |
| **等级：铜牌/银牌/金牌/白金** | ❌ **缺失** | 有阈值常量，但无等级查询 API |
| **积分日志** | ✅ | |
| **公告管理** | ⚠️ | 功能完整，但列名映射错误导致不可用 |
| **志愿服务** | ✅ | |
| **统计分析** | ✅ | |

---

## 13. 汇总：缺失功能和断裂链条

### 严重断裂（会导致运行时错误）

| # | 位置 | 问题 | 严重程度 |
|---|------|------|---------|
| 1 | Book.totalCount → `total_quantity` | 实体字段名与 schema 列名不匹配，INSERT/UPDATE 会 SQL 异常 | **CRITICAL** |
| 2 | Announcement.created_at → `create_time` | 实体使用 `created_at` 但 schema 是 `create_time`，CRUD 全挂 | **CRITICAL** |
| 3 | VolunteerService 字段映射错误 | `reviewer_id`→`approved_by`, `review_time`→`approved_at`, `review_remark`→`remark` 三处全部对不上 | **CRITICAL** |
| 4 | BorrowRecordMapper 状态常量不一致 | `selectCurrentBorrows` 等查 `'BORROWED'` 但 DB 存的是 `'BORROWING'` | **CRITICAL** |
| 5 | BorrowRecordMapper `created_at` 列名 | 多个 SQL 用 `created_at` 但 schema 列是 `create_time` | **HIGH** |
| 6 | SeatReservation 未设置 seat_id/room_id | `createReservation()` 未填充 seatId 和 roomId，虽然查询通过 JOIN 走 seatNumber | **HIGH** |

### 功能缺失（README 承诺但未实现）

| # | 缺失功能 | 影响 |
|---|---------|------|
| 1 | **密码强度校验** | 注册和修改密码无安全性校验 |
| 2 | **新书推荐 API** | BookMapper.xml 有 SQL 但无 Controller 暴露 |
| 3 | **图书分类管理 CRUD** | 无法添加/编辑/删除分类 |
| 4 | **阅览室管理 CRUD** | 只能查询预设的 3 个阅览室 |
| 5 | **座位管理 CRUD** | 无法添加/删除/配置座位 |
| 6 | **积分等级查询** | 前端无法获取用户的等级（铜牌/银牌/金牌/白金） |
| 7 | **阅读室列表 API** | SeatService 有方法但 Controller 无暴露 |
| 8 | **管理员手动解锁账号 API** | AccountLockService 有实现但 Controller 无暴露 |
| 9 | **验证码获取 API** | 验证码校验已在 login 中，但没有生成验证码的接口 |
| 10 | **StatistricsDaily 查询 API** | 定时任务写入数据但无读取接口 |

### 其他问题

| # | 问题 | 说明 |
|---|------|------|
| 1 | Compensation 丢失扣分未处理 | processCreditPayment 硬编码 `"DAMAGE"` 而非根据 compType 动态选择 |
| 2 | SeatRedisCache 未使用 | 类已实现但无任何地方调用 |
| 3 | `isLockedByUsername()` 返回 false | AccountLockServiceImpl 方法未真正实现 |
| 4 | CreditLog 实体 vs DTO 字段重命名 | 兼容方法工作，但增加维护复杂度 |

---

## 总结

**业务流完整性排名**（最完整到最不完整）:
1. ✅ **Auth** - 完整，仅缺密码强度
2. ✅ **Volunteer** - 几乎完整
3. ✅ **Statistics** - 几乎完整，缺 DailyStats 查询
4. ⚠️ **Borrow** - 逻辑完整，但 Mapper SQL 有严重列名/状态值错误
5. ⚠️ **Book** - CRUD 完整，但缺少新书推荐和分类管理
6. ⚠️ **Reader** - 完整
7. ⚠️ **Credit** - 核心功能完整，缺等级查询
8. ⚠️ **Compensation** - 流程完整，丢失扣分不按类型区分
9. ❌ **Seat** - 预约流程完整，但缺乏阅览室/座位管理 CRUD
10. ❌ **Announcement** - 代码完整，但列名映射全错，实际上不可用

**最需要优先修复的 3 个问题**:
1. Book entity `totalCount` → DB `total_quantity` 列名不匹配
2. Announcement entity `created_at`/`updated_at` → DB `create_time`/`update_time` 列名不匹配
3. BorrowRecordMapper 状态值 `'BORROWED'` → `'BORROWING'` 不匹配

# 文档一致性审查报告

**审查时间**: 2026-04-24  
**审查范围**: API.md、ARCHITECTURE.md、README.md  
**审查依据**: schema.sql、Controller源码、DTO定义

---

## 一、API.md 一致性检查

### 1.1 API路径不一致

| API文档路径 | 文档描述 | 实际接口路径 | HTTP方法 | 一致性 |
|-------------|----------|--------------|----------|--------|
| `/api/books/categories` | 获取图书分类 | ❌ **不存在** | - | ❌ 缺失 |
| `/api/books/import` | 批量导入图书 | ❌ **不存在** | - | ❌ 缺失 |
| `/api/books/export` | 导出图书 | ❌ **不存在** | - | ❌ 缺失 |
| `/api/books/check-isbn/{isbn}` | ISBN查重 | `GET /books/check-isbn?isbn=xxx` | GET | ⚠️ 路径参数不一致 |
| `/api/seat-reservations` | 预约座位 | `POST /seats/reserve` | POST | ❌ 路径完全不一致 |
| `/api/seat-reservations/{id}` | 取消预约 | `POST /seats/cancel/{reservationId}` | POST | ❌ 路径+方法不一致 |
| `/api/seats/{id}/availability` | 查询可用时间 | `GET /seats/check-availability` | GET | ⚠️ 路径不一致 |
| ❌ **缺失** | 座位签到 | `POST /seats/checkin/{reservationId}` | POST | ❌ 文档缺失 |
| ❌ **缺失** | 座位签退 | `POST /seats/checkout/{reservationId}` | POST | ❌ 文档缺失 |
| ❌ **缺失** | 我的预约 | `GET /seats/my` | GET | ❌ 文档缺失 |
| `/api/volunteer-activities/{activityId}/review/{volunteerId}` | 审核志愿者 | `POST /volunteers/{id}/review` | POST | ❌ 路径不一致 |
| `/api/announcements/pinned` | 热门公告 | ❌ **不存在** | - | ❌ 缺失 |

### 1.2 查询参数不一致

| 接口 | 文档参数 | 实际参数 | 一致性 |
|------|----------|----------|--------|
| `GET /books` | `title, author, category, isbn, status` | `keyword, categoryId` | ❌ 完全不一致 |
| `POST /books` | `isbn, title, author, publisher, publishDate, category, description, price, totalQuantity, stock` | `isbn, title, author, publisher, publishDate, categoryId, description, price, totalCount` (无stock字段) | ⚠️ 部分不一致 |
| `POST /borrow-records` | `readerId, bookId, dueDate` | `bookId, borrowDays` (无readerId/dueDate) | ❌ 完全不一致 |
| `POST /seat-reservations` | `seatId, date, startTime, endTime` | `seatId, seatNumber, roomId, reservationDate, startTime, endTime, area` | ⚠️ 参数不完整 |
| `POST /volunteers` | `serviceDate, startTime, endTime, serviceHours, serviceType, description` | `serviceDate, startTime, endTime, serviceHours, serviceType, description` | ✅ 一致 |
| `GET /credits/logs` | 无分页参数说明 | `current, size` | ⚠️ 文档缺失 |

### 1.3 权限标注不一致

| 接口 | 文档权限 | 实际权限 | 一致性 |
|------|----------|----------|--------|
| `POST /books` | 无 | `ADMIN/LIBRARIAN` | ⚠️ 文档缺失 |
| `POST /volunteers` | 需登录 | `ADMIN/LIBRARIAN/READER/VOLUNTEER` | ⚠️ 权限扩展 |
| `GET /volunteers/stats` | 无 | 需登录 | ❌ 文档缺失 |
| `GET /credits` | READER | READER | ✅ 一致 |

---

## 二、ARCHITECTURE.md vs schema.sql 字段一致性

### 2.1 表结构不一致

| 表名 | 文档字段 | 数据库字段 | 备注 |
|------|----------|------------|------|
| **sys_user** | 无以下字段 | `card_number` | ❌ 文档缺失 |
| | 无以下字段 | `credit_score` | ❌ 文档缺失 |
| | 无以下字段 | `borrow_count` | ❌ 文档缺失 |
| | 无以下字段 | `deleted` | ❌ 文档缺失 |
| **book** | `total_copies` | `total_quantity` | ❌ 字段名错误 |
| | `category` | `category_id` | ❌ 字段名错误 |
| | 无以下字段 | `cover_image` | ❌ 文档缺失 |
| | 无以下字段 | `borrow_count` | ❌ 文档缺失 |
| | 无以下字段 | `deleted` | ❌ 文档缺失 |
| **borrow_record** | `reader_id` | `user_id` | ❌ 字段名错误 |
| | 无以下字段 | `fine_amount` | ❌ 文档缺失 |
| | 无以下字段 | `renew_count` | ❌ 文档缺失 |
| | `status` (INT) | `status` (VARCHAR: BORROWING/RETURNED/OVERDUE) | ⚠️ 类型描述错误 |
| | 无以下字段 | `deleted` | ❌ 文档缺失 |
| **seat_reservation** | 无以下字段 | `room_id` | ❌ 文档缺失 |
| | 无以下字段 | `check_in_time` | ❌ 文档缺失 |
| | 无以下字段 | `check_out_time` | ❌ 文档缺失 |
| | 无以下字段 | `violation_count` | ❌ 文档缺失 |
| | 无以下字段 | `deleted` | ❌ 文档缺失 |

### 2.2 文档中缺失的表

| 表名 | 说明 | 状态 |
|------|------|------|
| `book_category` | 图书分类表 | ❌ 完全缺失 |
| `reading_room` | 阅览室表 | ❌ 完全缺失 |
| `credit_log` | 积分日志表 | ❌ 完全缺失 |
| `sys_operation_log` | 操作日志表 | ❌ 完全缺失 |

### 2.3 索引定义不一致

| 文档索引 | 实际索引 | 一致性 |
|----------|----------|--------|
| `idx_book_category_id` (在book表) | `idx_category_id` | ⚠️ 名称不一致 |
| 无 `idx_card_number` (在sys_user) | `idx_card_number` 存在 | ❌ 文档缺失 |

---

## 三、README.md 准确性检查

| 检查项 | 文档内容 | 实际情况 | 一致性 |
|--------|----------|----------|--------|
| 技术栈版本 | MyBatis-Plus 3.5.6 | 实际使用 3.5.5 | ⚠️ 版本不一致 |
| 借阅限制 | 5本/人/30天 | 实际无固定天数限制 | ⚠️ 描述不准确 |
| 信用积分 | 初始100分,等级制度 | 存在初始100分 | ✅ 一致 |
| Vue版本 | 3.4 | package.json显示3.4+ | ✅ 一致 |

---

## 四、跨文档一致性检查

### 4.1 模块命名不一致

| 模块 | API.md | ARCHITECTURE.md | 实际Controller |
|------|--------|-----------------|----------------|
| 志愿服务 | `volunteer-activities` | `VolunteerController` | `volunteers` |
| 积分管理 | `points` | 提到但未详细说明 | `credits` |
| 座位预约 | `seat-reservations` | `SeatController` | `seats` |

### 4.2 赔偿管理接口

| 检查项 | API.md | 实际情况 |
|--------|--------|----------|
| 赔偿金额规则 | 图书损坏×1.5, 丢失×2.0, 逾期每天0.5元 | 需验证Service实现 |

---

## 五、文档评分 (1-10)

| 文档 | 评分 | 主要问题 |
|------|------|----------|
| **API.md** | **5/10** | API路径与实际接口不一致率约40%，多处关键接口缺失或路径错误 |
| **ARCHITECTURE.md** | **4/10** | 数据库表结构描述不完整，字段名错误率高，约60%字段描述与schema不一致 |
| **跨文档一致性** | **6/10** | 模块命名存在不一致，但核心功能描述相对准确 |
| **README.md** | **7/10** | 整体准确，技术栈版本有微小差异 |

---

## 六、总体评价

**综合评分: 5.5/10 (B- 需重大修复)**

### 主要问题汇总:
1. **API文档严重滞后**: 约40%的API路径与实际Controller实现不一致
2. **数据库文档过时**: 大量schema.sql中的字段未在ARCHITECTURE.md中记录
3. **字段命名错误**: 多处使用旧版本字段名(如`total_copies` vs `total_quantity`)
4. **缺失关键接口**: 座位签到/签退、借阅等重要接口在API.md中缺失

---

## 七、必须修复的不一致

### 🔴 P0 - 关键错误 (必须立即修复)

1. **API路径与Controller不一致**
   - `/api/seat-reservations` → 应改为 `/api/seats/reserve`
   - `/api/books/categories` → 需确认是否存在，如不存在需从文档删除
   - `/api/volunteer-activities` → 应改为 `/api/volunteers`

2. **ARCHITECTURE.md字段名错误**
   - `book.total_copies` → `book.total_quantity`
   - `book.category` → `book.category_id`
   - `borrow_record.reader_id` → `borrow_record.user_id`

3. **缺失表结构文档**
   - 补充 `book_category` 表
   - 补充 `reading_room` 表
   - 补充 `credit_log` 表

### 🟡 P1 - 重要问题 (应尽快修复)

4. **缺失接口文档**
   - `/api/seats/checkin/{id}`
   - `/api/seats/checkout/{id}`
   - `/api/seats/my`
   - `/api/credits/logs`
   - `/api/credits/logs/user/{userId}`

5. **参数描述不准确**
   - `GET /books` 查询参数需更新
   - `POST /borrow-records` 请求体需更新
   - `POST /seat-reservations` 请求体需补充完整字段

### 🟢 P2 - 建议优化

6. **README.md版本信息**
   - MyBatis-Plus版本从3.5.6更正为3.5.5

7. **跨文档命名统一**
   - 统一志愿服务模块命名为 `volunteers`
   - 统一积分模块命名为 `credits` 或 `points`

---

## 八、修复优先级建议

```
第一优先级 (立即修复):
├── 1. 修正ARCHITECTURE.md中的表结构
│   ├── 修正book表: total_copies→total_quantity, category→category_id
│   ├── 修正borrow_record表: reader_id→user_id
│   └── 补充缺失表: book_category, reading_room, credit_log
│
└── 2. 修正API.md中的错误路径
    ├── /seat-reservations → /seats/reserve, /seats/cancel/{id}
    └── /volunteer-activities → /volunteers

第二优先级 (本周修复):
├── 3. 补充缺失的API接口
│   ├── 座位签到/签退
│   ├── 积分日志查询
│   └── 我的预约列表
│
└── 4. 更新API参数描述
    ├── GET /books 查询参数
    └── POST /borrow-records 请求体

第三优先级 (后续优化):
└── 5. README.md版本信息更新
└── 6. 跨文档命名统一
```

---

*本报告由 docs-reviewer 自动生成*
*审查时间: 2026-04-24*

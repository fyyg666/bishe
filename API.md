# API接口文档

本文档详细描述图书馆管理系统的所有RESTful API接口。

## 基础信息

- **基础URL**：http://localhost:8080/api
- **Content-Type**：application/json
- **字符编码**：UTF-8

## 通用响应格式

### 成功响应

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    // 业务数据
  },
  "timestamp": 1700000000000
}
```

### 错误响应

```json
{
  "code": 400,
  "message": "请求参数错误",
  "data": null,
  "timestamp": 1700000000000
}
```

## 响应码说明

| 响应码 | 说明 |
|--------|------|
| 200 | 操作成功 |
| 400 | 请求参数错误 |
| 401 | 未授权（Token无效或过期） |
| 403 | 权限不足 |
| 404 | 资源不存在 |
| 409 | 资源冲突（如ISBN重复） |
| 429 | 请求过于频繁 |
| 500 | 服务器内部错误 |

## API文档工具

### Swagger UI

在线API文档界面，适合快速测试接口。

**访问地址**：`http://localhost:8080/api/swagger-ui.html`

**功能特性**：
- 可视化API文档界面
- 在线接口测试
- 自动生成请求示例
- 支持JWT Token认证

**使用步骤**：
1. 打开Swagger UI页面
2. 点击右上角 **Authorize** 按钮
3. 输入JWT AccessToken
4. 展开需要测试的接口
5. 点击 **Try it out** 进行测试

### Knife4j（推荐）

增强版API文档，提供更多高级功能。

**访问地址**：`http://localhost:8080/api/doc.html`

**功能特性**：
- 文档分组管理
- 接口搜索
- 接口版本管理
- 离线文档导出
- 更多调试功能

**使用步骤**：
1. 打开Knife4j页面
2. 完成认证授权（同Swagger）
3. 选择分组查看不同模块接口
4. 使用左侧搜索框快速定位接口
5. 导出离线文档（菜单 → 文档 → 导出）

## 认证接口

### 1. 用户登录

**POST** `/api/auth/login`

**请求体**：
```json
{
  "username": "admin",
  "password": "password123"
}
```

**响应**：
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 7200
  }
}
```

### 2. 用户注册

**POST** `/api/auth/register`

**请求体**：
```json
{
  "username": "newuser",
  "password": "Password123!",
  "realName": "张三",
  "email": "user@example.com",
  "phone": "13800138000"
}
```

### 3. 刷新Token

**POST** `/api/auth/refresh`

**请求头**：`Authorization: Bearer {refreshToken}`

### 4. 退出登录

**POST** `/api/auth/logout`

**请求头**：`Authorization: Bearer {accessToken}`

### 5. 获取当前用户信息

**GET** `/api/auth/me`

**请求头**：`Authorization: Bearer {accessToken}`

## 图书管理接口

### 1. 分页查询图书

**GET** `/api/books`

**查询参数**：
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 页码，默认1 |
| size | int | 否 | 每页数量，默认10 |
| title | string | 否 | 书名（模糊查询） |
| author | string | 否 | 作者（模糊查询） |
| category | string | 否 | 分类 |
| isbn | string | 否 | ISBN |
| status | int | 否 | 状态：0-下架，1-上架 |

### 2. 获取图书详情

**GET** `/api/books/{id}`

### 3. 添加图书

**POST** `/api/books`

**请求体**：
```json
{
  "isbn": "978-7-111-54742-0",
  "title": "深入理解Java虚拟机",
  "author": "周志明",
  "publisher": "机械工业出版社",
  "publishDate": "2023-01-01",
  "category": "技术",
  "description": "JVM经典书籍",
  "price": 119.00,
  "totalQuantity": 5,
  "stock": 5
}
```

### 4. 更新图书

**PUT** `/api/books/{id}`

### 5. 删除图书

**DELETE** `/api/books/{id}`

### 6. 获取图书分类

**GET** `/api/books/categories`

### 7. 批量导入图书

**POST** `/api/books/import`

**Content-Type**：multipart/form-data

**参数**：file - Excel文件

### 8. 导出图书

**GET** `/api/books/export`

**查询参数**：format - excel/csv

### 9. ISBN查重

**GET** `/api/books/check-isbn/{isbn}`

## 读者管理接口

### 1. 分页查询读者

**GET** `/api/readers`

### 2. 获取读者详情

**GET** `/api/readers/{id}`

### 3. 添加读者

**POST** `/api/readers`

### 4. 更新读者

**PUT** `/api/readers/{id}`

### 5. 删除读者

**DELETE** `/api/readers/{id}`

### 6. 获取读者借阅记录

**GET** `/api/readers/{id}/borrow-records`

## 借阅管理接口

### 1. 分页查询借阅记录

**GET** `/api/borrow-records`

### 2. 借书

**POST** `/api/borrow-records`

**请求体**：
```json
{
  "readerId": 1,
  "bookId": 1,
  "dueDate": "2024-01-15"
}
```

### 3. 还书

**PUT** `/api/borrow-records/{id}/return`

### 4. 续借

**PUT** `/api/borrow-records/{id}/renew`

### 5. 逾期记录

**GET** `/api/borrow-records/overdue`

### 6. 借阅统计

**GET** `/api/borrow-records/statistics`

### 7. 读者借阅统计

**GET** `/api/borrow-records/reader/{readerId}/statistics`

### 8. 图书借阅统计

**GET** `/api/borrow-records/book/{bookId}/statistics`

## 座位预约接口

### 1. 分页查询座位

**GET** `/api/seats`

### 2. 获取座位详情

**GET** `/api/seats/{id}`

### 3. 预约座位

**POST** `/api/seat-reservations`

**请求体**：
```json
{
  "seatId": 1,
  "date": "2024-01-15",
  "startTime": "09:00",
  "endTime": "12:00"
}
```

### 4. 取消预约

**DELETE** `/api/seat-reservations/{id}`

### 5. 查询预约记录

**GET** `/api/seat-reservations/my`

### 6. 查询座位可用时间

**GET** `/api/seats/{id}/availability`

## 积分管理接口

### 1. 查询积分

**GET** `/api/points/balance`

### 2. 积分记录

**GET** `/api/points/records`

### 3. 积分兑换

**POST** `/api/points/redeem`

**请求体**：
```json
{
  "rewardId": 1,
  "quantity": 1
}
```

### 4. 积分规则

**GET** `/api/points/rules`

### 5. 可兑换奖励

**GET** `/api/points/rewards`

### 6. 奖励兑换记录

**GET** `/api/points/redemption-history`

## 公告管理接口

### 1. 分页查询公告

**GET** `/api/announcements`

### 2. 获取公告详情

**GET** `/api/announcements/{id}`

### 3. 发布公告（需LIBRARIAN权限）

**POST** `/api/announcements`

### 4. 更新公告（需LIBRARIAN权限）

**PUT** `/api/announcements/{id}`

### 5. 删除公告（需LIBRARIAN权限）

**DELETE** `/api/announcements/{id}`

### 6. 获取最新公告

**GET** `/api/announcements/latest`

### 7. 获取热门公告

**GET** `/api/announcements/pinned`

## 志愿服务接口

### 1. 分页查询志愿活动

**GET** `/api/volunteer-activities`

### 2. 获取活动详情

**GET** `/api/volunteer-activities/{id}`

### 3. 报名参加活动

**POST** `/api/volunteer-activities/{id}/join`

### 4. 取消报名

**DELETE** `/api/volunteer-activities/{id}/leave`

### 5. 审核志愿者（需LIBRARIAN权限）

**PUT** `/api/volunteer-activities/{activityId}/review/{volunteerId}`

## 赔偿管理接口

### 1. 查询赔偿记录

**GET** `/api/compensations`

### 2. 创建赔偿记录

**POST** `/api/compensations`

### 3. 确认赔偿（需LIBRARIAN权限）

**PUT** `/api/compensations/{id}/confirm`

### 4. 赔偿金额计算规则

| 情况 | 赔偿金额 |
|------|----------|
| 图书损坏 | 图书原价 × 1.5 |
| 图书丢失 | 图书原价 × 2.0 |
| 逾期罚款 | 每天 0.5 元 |

## 统计分析接口

> **模块说明**：统计分析模块提供系统运营数据的全面查询能力，包括借阅、图书、读者、座位等多维度统计。
> 所有接口（除热门图书排行外）需要 **ADMIN** 或 **LIBRARIAN** 角色权限。

### 1. 综合统计概览

**GET** `/api/statistics/overview`

**权限**：ADMIN / LIBRARIAN

**说明**：获取系统综合统计概览数据，包含借阅、图书、读者、座位四个维度的核心指标汇总。

**响应示例**：
```json
{
  "code": 200,
  "data": {
    "borrowStatistics": {
      "totalBorrows": 1250,
      "activeBorrows": 85,
      "overdueBorrows": 12,
      "returnedToday": 23,
      "averageBorrowDays": 14.5
    },
    "bookStatistics": {
      "totalBooks": 5000,
      "totalQuantity": 15000,
      "stock": 12000,
      "borrowedCopies": 3000,
      "categories": 25
    },
    "readerStatistics": {
      "totalReaders": 800,
      "activeReaders": 350,
      "overdueReaders": 12,
      "averageCreditScore": 95.5
    },
    "seatStatistics": {
      "totalSeats": 200,
      "availableSeats": 150,
      "occupiedSeats": 50,
      "todayReservations": 80
    }
  }
}
```

### 2. 借阅统计数据

**GET** `/api/statistics/borrows`

**权限**：ADMIN / LIBRARIAN

**响应字段**：

| 字段 | 类型 | 说明 |
|------|------|------|
| totalBorrows | long | 累计借阅总数 |
| activeBorrows | long | 当前在借数量 |
| overdueBorrows | long | 逾期未还数量 |
| returnedToday | long | 今日归还数 |
| averageBorrowDays | double | 平均借阅天数 |

### 3. 图书统计数据

**GET** `/api/statistics/books`

**权限**：ADMIN / LIBRARIAN

**响应字段**：

| 字段 | 类型 | 说明 |
|------|------|------|
| totalBooks | long | 图书种类总数 |
| totalQuantity | long | 图书总册数 |
| stock | long | 可借册数 |
| borrowedCopies | long | 已借出册数 |
| categories | long | 分类数量 |

### 4. 读者统计数据

**GET** `/api/statistics/readers`

**权限**：ADMIN / LIBRARIAN

**响应字段**：

| 字段 | 类型 | 说明 |
|------|------|------|
| totalReaders | long | 读者总人数 |
| activeReaders | long | 活跃读者数（近30天有借阅） |
| overdueReaders | long | 有逾期记录的读者数 |
| averageCreditScore | double | 平均信用积分 |

### 5. 座位统计数据

**GET** `/api/statistics/seats`

**权限**：ADMIN / LIBRARIAN

**响应字段**：

| 字段 | 类型 | 说明 |
|------|------|------|
| totalSeats | long | 座位总数量 |
| availableSeats | long | 当前可用座位数 |
| occupiedSeats | long | 已占用座位数 |
| todayReservations | long | 今日签到入座数 |

### 6. 借阅趋势

**GET** `/api/statistics/borrow-trend`

**权限**：ADMIN / LIBRARIAN

**查询参数**：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| days | Integer | 否 | 30 | 统计天数范围（1-365）|

**响应示例**：
```json
{
  "code": 200,
  "data": [
    { "date": "2024-01-01", "borrows": 15, "returns": 8 },
    { "date": "2024-01-02", "borrows": 22, "returns": 18 }
  ]
}
```

**说明**：
- `borrows`：当日新增借阅数
- `returns`：当日归还图书数
- 数据按日期升序排列，包含从今天往前推N天的完整数据

### 7. 热门图书排行

**GET** `/api/statistics/hot-books`

**权限**：所有已登录用户

**查询参数**：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| limit | Integer | 否 | 10 | 返回数量限制（1-50）|

**响应示例**：
```json
{
  "code": 200,
  "data": [
    {
      "id": 1,
      "isbn": "978-7-111-54742-0",
      "title": "深入理解Java虚拟机",
      "author": "周志明",
      "publisher": "机械工业出版社",
      "borrowCount": 156,
      "totalCount": 5,
      "availableCount": 1
    }
  ]
}
```

**说明**：按借阅次数降序排列，返回最受欢迎的图书列表。

### 8. 图书分类分布

**GET** `/api/statistics/category-distribution`

**权限**：ADMIN / LIBRARIAN

**响应示例**：
```json
{
  "code": 200,
  "data": [
    { "categoryId": 1, "categoryName": "计算机", "count": 1200 },
    { "categoryId": 2, "categoryName": "文学", "count": 850 },
    { "categoryId": 3, "categoryName": "历史", "count": 500 }
  ]
}
```

**说明**：按分类统计图书数量，按数量降序排列。

### 9. 月度统计

**GET** `/api/statistics/monthly`

**权限**：ADMIN / LIBRARIAN

**查询参数**：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| months | Integer | 否 | 12 | 统计月份数（1-24）|

**响应示例**：
```json
{
  "code": 200,
  "data": [
    { "month": "2024-01", "borrows": 320, "returns": 280, "newReaders": 25 },
    { "month": "2024-02", "borrows": 295, "returns": 310, "newReaders": 18 }
  ]
}
```

**响应字段**：

| 字段 | 类型 | 说明 |
|------|------|------|
| month | string | 月份（格式: yyyy-MM）|
| borrows | long | 当月借阅数 |
| returns | long | 当月归还数 |
| newReaders | long | 当月新注册读者数 |

## 信用积分接口

> **模块说明**：信用积分系统管理用户的积分余额和变动日志。积分通过正常借阅、志愿服务等行为获得，逾期、损坏图书等行为会扣减积分。

### 1. 查询我的积分

**GET** `/api/credits`

**权限**：READER

**说明**：获取当前登录用户的信用积分余额。

**响应示例**：
```json
{ "code": 200, "data": 100 }
```

### 2. 查询指定用户积分（管理员）

**GET** `/api/credits/user/{userId}`

**权限**：ADMIN / LIBRARIAN

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| userId | Long | 目标用户ID |

### 3. 查询我的积分日志

**GET** `/api/credits/logs`

**权限**：READER

**查询参数**：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| current | Long | 否 | 1 | 当前页码 |
| size | Long | 否 | 10 | 每页条数 |

**响应示例**：
```json
{
  "code": 200,
  "data": {
    "records": [
      {
        "id": 1,
        "userId": 1,
        "scoreChange": 10,
        "reason": "按时归还图书",
        "createTime": "2024-01-15T10:30:00"
      }
    ],
    "total": 25,
    "current": 1,
    "size": 10
  }
}
```

### 4. 查询用户积分日志（管理员）

**GET** `/api/credits/logs/user/{userId}`

**权限**：ADMIN / LIBRARIAN

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| userId | Long | 目标用户ID |

**查询参数**：同上（current, size）

## 志愿服务接口

> **模块说明**：志愿者可申请并参与图书馆志愿服务活动，审核通过后自动增加信用积分（每服务1小时+5分，上限50分）。单次服务时长不超过12小时。

### 1. 分页查询志愿服务记录

**GET** `/api/volunteers`

**查询参数**：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| current | Long | 否 | 1 | 页码 |
| size | Long | 否 | 10 | 每页大小 |
| status | String | 否 | - | 状态筛选：PENDING/APPROVED/REJECTED/CANCELLED |

### 2. 查询我的志愿服务

**GET** `/api/volunteers/my`

**权限**：需登录

**查询参数**：current, size

### 3. 获取志愿服务详情

**GET** `/api/volunteers/{id}`

**路径参数**：id - 记录ID

### 4. 申请志愿服务

**POST** `/api/volunteers`

**请求体**：
```json
{
  "serviceDate": "2024-01-20",
  "startTime": "2024-01-20T09:00:00",
  "endTime": "2024-01-20T11:00:00",
  "serviceHours": 2.0,
  "serviceType": "图书整理",
  "description": "整理A区书架"
}
```

**说明**：`serviceHours` 可不填，系统将根据 startTime 和 endTime 自动计算。

### 5. 更新志愿服务记录

**PUT** `/api/volunteers/{id}`

**权限**：仅本人且状态为PENDING时可修改

**请求体**：同创建接口

### 6. 取消志愿服务申请

**POST** `/api/volunteers/{id}/cancel`

**权限**：仅本人且状态为PENDING时可取消

### 7. 审核志愿服务（管理员）

**POST** `/api/volunteers/{id}/review`

**权限**：ADMIN / LIBRARIAN

**查询参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| approved | Boolean | 是 | 是否通过审核 |
| remark | String | 否 | 审核备注 |

**业务规则**：
- 审核通过时，自动为用户增加信用积分（每服务1小时 +5分，上限50分）
- 审核通过时，`status` 变更为 APPROVED；拒绝时变更为 REJECTED

### 8. 查询待审核列表（管理员）

**GET** `/api/volunteers/pending`

**权限**：ADMIN / LIBRARIAN

### 9. 删除志愿服务记录（管理员）

**DELETE** `/api/volunteers/{id}`

**权限**：ADMIN / LIBRARIAN（逻辑删除）

### 10. 查询我的志愿统计

**GET** `/api/volunteers/stats`

**权限**：需登录

**响应示例**：
```json
{
  "code": 200,
  "data": {
    "totalRecords": 10,
    "totalHours": 25.5,
    "pendingCount": 2
  }
}
```

**响应字段**：

| 字段 | 类型 | 说明 |
|------|------|------|
| totalRecords | long | 已通过的志愿服务记录数 |
| totalHours | BigDecimal | 累计服务时长（小时）|
| pendingCount | long | 待审核的记录数 |

## 错误码详细说明

| 错误码 | HTTP状态码 | 说明 | 解决方案 |
|--------|------------|------|----------|
| AUTH_001 | 401 | 用户名或密码错误 | 检查登录信息 |
| AUTH_002 | 401 | Token已过期 | 刷新Token |
| AUTH_003 | 401 | Token无效 | 重新登录 |
| AUTH_004 | 403 | 权限不足 | 联系管理员 |
| BOOK_001 | 404 | 图书不存在 | 检查图书ID |
| BOOK_002 | 409 | ISBN已存在 | 使用其他ISBN |
| BOOK_003 | 400 | 库存不足 | 等待归还或预约 |
| BORROW_001 | 400 | 读者已借满5本书 | 归还图书后借阅 |
| BORROW_002 | 400 | 图书已借出 | 等待归还或预约 |
| BORROW_003 | 400 | 借阅已逾期 | 先归还并处理罚款 |
| SEAT_001 | 400 | 座位已被预约 | 选择其他时间段 |
| SEAT_002 | 400 | 预约时间冲突 | 修改预约时间 |
| POINTS_001 | 400 | 积分不足 | 赚取更多积分 |
| POINTS_002 | 400 | 奖励已兑完 | 选择其他奖励 |

# 架构设计文档

本文档详细介绍图书馆管理系统的技术架构和设计决策。

## 版本信息

- 文档版本：2.0.0
- 最后更新：2026-04-23
- 维护者：开发团队

---

## 系统架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                         客户端层                                  │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │   Web浏览器   │  │    移动端     │  │    小程序     │          │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘          │
└─────────┼─────────────────┼─────────────────┼──────────────────┘
          │                 │                 │
          └─────────────────┼─────────────────┘
                            │ HTTPS
┌───────────────────────────┼─────────────────────────────────────┐
│                           ▼                                       │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │                      Nginx反向代理                            │ │
│  │              (负载均衡 / SSL终止 / 静态资源)                   │ │
│  └─────────────────────────────────────────────────────────────┘ │
│                           │                                       │
└───────────────────────────┼───────────────────────────────────────┘
                            │
┌───────────────────────────┼───────────────────────────────────────┐
│                           ▼                                       │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │                     Spring Boot 应用                         │ │
│  │  ┌───────────┐  ┌───────────┐  ┌───────────┐                │ │
│  │  │Controller │──│ Service   │──│ Mapper    │                │ │
│  │  └───────────┘  └───────────┘  └───────────┘                │ │
│  │        │            │            │                          │ │
│  │        ▼            ▼            ▼                          │ │
│  │  ┌───────────┐  ┌───────────┐  ┌───────────┐                │ │
│  │  │Security   │  │Cache(L1)  │  │ Database  │                │ │
│  │  │Filter     │  │Caffeine   │  │MySQL 8.0  │                │ │
│  │  └───────────┘  └───────────┘  └───────────┘                │ │
│  └─────────────────────────────────────────────────────────────┘ │
│                           │                                       │
└───────────────────────────┼───────────────────────────────────────┘
                            │
          ┌─────────────────┼─────────────────┐
          │                 │                 │
          ▼                 ▼                 ▼
    ┌───────────┐    ┌───────────┐    ┌───────────┐
    │ Redis L2  │    │   MySQL   │    │  Actuator │
    │ Cache     │    │ Database  │    │  Monitor  │
    └───────────┘    └───────────┘    └───────────┘
```

## 技术选型说明

### 后端技术选型

#### Spring Boot 3.2.5

- **原因**：LTS版本，稳定可靠
- **优势**：
  - 自动配置，简化开发
  - 嵌入式服务器，部署便捷
  - 丰富的Starter生态

#### MyBatis Plus 3.5.5

- **原因**：简化CRUD操作，提升开发效率
- **优势**：
  - 强大的ORM映射
  - 内置分页插件
  - 代码生成器

#### Redis + Redisson

- **原因**：分布式缓存和分布式锁
- **优势**：
  - 高性能缓存
  - 分布式锁支持
  - 消息队列功能

#### Caffeine Cache

- **原因**：本地热点缓存
- **优势**：
  - 高性能本地缓存
  - 低延迟访问
  - 与Spring Cache集成

#### JWT

- **原因**：无状态认证
- **优势**：
  - 无服务端会话存储
  - 支持跨域认证
  - Token可携带用户信息

### 前端技术选型

#### Vue 3

- **原因**：渐进式前端框架
- **优势**：
  - Composition API
  - 更好的性能
  - TypeScript支持

#### Element Plus

- **原因**：成熟的UI组件库
- **优势**：
  - 丰富的组件
  - 良好的文档
  - 持续更新维护

#### Pinia

- **原因**：Vue 3推荐状态管理
- **优势**：
  - 轻量级
  - TypeScript支持
  - DevTools集成

## 模块划分

### 后端模块结构

```
com.library.system
├── config/                    # 配置模块
│   ├── SecurityConfig        # 安全配置
│   ├── RedisConfig           # Redis配置
│   ├── CaffeineConfig        # Caffeine配置
│   ├── WebMvcConfig          # Web配置
│   └── SwaggerConfig         # API文档配置
│
├── controller/               # 控制器模块
│   ├── AuthController        # 认证
│   ├── BookController        # 图书管理
│   ├── ReaderController      # 读者管理
│   ├── BorrowRecordController# 借阅管理
│   ├── SeatController        # 座位预约
│   ├── AnnouncementController# 公告管理
│   └── VolunteerController   # 志愿服务
│
├── service/                  # 服务层模块
│   ├── impl/                 # 服务实现
│   └── interfaces/           # 服务接口
│
├── mapper/                   # 数据访问模块
│   └── (MyBatis Plus Mapper)
│
├── entity/                  # 实体模块
│   ├── User
│   ├── Book
│   ├── Reader
│   ├── BorrowRecord
│   ├── Seat
│   ├── SeatReservation
│   ├── Announcement
│   ├── VolunteerActivity
│   └── Compensation
│
├── dto/                     # 数据传输对象
│   ├── request/             # 请求DTO
│   └── response/            # 响应DTO
│
├── vo/                      # 视图对象
│
├── security/                # 安全模块
│   ├── JwtTokenProvider     # Token提供器
│   ├── JwtAuthenticationFilter # 认证过滤器
│   ├── UserDetailsServiceImpl  # 用户详情服务
│   └── TokenBlacklistService   # Token黑名单
│
├── common/                  # 通用模块
│   ├── Result               # 统一响应
│   ├── Constants            # 常量定义
│   └── enums/               # 枚举类
│
└── exception/               # 异常模块
    ├── GlobalExceptionHandler # 全局异常处理
    └── business/            # 业务异常
```

### 前端模块结构

```
src/
├── api/                     # API接口
│   ├── auth.js
│   ├── book.js
│   ├── reader.js
│   ├── borrow.js
│   ├── seat.js
│   └── announcement.js
│
├── components/              # 公共组件
│   ├── common/             # 通用组件
│   └── business/           # 业务组件
│
├── composables/            # 组合式函数
│   ├── useAuth.js
│   ├── useBook.js
│   └── usePagination.js
│
├── router/                  # 路由
│   └── index.js
│
├── store/                   # 状态管理
│   ├── user.js
│   └── app.js
│
├── views/                   # 页面视图
│   ├── auth/               # 认证页面
│   ├── book/               # 图书页面
│   ├── reader/             # 读者页面
│   ├── borrow/             # 借阅页面
│   ├── seat/               # 座位页面
│   └── dashboard/          # 仪表盘
│
└── styles/                 # 样式
    ├── variables.scss
    ├── mixins.scss
    └── global.scss
```

## 核心流程

### 用户认证流程

```
用户登录
    │
    ▼
输入用户名密码
    │
    ▼
调用 /api/auth/login
    │
    ├─── 验证成功 ──→ 生成JWT Token ──→ 返回Token给前端
    │                                       │
    │                                       ▼
    │                               前端存储Token
    │                                       │
    │                                       ▼
    │                               后续请求携带Token
    │
    └─── 验证失败 ──→ 返回错误信息
```

### 借阅流程

```
读者借书请求
    │
    ▼
检查读者状态
    │
    ├─── 状态异常 ──→ 返回错误
    │
    ▼
检查借阅数量限制(≤5本)
    │
    ├─── 已达上限 ──→ 返回错误
    │
    ▼
检查图书库存
    │
    ├─── 无库存 ──→ 返回错误
    │
    ▼
创建借阅记录
    │
    ▼
扣减图书库存
    │
    ▼
记录操作日志
    │
    ▼
返回成功
```

### 座位预约流程

```
用户发起预约
    │
    ▼
检查预约时间是否有效
    │
    ├─── 无效 ──→ 返回错误
    │
    ▼
检查座位是否已被预约
    │
    ├─── 已预约 ──→ 返回冲突错误
    │
    ▼
获取分布式锁(防止并发)
    │
    │  ┌────────────────────────────────────────────┐
    │  │  使用Redisson实现分布式锁                    │
    │  │  锁Key格式: seat:lock:{seatId}:{date}      │
    │  │  锁超时时间: 10秒                           │
    │  │  等待获取锁超时: 3秒                         │
    │  └────────────────────────────────────────────┘
    ▼
再次检查座位可用性
    │
    ├─── 不可用 ──→ 释放锁,返回错误
    │
    ▼
创建预约记录
    │
    ▼
释放分布式锁
    │
    ▼
发送预约成功通知
    │
    ▼
返回成功
```

> **分布式锁说明**：使用Redisson的`RLock`实现分布式锁，确保在集群环境下同一座位同一时间段只能被一个用户成功预约。锁Key采用座位ID+日期的组合，支持高并发场景下的预约冲突检测。

## 数据库设计

### ER图

```
┌─────────────┐       ┌─────────────┐       ┌─────────┐
│   sys_user  │──────│ BorrowRecord│───────│  Book   │
└─────────────┘       └─────────────┘       └─────────┘
    │                    │                    │
    │                    │                    │
    ▼                    ▼                    ▼
┌─────────┐       ┌─────────────┐       ┌─────────────┐
│ Reader  │       │ Compensation│       │  Category  │
└─────────┘       └─────────────┘       └─────────────┘

┌─────────────────┐       ┌─────────────────────┐
│ SeatReservation │──────│        Seat         │
└─────────────────┘       └─────────────────────┘

┌─────────────────┐       ┌─────────────────────┐
│ VolunteerRecord │──────│ VolunteerActivity   │
└─────────────────┘       └─────────────────────┘
```

### 主要表结构

#### 用户表 (sys_user)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| username | VARCHAR(50) | 用户名 |
| password | VARCHAR(255) | 密码（加密） |
| role | VARCHAR(20) | 角色 |
| status | INT | 状态 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

#### 图书表 (book)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| isbn | VARCHAR(20) | ISBN |
| title | VARCHAR(200) | 书名 |
| author | VARCHAR(100) | 作者 |
| publisher | VARCHAR(100) | 出版社 |
| publish_date | DATE | 出版日期 |
| category | VARCHAR(50) | 分类 |
| price | DECIMAL(10,2) | 价格 |
| total_copies | INT | 总数量 |
| stock | INT | 可借数量 |
| status | INT | 状态 |

#### 借阅记录表 (borrow_record)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| reader_id | BIGINT | 读者ID |
| book_id | BIGINT | 图书ID |
| borrow_date | DATETIME | 借书日期 |
| due_date | DATETIME | 应还日期 |
| return_date | DATETIME | 实际还书日期 |
| status | INT | 状态 |

#### 座位预约表 (seat_reservation)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| seat_id | BIGINT | 座位ID |
| user_id | BIGINT | 用户ID |
| reservation_date | DATE | 预约日期 |
| start_time | TIME | 开始时间 |
| end_time | TIME | 结束时间 |
| status | INT | 状态 |

### 索引设计

```sql
-- 图书表索引
CREATE INDEX idx_book_isbn ON book(isbn);
CREATE INDEX idx_book_category_id ON book(category_id);
CREATE INDEX idx_book_status ON book(status);

-- 借阅记录表索引
CREATE INDEX idx_borrow_reader ON borrow_record(reader_id);
CREATE INDEX idx_borrow_book ON borrow_record(book_id);
CREATE INDEX idx_borrow_status ON borrow_record(status);
CREATE INDEX idx_borrow_due_date ON borrow_record(due_date);

-- 座位预约表索引
CREATE INDEX idx_seat_reservation_seat ON seat_reservation(seat_id);
CREATE INDEX idx_seat_reservation_date ON seat_reservation(reservation_date);
CREATE INDEX idx_seat_reservation_user ON seat_reservation(user_id);

-- 用户表索引
CREATE INDEX idx_sys_user_username ON sys_user(username);
CREATE INDEX idx_sys_user_card_number ON sys_user(card_number);
CREATE INDEX idx_sys_user_role ON sys_user(role);
```

## 安全设计

### 认证授权

- JWT Token认证
- RBAC角色权限控制
- Token黑名单机制
- 账户锁定策略

### 数据安全

- 密码BCrypt加密
- XSS防护（OWASP Sanitizer）
- SQL注入防护（MyBatis参数绑定）
- 敏感数据脱敏

### 接口安全

- API限流（Redis滑动窗口）
- CORS跨域配置
- 请求参数校验
- 统一异常处理

## 性能优化

### 缓存策略

```
请求 → Caffeine(L1) → Redis(L2) → MySQL
              ↑             ↑
           命中返回      命中返回
                       未命中查库
```

### 数据库优化

- Druid连接池（100连接）
- N+1查询消除
- 批量插入优化
- 数据库索引优化

### 前端优化

- 路由懒加载
- 组件按需引入
- 图片懒加载
- 虚拟滚动（长列表）

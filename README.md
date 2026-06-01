# 图书馆管理系统 V2.0

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen)](https://spring.io/projects/spring-boot)
[![Vue.js](https://img.shields.io/badge/Vue.js-3.4-blue)](https://vuejs.org/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-orange)](https://www.mysql.com/)
[![Redis](https://img.shields.io/badge/Redis-7.0-red)](https://redis.io/)
[![License](https://img.shields.io/badge/License-MIT-yellow)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17+-blueviolet)](https://adoptium.org/)
[![Node](https://img.shields.io/badge/Node.js-18+-green)](https://nodejs.org/)

基于 Spring Boot 3.2.5 + Vue 3 的现代化图书馆管理系统，完全重构自原项目。

## 项目概述

本项目是根据论文《基于Javaweb的图书馆管理系统的设计与实现》重新构建的全新系统，采用简洁清晰架构，实现论文中描述的所有功能。

### 技术栈

**后端**
- Spring Boot 3.2.5
- MyBatis-Plus 3.5.6
- Redis 7 + Caffeine（二级缓存）
- JWT 双 Token 认证
- Redisson 分布式锁

**前端**
- Vue 3.4 + Composition API
- Element Plus 2.5
- Vite 5
- Pinia 状态管理
- Vue Router 4

## 快速开始

### 1. 环境要求

- JDK 17+
- Node.js 18+
- MySQL 8.0
- Redis 7+

### 2. 数据库初始化

本项目使用 **Flyway** 进行数据库版本管理与自动迁移。Spring Boot 启动时会自动执行 `backend/src/main/resources/db/migration/` 下的迁移脚本，无需手动执行 SQL。

**前置条件**：确保 MySQL 中已存在名为 `library_system` 的空数据库，Flyway 将按版本顺序创建并升级所有表结构。

```bash
# 登录MySQL并创建空数据库（仅需首次执行）
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS library_system CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
```

### 3. 后端启动

```bash
cd backend

# 方式一：使用Maven
./mvnw spring-boot:run

# 方式二：打包后运行
./mvnw clean package -DskipTests
java -jar target/library-system-2.0.0.jar
```

### 4. 前端启动

```bash
cd frontend

# 安装依赖
npm install

# 启动开发服务器
npm run dev
```

### 5. 访问系统

- 前端地址：http://localhost:5173
- 后端API：http://localhost:8080/api
- 默认管理员账户：`admin` / `admin123`

## 核心功能

### 1. 用户认证
- JWT 双 Token（Access 2小时 + Refresh 7天）
- 账户锁定（5次失败/15分钟）
- 密码强度校验

### 2. 图书管理
- CRUD 操作
- ISBN 去重校验
- 分类管理
- 热门/新书推荐

### 3. 借阅管理
- 借书/还书/续借
- 借阅限制（5本/人/30天）
- 逾期罚款

### 4. 座位预约
- 阅览室管理
- 可视化选座
- 签到/签退
- 违约检测

### 5. 信用积分
- 积分制度（初始100分）
- 等级：铜牌/银牌/金牌/白金
- 积分日志

### 6. 其他功能
- 公告管理
- 志愿服务
- 统计分析

## 项目结构

```
library-system-v2/
├── backend/                    # Spring Boot 后端
│   ├── src/main/java/com/library/system/
│   │   ├── controller/       # 控制器层
│   │   ├── service/          # 服务层
│   │   ├── mapper/          # 数据访问层
│   │   ├── entity/           # 实体类
│   │   ├── dto/              # 数据传输对象
│   │   ├── config/           # 配置类
│   │   ├── security/         # 安全模块
│   │   └── common/           # 公共模块
│   ├── src/main/resources/
│   │   ├── db/migration/     # Flyway 迁移脚本
│   │   ├── mapper/           # MyBatis XML
│   │   └── application.yml   # 配置文件
│   └── pom.xml
│
├── frontend/                  # Vue.js 前端
│   ├── src/
│   │   ├── api/              # API接口封装
│   │   ├── components/        # 公共组件
│   │   ├── views/             # 页面视图
│   │   ├── router/            # 路由配置
│   │   ├── stores/            # 状态管理
│   │   └── styles/            # 样式文件
│   ├── package.json
│   └── vite.config.js
│
├── docs/                      # 文档目录
│   ├── API.md
│   ├── ARCHITECTURE.md
│   ├── DEVELOPMENT.md
│   └── DEPLOYMENT.md
│
└── README.md
```

## API 文档

启动后端后，访问：
- Swagger UI: http://localhost:8080/api/swagger-ui.html
- Knife4j: http://localhost:8080/api/doc.html

## 测试

```bash
# 后端单元测试
cd backend
./mvnw test

# 前端测试
cd frontend
npm run test
```

## 论文对应

本项目完全实现了论文《基于Javaweb的图书馆管理系统的设计与实现》中描述的所有功能：

| 论文章节 | 实现内容 |
|---------|---------|
| 2.2.3 JWT双Token | JwtUtils, JwtFilter |
| 2.5.1 Caffeine缓存 | CacheConfig |
| 2.5.2 二级缓存 | Redis + Caffeine |
| 2.5.3 布隆过滤器 | BloomFilterService |
| 2.5.4 并发控制 | @Version, Redisson |
| 4.2.1 功能模块 | 10个核心模块 |
| 5.2.1 认证机制 | AuthController/Service |
| 5.2.2 二级缓存 | CacheConfig |
| 5.2.3 并发控制 | BorrowService |
| 5.3.1 信用积分 | CreditService |

## License

MIT License

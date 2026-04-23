# 部署指南

本文档详细介绍图书馆管理系统的部署流程，包括开发环境搭建和生产环境部署。

## 环境要求

### 硬件要求

| 环境 | CPU | 内存 | 硬盘 |
|------|-----|------|------|
| 开发环境 | 2核+ | 4GB+ | 20GB+ |
| 生产环境 | 4核+ | 8GB+ | 50GB+ |

### 软件要求

| 软件 | 版本 | 说明 |
|------|------|------|
| JDK | 17+ | 后端运行环境 |
| Node.js | 18+ | 前端构建工具 |
| MySQL | 8.0+ | 主数据库 |
| Redis | 7.0+ | 缓存和Session存储 |
| Nginx | 1.20+ | 反向代理服务器 |
| Docker | 20.10+ | 容器化部署 |

## 开发环境搭建

### 1. 安装基础软件

```bash
# 安装JDK 17
# Windows: https://adoptium.net/
# macOS: brew install openjdk@17
# Linux: sudo apt install openjdk-17-jdk

# 安装Node.js
# Windows: https://nodejs.org/
# macOS: brew install node@18
# Linux: curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -

# 安装MySQL
# 参考: https://dev.mysql.com/downloads/mysql/

# 安装Redis
# Windows: https://github.com/microsoftarchive/redis/releases
# macOS: brew install redis
# Linux: sudo apt install redis-server
```

### 2. 配置MySQL

```bash
# 登录MySQL
mysql -u root -p

# 创建数据库
CREATE DATABASE library_system CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# 创建用户（可选）
CREATE USER 'library'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON library_system.* TO 'library'@'localhost';
FLUSH PRIVILEGES;
```

### 3. 配置Redis

```bash
# 启动Redis服务
# Windows
redis-server

# macOS/Linux
redis-server /usr/local/etc/redis.conf

# 测试连接
redis-cli ping
```

### 4. 导入数据库脚本

```bash
# 进入项目目录
cd library-system-v2

# 执行SQL脚本（如果存在）
mysql -u root -p library_system < sql/init.sql
```

### 5. 配置后端环境

```bash
# 进入后端目录
cd backend

# 复制环境变量模板
cp .env.example .env

# 编辑环境变量文件
# Windows: notepad .env
# macOS/Linux: vim .env

# 主要配置项
DB_HOST=localhost
DB_PORT=3306
DB_NAME=library_system
DB_USERNAME=root
DB_PASSWORD=your_password

REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=your_redis_password

JWT_SECRET=your_jwt_secret_key_at_least_32_characters
```

### 6. 启动后端服务

```bash
# 编译项目
mvn clean compile

# 运行测试
mvn test

# 启动应用
mvn spring-boot:run

# 或打包后运行
mvn clean package -DskipTests
java -jar target/library-system-2.0.0.jar
```

### 7. 配置前端环境

```bash
# 进入前端目录
cd frontend

# 创建环境变量文件
cp .env.example .env

# 编辑环境变量
VITE_API_BASE_URL=http://localhost:8080/api
```

### 8. 启动前端服务

```bash
# 安装依赖
npm install

# 启动开发服务器
npm run dev

# 访问 http://localhost:5173
```

## 生产环境部署

### 方案一：传统部署

#### 1. 服务器准备

```bash
# 更新系统
sudo apt update && sudo apt upgrade -y

# 安装依赖
sudo apt install -y openjdk-17-jdk nginx certbot

# 配置防火墙
sudo ufw allow 22
sudo ufw allow 80
sudo ufw allow 443
```

#### 2. 构建项目

```bash
# 克隆项目
git clone <repository_url>
cd library-system-v2

# 构建后端
cd backend
mvn clean package -DskipTests -Pprod
cd ..

# 构建前端
cd frontend
npm install
npm run build
cd ..
```

#### 3. 配置Nginx

```bash
sudo vim /etc/nginx/sites-available/library-system
```

```nginx
server {
    listen 80;
    server_name your_domain.com;

    # 前端静态文件
    location / {
        root /path/to/library-system-v2/frontend/dist;
        try_files $uri $uri/ /index.html;
    }

    # API反向代理
    location /api {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # WebSocket支持（如需要）
    location /ws {
        proxy_pass http://localhost:8080;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }
}
```

```bash
# 启用站点
sudo ln -s /etc/nginx/sites-available/library-system /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
```

#### 4. 配置SSL证书

```bash
# 申请Let's Encrypt证书
sudo certbot --nginx -d your_domain.com
```

#### 5. 部署后端服务

```bash
# 创建systemd服务
sudo vim /etc/systemd/system/library-backend.service
```

```ini
[Unit]
Description=Library Management System Backend
After=network.target mysql.service redis.service

[Service]
Type=simple
User=www-data
WorkingDirectory=/path/to/library-system-v2/backend
ExecStart=/usr/bin/java -jar target/library-system-2.0.0.jar --spring.profiles.active=prod
Restart=on-failure
Environment=JAVA_OPTS="-Xmx2g -Xms512m"

[Install]
WantedBy=multi-user.target
```

```bash
# 启动服务
sudo systemctl daemon-reload
sudo systemctl enable library-backend
sudo systemctl start library-backend

# 检查状态
sudo systemctl status library-backend
```

### 方案二：Docker部署

#### 1. 安装Docker

```bash
# 安装Docker
curl -fsSL https://get.docker.com | sh

# 安装Docker Compose
sudo apt install docker-compose

# 添加当前用户到docker组
sudo usermod -aG docker $USER
```

#### 2. 配置Docker Compose

```bash
# 创建docker-compose.yml
vim docker-compose.yml
```

```yaml
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    container_name: library-mysql
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      MYSQL_DATABASE: library_system
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
      - ./sql/init.sql:/docker-entrypoint-initdb.d/init.sql
    networks:
      - library-network

  redis:
    image: redis:7-alpine
    container_name: library-redis
    command: redis-server --requirepass ${REDIS_PASSWORD}
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    networks:
      - library-network

  backend:
    build: ./backend
    container_name: library-backend
    environment:
      SPRING_PROFILES_ACTIVE: prod
      DB_HOST: mysql
      DB_PORT: 3306
      DB_NAME: library_system
      DB_USERNAME: root
      DB_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      REDIS_HOST: redis
      REDIS_PORT: 6379
      REDIS_PASSWORD: ${REDIS_PASSWORD}
    ports:
      - "8080:8080"
    depends_on:
      - mysql
      - redis
    networks:
      - library-network

  frontend:
    image: nginx:alpine
    container_name: library-frontend
    volumes:
      - ./frontend/dist:/usr/share/nginx/html
      - ./nginx.conf:/etc/nginx/conf.d/default.conf
    ports:
      - "80:80"
    depends_on:
      - backend
    networks:
      - library-network

volumes:
  mysql_data:
  redis_data:

networks:
  library-network:
    driver: bridge
```

#### 3. 生产环境 docker-compose.prod.yml 示例

生产环境配置完整示例，包含安全加固和环境变量管理：

```yaml
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    container_name: library-mysql-prod
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      MYSQL_DATABASE: library_system
      MYSQL_CHARACTER_SET_SERVER: utf8mb4
      MYSQL_COLLATION_SERVER: utf8mb4_unicode_ci
    volumes:
      - mysql_data_prod:/var/lib/mysql
      - ./sql/schema.sql:/docker-entrypoint-initdb.d/01-schema.sql
      - ./sql/data.sql:/docker-entrypoint-initdb.d/02-data.sql
    networks:
      - library-network-prod
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-u", "root", "-p${MYSQL_ROOT_PASSWORD}"]
      interval: 10s
      timeout: 5s
      retries: 5
    restart: unless-stopped
    deploy:
      resources:
        limits:
          memory: 2G
        reservations:
          memory: 512M

  redis:
    image: redis:7-alpine
    container_name: library-redis-prod
    command: >
      redis-server
      --requirepass ${REDIS_PASSWORD}
      --maxmemory 512mb
      --maxmemory-policy allkeys-lru
      --appendonly yes
      --appendfsync everysec
    volumes:
      - redis_data_prod:/data
    networks:
      - library-network-prod
    healthcheck:
      test: ["CMD", "redis-cli", "--raw", "incr", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5
    restart: unless-stopped
    deploy:
      resources:
        limits:
          memory: 1G

  backend:
    build:
      context: ./backend
      dockerfile: Dockerfile
    container_name: library-backend-prod
    environment:
      SPRING_PROFILES_ACTIVE: prod
      DB_HOST: mysql
      DB_PORT: 3306
      DB_NAME: library_system
      DB_USERNAME: root
      DB_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      REDIS_HOST: redis
      REDIS_PORT: 6379
      REDIS_PASSWORD: ${REDIS_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
      JAVA_OPTS: "-Xmx1g -Xms512m -XX:+UseG1GC"
    ports:
      - "127.0.0.1:8080:8080"
    depends_on:
      mysql:
        condition: service_healthy
      redis:
        condition: service_healthy
    networks:
      - library-network-prod
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s
    deploy:
      resources:
        limits:
          memory: 1.5G
        reservations:
          memory: 512M

  frontend:
    image: nginx:alpine
    container_name: library-frontend-prod
    volumes:
      - ./frontend/dist:/usr/share/nginx/html:ro
      - ./nginx/nginx.conf:/etc/nginx/conf.d/default.conf:ro
    ports:
      - "80:80"
      - "443:443"
    depends_on:
      - backend
    networks:
      - library-network-prod
    restart: unless-stopped

volumes:
  mysql_data_prod:
  redis_data_prod:

networks:
  library-network-prod:
    driver: bridge
```

**生产环境安全要点：**
- 数据库和Redis端口不暴露到主机，仅容器间通信
- 后端API仅绑定本地回环接口，由Nginx代理访问
- 使用环境变量管理敏感信息，不要硬编码密码
- 启用Redis持久化和内存限制
- 配置健康检查确保服务可用性
- 设置资源限制防止单个服务耗尽系统资源

**启动生产环境：**
```bash
# 设置环境变量
export MYSQL_ROOT_PASSWORD="your_secure_password"
export REDIS_PASSWORD="your_secure_redis_password"
export JWT_SECRET="your_jwt_secret_at_least_32_characters"

# 启动服务
docker-compose -f docker-compose.prod.yml up -d

# 查看服务状态
docker-compose -f docker-compose.prod.yml ps

# 查看日志
docker-compose -f docker-compose.prod.yml logs -f backend
```

#### 4. 启动服务

```bash
# 启动所有服务
docker-compose up -d

# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f

# 停止服务
docker-compose down
```

## 数据库迁移

### 使用Flyway（推荐）

```bash
# 在pom.xml中添加Flyway依赖后执行
mvn flyway:migrate
```

### 手动迁移

```bash
# 备份数据
mysqldump -u root -p library_system > backup_$(date +%Y%m%d).sql

# 执行SQL脚本
mysql -u root -p library_system < migration_script.sql
```

## 常见问题与故障排查

### 1. 数据库连接失败

**症状**：应用启动时报 `Communications link failure` 或 `Access denied`

**排查步骤**：
1. 检查MySQL服务是否启动：`systemctl status mysql` 或 `docker ps`
2. 验证连接信息：检查 `application.yml` 中的 host/port/username/password
3. 确认数据库已创建：`SHOW DATABASES;`
4. 检查防火墙：`sudo ufw status` / `sudo iptables -L`
5. 远程连接测试：`mysql -h <host> -P <port> -u <user> -p`

**解决方案**：
- 启动MySQL服务
- 修正环境变量配置
- 创建数据库：`CREATE DATABASE library_system CHARACTER SET utf8mb4;`

### 2. Redis连接失败

**症状**：应用启动时报 `Cannot connect to Redis` 或超时不响应

**排查步骤**：
1. 检查Redis服务：`redis-cli ping`（本地）或 `docker exec redis redis-cli ping`
2. 验证密码：`redis-cli -a <password> ping`
3. 检查网络连通性：`telnet <host> 6379`
4. 查看Redis日志：`tail -f /var/log/redis/redis.log`

**解决方案**：
- 启动Redis服务
- 确认密码配置与环境变量一致
- 检查Docker网络配置是否正确

### 3. 前端无法访问后端API

**症状**：前端页面加载失败，或请求返回404/502

**排查步骤**：
1. 确认后端服务运行：`curl http://localhost:8080/actuator/health`
2. 检查Nginx配置：`nginx -t`
3. 验证API代理设置：检查 `location /api` 块配置
4. 查看Nginx错误日志：`tail -f /var/log/nginx/error.log`
5. 检查跨域配置：确认CORS设置允许前端域名

**解决方案**：
- 重启后端服务
- 重新加载Nginx配置：`nginx -s reload`
- 检查 `VITE_API_BASE_URL` 环境变量配置

### 4. 内存不足（OOM）

**症状**：应用崩溃或响应缓慢，查看日志有 `OutOfMemoryError`

**排查步骤**：
1. 检查JVM内存使用：`jmap -heap <pid>`
2. 查看容器内存限制：`docker stats`
3. 分析堆转储：`jmap -dump:format=b,file=heap.hprof <pid>`

**解决方案**：
- 增加JVM堆内存：`JAVA_OPTS="-Xmx4g -Xms2g"`
- 优化Docker Compose资源限制
- 检查内存泄漏，使用VisualVM分析

### 5. JWT认证失败

**症状**：登录后API请求返回401 Unauthorized

**排查步骤**：
1. 检查Token是否过期（AccessToken有效期2小时）
2. 验证Token格式：请求头应为 `Bearer <token>`
3. 检查JWT密钥是否一致：`echo $JWT_SECRET`
4. 确认Redis用于存储黑名单/会话

**解决方案**：
- 刷新Token：`POST /api/auth/refresh`
- 检查JWT密钥配置是否正确
- 清除浏览器缓存重新登录

### 6. 座位预约冲突

**症状**：多个用户同时预约同一座位，全部成功但数据异常

**排查步骤**：
1. 检查Redisson分布式锁是否正常工作
2. 查看预约记录是否有重叠时间段
3. 检查数据库隔离级别

**解决方案**：
- 确保Redis正常运行
- 检查锁超时配置是否合理
- 查看应用日志确认锁获取/释放情况

### 7. Docker容器启动失败

**症状**：`docker-compose up` 报错或容器立即退出

**排查步骤**：
1. 查看容器日志：`docker-compose logs <service>`
2. 检查端口占用：`netstat -tlnp | grep <port>`
3. 验证环境变量文件：`.env` 文件是否存在
4. 检查Docker日志：`journalctl -u docker`

**解决方案**：
- 修正配置后重新构建：`docker-compose down && docker-compose up -d --build`
- 释放占用的端口或修改配置使用其他端口
- 确保 `.env` 文件包含所有必需变量

### 8. 数据库迁移失败

**症状**：Flyway或手动SQL执行报错

**排查步骤**：
1. 查看Flyway日志确认失败原因
2. 检查SQL语法是否正确
3. 确认迁移脚本版本顺序

**解决方案**：
- 修复SQL后重新执行
- 使用 `flyway:repair` 修复历史记录
- 手动备份数据后重建数据库

## 监控与日志

### Actuator端点

| 端点 | 方法 | 说明 |
|------|------|------|
| `/actuator/health` | GET | 健康检查，包含Redis、MySQL连接状态 |
| `/actuator/health/db` | GET | 数据库健康状态 |
| `/actuator/health/redis` | GET | Redis健康状态 |
| `/actuator/metrics` | GET | 所有性能指标列表 |
| `/actuator/metrics/jvm.memory.used` | GET | JVM内存使用情况 |
| `/actuator/metrics/http.server.requests` | GET | HTTP请求统计 |
| `/actuator/info` | GET | 应用自定义信息 |
| `/actuator/env` | GET | 环境变量（需谨慎） |

**生产环境Actuator安全配置**：
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,info
      base-path: /actuator
  endpoint:
    health:
      show-details: when_authorized
  health:
    redis:
      enabled: true
    db:
      enabled: true
```

### 日志级别说明

| 级别 | 使用场景 | 日志量 |
|------|----------|--------|
| ERROR | 错误异常，影响业务 | 少量 |
| WARN | 潜在问题，警告信息 | 少量 |
| INFO | 重要业务流程节点 | 中等 |
| DEBUG | 开发调试，详细信息 | 大量 |
| TRACE | 最详细，追踪问题 | 极大 |

### 日志查看方法

**本地日志文件**：
```bash
# 实时查看应用日志
tail -f logs/library-system.log

# 查看最近100行日志
tail -n 100 logs/library-system.log

# 搜索错误日志
grep -i error logs/library-system.log

# 按日期查看
grep "2026-04-23" logs/library-system.log

# 查看ERROR级别日志
grep "ERROR" logs/library-system.log | tail -50
```

**Docker容器日志**：
```bash
# 查看后端容器日志
docker logs -f library-backend

# 查看最近50行
docker logs --tail 50 library-backend

# 按时间范围查看
docker logs --since "2026-04-23T10:00:00" library-backend

# 搜索错误日志
docker logs library-backend 2>&1 | grep -i error
```

**日志格式解析**：
```
2026-04-23 10:30:15.123 [http-nio-8080-exec-1] INFO  c.l.s.service.BookService - [getBookById] - 图书ID: 123 查询成功
   |       |        |        |         |        |       |
   时间     线程名  级别     类名       方法名   额外信息  业务日志
```

**常用日志分析命令**：
```bash
# 统计各级别日志数量
grep -c "ERROR" logs/library-system.log
grep -c "WARN" logs/library-system.log

# 查看某个用户的操作日志
grep "userId:123" logs/library-system.log

# 分析接口响应时间（需开启DEBUG）
grep "执行时长" logs/library-system.log | awk '{print $NF}' | sort -n

# 统计API调用频率
grep "请求路径" logs/library-system.log | awk '{print $NF}' | sort | uniq -c | sort -rn | head -10
```

### 日志配置

```yaml
# application-prod.yml
logging:
  level:
    # 业务日志
    com.library: INFO
    # Spring框架日志
    org.springframework: WARN
    org.springframework.web: INFO
    org.mybatis: DEBUG
    # 数据库日志
    com.zaxxer.hikari: INFO
  pattern:
    # 日志输出格式
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
  file:
    name: logs/library-system.log
    max-size: 100MB
    max-history: 30
  logback:
    rollingpolicy:
      total-size-cap: 3GB
```

## 安全建议

1. **修改默认密码**：立即修改数据库和Redis的默认密码
2. **配置SSL**：生产环境必须启用HTTPS
3. **定期备份**：配置自动备份策略
4. **日志审计**：启用安全日志记录
5. **最小权限**：数据库用户使用最小必要权限

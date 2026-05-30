# CI/CD 部署指南

## 概述

本项目使用 **GitHub Actions** 实现自动化 CI/CD，提交代码后自动完成构建、测试、镜像发布和远程部署。

## CI 工作流

### 触发条件

| 事件 | 分支 | 操作 |
|------|------|------|
| `push` | `main`, `develop`, `feature/**`, `fix/**` | 运行所有测试 + 构建验证 |
| `pull_request` | `main`, `develop` | 运行所有测试 + Docker 构建检查 |

### 包含的 Job

| Job | 内容 | 超时 |
|-----|------|------|
| `lint` | 后端 Checkstyle + 前端 ESLint | 10min |
| `backend-unit-test` | Maven 单元测试 + JaCoCo 覆盖率 | 20min |
| `backend-integration-test` | Testcontainers 集成测试 | 30min |
| `frontend-unit-test` | Vitest 单元测试 + 覆盖率 | 15min |
| `frontend-build` | Vite 构建验证 | 10min |
| `docker-build-check` | Docker 镜像构建验证（仅 PR） | 15min |
| `coverage-report` | 覆盖率汇总报告 | 5min |

## CD 工作流

### 触发条件

| 事件 | 分支 | 操作 |
|------|------|------|
| `push` | `main` | 测试→构建镜像→推送到 GHCR→SSH 部署 |
| `workflow_dispatch` | 任意 | 手动选择环境和选项进行部署 |

### 包含的 Job

| Job | 内容 | 超时 |
|-----|------|------|
| `test` | 后端 + 前端测试 | 15min |
| `build-and-push` | 构建 Docker 镜像并推送到 GHCR | 20min |
| `deploy` | SSH 连接到服务器，执行 docker compose 部署 | 15min |

## 本地测试 CI/CD

### 1. 后端测试

```bash
cd backend

# 单元测试
mvn test

# 集成测试
mvn verify -P integration-test

# 覆盖率报告
mvn test jacoco:report
# 报告位置: backend/target/site/jacoco/index.html
```

### 2. 前端测试

```bash
cd frontend

# 安装依赖
npm install

# 单元测试
npx vitest run

# 带覆盖率
npx vitest run --coverage

# E2E 测试（需要先启动应用）
npx playwright test
```

### 3. Docker 构建

```bash
# 构建后端
docker build -t library-backend:local ./backend

# 构建前端
docker build \
  --build-arg VITE_API_BASE_URL=/api/v1 \
  -t library-frontend:local \
  ./frontend

# 本地运行完整系统
docker compose up -d
```

## 服务器部署准备

### 1. 服务器要求

- Linux 服务器（推荐 Ubuntu 22.04+ / CentOS 7+）
- Docker 24.0+ 和 Docker Compose v2
- 开放端口：80 (HTTP), 443 (HTTPS, 可选), 22 (SSH)

### 2. 部署目录结构

```bash
# 在服务器上创建部署目录
mkdir -p /opt/library-system
cd /opt/library-system

# 目录结构
/opt/library-system/
├── docker-compose.yml    # CD 工作流自动生成
├── nginx.conf            # 从项目复制
├── ssl/                  # SSL 证书（可选）
└── .env                  # 环境变量配置
```

### 3. 环境变量（.env 文件）

创建 `/opt/library-system/.env`：

```bash
# 数据库
MYSQL_ROOT_PASSWORD=your-strong-mysql-password

# Redis
REDIS_PASSWORD=your-redis-password

# JWT
JWT_SECRET=your-jwt-secret-at-least-32-characters-long

# Grafana（可选）
GRAFANA_PASSWORD=admin
```

### 4. 初始化数据库

```bash
# 启动 MySQL 容器
docker compose up -d mysql

# 等待 MySQL 就绪
docker compose exec mysql mysqladmin ping -h localhost -u root -p${MYSQL_ROOT_PASSWORD}

# 导入初始化脚本
docker compose exec -T mysql mysql -u root -p${MYSQL_ROOT_PASSWORD} library_system < schema.sql
docker compose exec -T mysql mysql -u root -p${MYSQL_ROOT_PASSWORD} library_system < data.sql
```

## GitHub Secrets 配置

在仓库 Settings → Secrets and variables → Actions 中添加：

| Secret 名称 | 描述 | 示例 |
|-------------|------|------|
| `DEPLOY_HOST` | 服务器 IP 或域名 | `123.456.789.0` |
| `DEPLOY_USER` | SSH 登录用户 | `ubuntu` |
| `DEPLOY_SSH_KEY` | SSH 私钥内容 | `-----BEGIN OPENSSH PRIVATE KEY-----...` |
| `DEPLOY_PATH` | 部署路径（可选） | `/opt/library-system` |
| `MYSQL_ROOT_PASSWORD` | MySQL 密码 | - |
| `REDIS_PASSWORD` | Redis 密码 | - |
| `JWT_SECRET` | JWT 密钥 | - |

### SSH 密钥配置步骤

```bash
# 1. 在服务器上生成密钥对（如果没有）
ssh-keygen -t ed25519 -C "github-actions" -f ~/.ssh/github-actions

# 2. 添加公钥到 authorized_keys
cat ~/.ssh/github-actions.pub >> ~/.ssh/authorized_keys

# 3. 复制私钥内容
cat ~/.ssh/github-actions

# 4. 将私钥添加到 GitHub Secrets（DEPLOY_SSH_KEY）

# 5. 获取服务器 SSH 指纹
ssh-keyscan -H your-server-ip

# 6. 将输出添加到 GitHub Secrets（DEPLOY_KNOWN_HOSTS）
```

## 部署流程示意图

```mermaid
sequenceDiagram
    participant Dev as 开发者
    participant GH as GitHub
    participant GHA as GitHub Actions
    participant GHCR as GitHub Container Registry
    participant Server as 部署服务器

    Dev->>GH: git push main
    GH->>GHA: 触发 CD Pipeline
    GHA->>GHA: 运行测试
    GHA->>GHA: 构建 Docker 镜像
    GHA->>GHCR: 推送镜像
    GHA->>Server: SSH 连接
    Server->>Server: docker compose pull
    Server->>Server: docker compose up -d
    Server->>Server: 健康检查 (10次尝试)
    alt 健康检查通过
        Server-->>GHA: ✅ 部署成功
    else 健康检查失败
        Server->>Server: 回滚到上一版本
        Server-->>GHA: ❌ 部署失败
    end
    GHA-->>Dev: 通知部署结果
```

## 版本管理

- Docker 镜像使用 Git commit SHA 的前 8 位作为版本标签
- `:latest` 标签始终指向最新的 main 分支构建
- 回滚时可指定使用上一版本的 commit SHA

## 常见问题

### 1. 部署后服务无法访问

检查健康检查日志和 Docker 日志：

```bash
# 查看后端日志
docker compose logs backend

# 查看前端日志
docker compose logs frontend

# 检查健康检查端点
curl http://localhost:8080/api/v1/actuator/health
```

### 2. 数据库连接失败

```bash
# 检查 MySQL 是否正常运行
docker compose ps mysql

# 检查连接信息
docker compose exec backend env | grep DB

# 手动测试连接
docker compose exec mysql mysql -u root -p${MYSQL_ROOT_PASSWORD} -e "SELECT 1"
```

### 3. SSH 连接失败

```bash
# 在 GitHub Actions 运行的 Linux 环境中测试
ssh -i ~/.ssh/id_rsa -o StrictHostKeyChecking=no user@host "echo connected"
```

### 4. Docker 镜像拉取失败

```bash
# 确保服务器上有正确的 Registry 登录
echo $GITHUB_TOKEN | docker login ghcr.io -u $USERNAME --password-stdin

# 测试拉取
docker pull ghcr.io/your-org/your-repo/backend:latest
```

# 环境搭建详细指南

本文档提供图书馆管理系统V2.0的详细环境搭建指南，包括所有必需软件的安装和配置步骤。

## 版本要求

| 软件 | 最低版本 | 推荐版本 | 用途 |
|------|----------|----------|------|
| JDK | 17 | 17 LTS / 21 | 后端运行环境 |
| Node.js | 18 | 20 LTS | 前端构建工具 |
| npm | 9.0 | 10.x | Node包管理器 |
| Maven | 3.8 | 3.9.x | 后端构建工具 |
| MySQL | 8.0 | 8.0.36 | 主数据库 |
| Redis | 7.0 | 7.2.x | 缓存/Session/分布式锁 |
| Git | 2.30 | 最新版 | 版本控制 |

---

## 目录

1. [Windows环境搭建](#1-windows环境搭建)
2. [macOS环境搭建](#2-macos环境搭建)
3. [Linux环境搭建](#3-linux环境搭建)
4. [IDE配置](#4-ide配置)
5. [环境验证](#5-环境验证)

---

## 1. Windows环境搭建

### 1.1 安装JDK 17

**方式一：使用Eclipse Temurin（推荐）**

1. 访问 https://adoptium.net/temurin/releases/ 下载最新版本
2. 选择 `Windows`, `x64`, `JDK`, `17`
3. 下载 `.msi` 安装包并运行安装
4. 安装完成后，配置环境变量

**方式二：使用Oracle JDK**

1. 访问 https://www.oracle.com/java/technologies/downloads/#java17
2. 下载 Windows x64 Installer
3. 运行安装程序
4. 配置环境变量

**配置环境变量：**

```
# 新建系统变量
JAVA_HOME = C:\Program Files\Eclipse Adoptium\jdk-17.0.9.9-hotspot

# 编辑Path变量，添加
%JAVA_HOME%\bin
%JAVA_HOME%\jre\bin
```

**验证安装：**
```powershell
# 打开PowerShell，运行
java -version

# 应输出类似：
# openjdk version "17.0.9" 2023-10-17
# OpenJDK Runtime Environment (Temurin-17+9) (build 17.0.9+9)
# OpenJDK 64-Bit Server VM (Temurin-17+9)
```

### 1.2 安装Node.js 20

1. 访问 https://nodejs.org/ 下载LTS版本（20.x）
2. 下载 `.msi` 安装包并运行
3. 勾选 "Automatically install necessary tools"
4. 完成安装

**验证安装：**
```powershell
node -v
# v20.11.0

npm -v
# 10.2.4
```

**配置npm镜像（可选）：**
```powershell
# 使用淘宝镜像
npm config set registry https://registry.npmmirror.com

# 恢复官方镜像
npm config set registry https://registry.npmjs.org
```

### 1.3 安装Maven 3.9

1. 访问 https://maven.apache.org/download.cgi 下载最新版本
2. 下载 `apache-maven-3.9.x-bin.zip`
3. 解压到 `C:\Program Files\Apache\Maven`
4. 配置环境变量

**配置环境变量：**

```
# 新建系统变量
MAVEN_HOME = C:\Program Files\Apache\Maven\apache-maven-3.9.6

# 编辑Path变量，添加
%MAVEN_HOME%\bin
```

**配置Maven镜像（编辑 `%MAVEN_HOME%\conf\settings.xml`）：**

```xml
<mirrors>
  <mirror>
    <id>aliyun</id>
    <name>Aliyun Maven</name>
    <url>https://maven.aliyun.com/repository/public</url>
    <mirrorOf>central</mirrorOf>
  </mirror>
</mirrors>
```

**验证安装：**
```powershell
mvn -version

# 应输出类似：
# Apache Maven 3.9.6 (...")
# Maven home: C:\Program Files\Apache\Maven\apache-maven-3.9.6
# Java version: 17.0.9, vendor: Eclipse Adoptium
```

### 1.4 安装MySQL 8.0

**方式一：使用MySQL Installer**

1. 访问 https://dev.mysql.com/downloads/installer/ 下载MySQL Installer
2. 运行安装程序
3. 选择 "Full" 安装类型
4. 配置 root 密码（记住此密码）
5. 完成安装

**方式二：使用Docker**

```powershell
# 拉取MySQL镜像
docker pull mysql:8.0

# 创建并启动容器
docker run -d `
  --name library-mysql `
  -p 3306:3306 `
  -e MYSQL_ROOT_PASSWORD=YourStrongPassword123! `
  -v mysql_data:/var/lib/mysql `
  mysql:8.0 `
  --character-set-server=utf8mb4 `
  --collation-server=utf8mb4_unicode_ci
```

**创建数据库：**

```sql
-- 登录MySQL
mysql -u root -p

-- 创建数据库
CREATE DATABASE library_system 
  CHARACTER SET utf8mb4 
  COLLATE utf8mb4_unicode_ci;

-- 创建用户（可选，更安全）
CREATE USER 'library'@'localhost' IDENTIFIED BY 'LibraryPass123!';
GRANT ALL PRIVILEGES ON library_system.* TO 'library'@'localhost';
FLUSH PRIVILEGES;
```

**验证安装：**
```powershell
mysql -u root -p -e "SELECT VERSION();"
```

### 1.5 安装Redis

**方式一：使用Docker（推荐）**

```powershell
# 拉取Redis镜像
docker pull redis:7-alpine

# 创建并启动容器
docker run -d `
  --name library-redis `
  -p 6379:6379 `
  -v redis_data:/data `
  redis:7-alpine `
  redis-server --appendonly yes
```

**方式二：使用Memurai（Windows原生Redis）**

1. 访问 https://www.memurai.com/ 下载Memurai
2. 安装并启动服务
3. Memurai兼容Redis协议，可直接使用

**验证安装：**
```powershell
# 使用redis-cli
redis-cli ping
# 应输出：PONG
```

### 1.6 安装Git

1. 访问 https://git-scm.com/download/win 下载
2. 运行安装程序
3. 建议勾选：
   - "Git Bash Here"
   - "Git GUI Here"
   - "Use Vim as Git's editor"
   - "Use OpenSSH"
   - "Use MinTTY"
4. 完成安装

**验证安装：**
```powershell
git --version
# git version 2.43.0.windows.1
```

---

## 2. macOS环境搭建

### 2.1 安装Homebrew（如果未安装）

```bash
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
```

### 2.2 使用Homebrew安装软件

```bash
# 安装JDK 17
brew install openjdk@17
# 链接JDK
sudo ln -sfn $(brew --prefix)/opt/openjdk@17/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-17.jdk

# 安装Node.js 20
brew install node@20

# 安装Maven
brew install maven

# 安装MySQL
brew install mysql

# 安装Redis
brew install redis

# 安装Git
brew install git
```

### 2.3 配置环境变量（编辑 ~/.zshrc 或 ~/.bash_profile）

```bash
# Java
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
export PATH=$JAVA_HOME/bin:$PATH

# Maven
export MAVEN_HOME=/opt/homebrew/opt/maven
export PATH=$MAVEN_HOME/bin:$PATH

# Node
export PATH="/opt/homebrew/opt/node@20/bin:$PATH"
```

### 2.4 启动服务

```bash
# 启动MySQL
brew services start mysql

# 启动Redis
brew services start redis

# 创建数据库
mysql -u root
CREATE DATABASE library_system CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

---

## 3. Linux环境搭建

### 3.1 Ubuntu/Debian

```bash
# 更新系统
sudo apt update && sudo apt upgrade -y

# 安装JDK 17
sudo apt install -y openjdk-17-jdk

# 安装Node.js 20
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt install -y nodejs

# 安装Maven
sudo apt install -y maven

# 安装MySQL
sudo apt install -y mysql-server
sudo systemctl start mysql
sudo systemctl enable mysql

# 安装Redis
sudo apt install -y redis-server
sudo systemctl start redis
sudo systemctl enable redis

# 安装Git
sudo apt install -y git
```

### 3.2 CentOS/RHEL

```bash
# 安装JDK 17
sudo yum install -y java-17-openjdk java-17-openjdk-devel

# 安装Node.js 20
curl -fsSL https://rpm.nodesource.com/setup_20.x | sudo bash -
sudo yum install -y nodejs

# 安装Maven
sudo yum install -y maven

# 安装MySQL 8
sudo yum localinstall https://dev.mysql.com/get/mysql80-community-release-el8-7.noarch.rpm
sudo yum install -y mysql-community-server
sudo systemctl start mysqld
sudo systemctl enable mysqld

# 安装Redis
sudo yum install -y redis
sudo systemctl start redis
sudo systemctl enable redis

# 安装Git
sudo yum install -y git
```

### 3.3 配置MySQL

```bash
# 获取临时密码
sudo grep 'temporary password' /var/log/mysqld.log

# 登录MySQL
mysql -u root -p

# 创建数据库
CREATE DATABASE library_system CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

---

## 4. IDE配置

### 4.1 IntelliJ IDEA配置

**Java项目配置：**

1. File → Project Structure → Project
   - SDK: 选择 JDK 17
   - Language level: 17

2. File → Settings → Build, Execution, Deployment → Build Tools → Maven
   - Maven home: 选择 Maven 安装目录
   - settings.xml: 选择配置文件

3. File → Settings → Build, Execution, Deployment → Build Tools → Maven → Runner
   - VM Options: `-Dfile.encoding=UTF-8`

4. 安装插件：
   - Lombok
   - MyBatisX

**前端项目配置（使用WebStorm或IDEA）：**

1. File → Settings → Languages & Frameworks → JavaScript
   - ECMAScript 6+

2. File → Settings → Node.js
   - Node interpreter: 选择 Node 路径

### 4.2 VS Code配置

**推荐扩展：**

```json
{
  "recommendations": [
    "Vue.volar",
    "Vue.vscode-typescript-vue-plugin",
    "dbaeumer.vscode-eslint",
    "esbenp.prettier-vscode",
    "ms-vscode.vscode-typescript-next",
    "formulahendry.auto-rename-tag",
    "christian-kohler.path-intellisense",
    "ecmel.vscode-html-css",
    "mikestead.dotenv"
  ]
}
```

**settings.json配置：**

```json
{
  "editor.formatOnSave": true,
  "editor.defaultFormatter": "esbenp.prettier-vscode",
  "[vue]": {
    "editor.defaultFormatter": "Vue.volar"
  },
  "[javascript]": {
    "editor.defaultFormatter": "esbenp.prettier-vscode"
  },
  "vetur.format.defaultFormatterOptions": {
    "prettier": {
      "semi": false,
      "singleQuote": true
    }
  }
}
```

---

## 5. 环境验证

### 5.1 验证开发环境

创建验证脚本 `verify-env.sh`：

```bash
#!/bin/bash

echo "===== 环境验证 ====="
echo ""

# JDK
echo "1. JDK 版本："
java -version 2>&1 | head -1
echo ""

# Node.js
echo "2. Node.js 版本："
node -v
echo "   npm 版本：$(npm -v)"
echo ""

# Maven
echo "3. Maven 版本："
mvn -v | head -1
echo ""

# Git
echo "4. Git 版本："
git --version
echo ""

# MySQL
echo "5. MySQL 连接："
mysql -u root -p -e "SELECT 'MySQL OK' AS status;" 2>/dev/null || echo "   MySQL 未运行或未配置"
echo ""

# Redis
echo "6. Redis 连接："
redis-cli ping 2>/dev/null || echo "   Redis 未运行"
echo ""

echo "===== 验证完成 ====="
```

### 5.2 验证项目编译

```bash
# 克隆项目
git clone <repository_url>
cd library-system-v2

# 验证后端编译
cd backend
mvn clean compile
cd ..

# 验证前端依赖
cd frontend
npm install
cd ..
```

### 5.3 启动服务

```bash
# 终端1: 启动MySQL（如果使用本地服务）
# MySQL会自动启动

# 终端1: 启动Redis（如果使用本地服务）
redis-server

# 终端2: 启动后端
cd backend
mvn spring-boot:run

# 终端3: 启动前端
cd frontend
npm run dev
```

### 5.4 访问验证

- 前端地址：http://localhost:5173
- 后端API：http://localhost:8080/api
- Swagger UI：http://localhost:8080/api/swagger-ui.html
- 默认管理员账户：admin / admin123

---

## 常见问题

### Q1: Maven下载依赖很慢

**解决方案：**
1. 配置国内镜像（见上文Maven配置）
2. 使用阿里云仓库
3. 首次编译耐心等待

### Q2: MySQL连接被拒绝

**解决方案：**
1. 检查MySQL服务是否启动
2. 检查用户名密码是否正确
3. 检查3306端口是否被占用

### Q3: Redis连接失败

**解决方案：**
1. 检查Redis服务是否启动
2. 检查6379端口是否开放
3. 验证密码配置（如有）

### Q4: 前端npm install失败

**解决方案：**
1. 清理npm缓存：`npm cache clean --force`
2. 删除node_modules和package-lock.json后重新安装
3. 使用国内镜像

### Q5: 端口冲突

**解决方案：**
```bash
# Windows查看端口占用
netstat -ano | findstr :8080

# Linux/macOS查看端口占用
lsof -i :8080

# 结束占用进程或修改配置使用其他端口
```

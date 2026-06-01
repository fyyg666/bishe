---
name: comprehensive-bug-fix
overview: 基于全面代码审查报告的20个缺陷，按 Critical → High → Medium → Low 优先级对项目进行完整修复，覆盖后端Java、前端Vue/JS、Nginx/Docker配置，确保所有问题被彻底解决。
todos:
  - id: batch1-critical
    content: 批次1 Critical修复：修改 BookServiceImpl.java 3处 @CacheEvict 注解 + 删除 nginx.conf return 301
    status: completed
  - id: batch2-high
    content: 批次2 High修复：修改 user.js logout/登录, AuthServiceImpl register验证码, router BorrowDetail认证, Constants默认密码随机化, auth.js refreshToken持久化
    status: completed
    dependencies:
      - batch1-critical
  - id: batch3-medium
    content: 批次3 Medium修复：修改 XssFilter, SecurityConfig, RateLimitFilter(2处), BookServiceImpl(importBooks2处), 4个前端API params→data, docker-compose Redis绑定
    status: completed
    dependencies:
      - batch2-high
  - id: batch4-low
    content: 批次4 Low修复：修改 user.js 登录catch/公开fetchingUserInfo, SecurityConfig HSTS条件化, CaffeineConfig 常量提取
    status: completed
    dependencies:
      - batch3-medium
---

## 修复目标

对 comprehensive-code-review-2026-06-01.md 报告中发现的全部 20 个问题进行彻底修复，覆盖：

- 后端 Java：BookServiceImpl.java, AuthServiceImpl.java, Constants.java, ReaderServiceImpl.java, SecurityConfig.java, RateLimitFilter.java, XssFilter.java, CaffeineConfig.java
- 前端 Vue/JS：stores/user.js, utils/auth.js, router/index.js, api/auth.js, api/borrow.js, api/compensation.js, api/volunteer.js, api/suggestion.js
- 基础设施配置：nginx/nginx.conf, docker-compose.yml

## 问题分级修复清单

### 批次1: Critical（2项，立即修复）

1. BookServiceImpl `@CacheEvict(beforeInvocation=true)` 缓存注解失效与缓存不一致
2. Nginx `return 301` 导致所有 location 块成为死代码

### 批次2: High（5项，紧急修复）

3. stores/user.js logout() 未 await apiLogout()，Promise rejection 未被捕获
4. AuthServiceImpl.register() 未调用验证码校验，可被批量注册攻击
5. router/index.js BorrowDetail 路由 meta 缺少 requiresAuth
6. Constants.Security.DEFAULT_PASSWORD="123456" 弱密码硬编码
7. utils/auth.js refreshToken 无持久化，页面刷新后丢失

### 批次3: Medium（8项，近期修复）

8. XssFilter 使用 ContentCachingResponseWrapper 可能截断响应体
9. SecurityConfig 使用已废弃的 sessionFixation().none()
10. RateLimitFilter login/register 降级路径提前释放 Semaphore
11. getClientIp() 取 X-Forwarded-For 链末尾IP而非客户端真实IP
12.多个 POST API 将数据放在 params 而非 data（4个文件）
12. BookServiceImpl.importBooks 逐条 INSERT 性能差
13. importBooks 异常 e.getMessage() 直接暴露给前端
14. docker-compose.yml Redis 端口未绑定 127.0.0.1

### 批次4: Low（5项，技术债务）

16. stores/user.js login() 无意义 try/catch 重抛
17. stores/user.js fetchingUserInfo 未公开暴露
18. SecurityConfig HSTS 头对所有环境生效
19. CaffeineConfig 三个 Bean 重复缓存名定义
20. JwtFilter PATH_MATCHER 声明为 static（非阻塞，跳过）

## 技术方案

### 修复策略

按优先级分 4 批次依次执行，每批次内按依赖关系排序，确保每步修改后代码可编译运行。

### 批次1: Critical 修复

#### 1.1 BookServiceImpl @CacheEvict 修复

- **文件**: `backend/src/main/java/com/library/system/service/impl/BookServiceImpl.java`
- **修改位置**:
- L96: `@CacheEvict(key = "#result.id", beforeInvocation = true)` → `@CacheEvict(key = "#request.isbn")`
- L135: `@CacheEvict(key = "#id", beforeInvocation = true)` → `@CacheEvict(key = "#id")`
- L176: `@CacheEvict(key = "#id", beforeInvocation = true)` → `@CacheEvict(key = "#id")`
- **根因**: `beforeInvocation=true` 使 createBook 的 `#result.id` 在方法体执行前为 null，SPEL 无法解析；updateBook/deleteBook 即使事务回滚缓存也已先被清除
- **验证**: 创建/更新/删除图书后检查对应缓存 key 是否正确驱逐

#### 1.2 Nginx 配置修复

- **文件**: `nginx/nginx.conf`
- **修改**: 删除 HTTP server 块 L8 的 `return 301 https://$server_name$request_uri;`
- **根因**: server 级别 return 导致其后所有 location{} 永不执行
- **验证**: curl HTTP 端口确认能正常返回静态页和代理 API

### 批次2: High 修复

#### 2.1 user.js logout() async/await

- **文件**: `frontend/src/stores/user.js`
- **修改**: L55 `function logout()` → `async function logout()`，L57 `apiLogout()` → `await apiLogout()`
- **验证**: 登出时浏览器 console 无未处理 Promise rejection

#### 2.2 AuthServiceImpl.register() 验证码

- **文件**: `backend/src/main/java/com/library/system/service/impl/AuthServiceImpl.java`
- **修改**: register() 方法 L104 之前插入 `validateCaptcha(request.getCaptchaKey(), request.getCaptchaCode());`
- **验证**: 不带验证码的注册请求返回 400 错误

#### 2.3 BorrowDetail 路由认证

- **文件**: `frontend/src/router/index.js`
- **修改**: L80 meta 增加 `requiresAuth: true`
- **验证**: 未登录访问 `/borrows/1` 应重定向到 `/login`

#### 2.4 默认密码随机化

- **文件**:
- `backend/src/main/java/com/library/system/common/Constants.java` Security 类新增 `generateDefaultPassword()` 静态方法
- `backend/src/main/java/com/library/system/service/impl/ReaderServiceImpl.java` L284 改用 `Constants.Security.generateDefaultPassword()`
- **验证**: resetPassword 后生成的密码为 12 位随机字符，非固定 `123456`

#### 2.5 refreshToken 持久化

- **文件**:
- `frontend/src/utils/auth.js`: 新增 `REFRESH_TOKEN_KEY` 常量，`setToken()`/`clearToken()` 中读写 sessionStorage
- `frontend/src/api/auth.js` L26-33: refreshToken() 添加 null 检查
- **验证**: 登录后刷新页面，refreshToken 可从 sessionStorage 恢复

### 批次3: Medium 修复

#### 3.1 XssFilter 移除 ContentCachingResponseWrapper

- **文件**: `backend/src/main/java/com/library/system/filter/XssFilter.java`
- **修改**: L57-65，移除 `ContentCachingResponseWrapper` 包装，直接使用原始 response
- **验证**: 文件下载接口正常返回完整数据

#### 3.2 SecurityConfig 移除废弃 API

- **文件**: `backend/src/main/java/com/library/system/config/SecurityConfig.java`
- **修改**: L136 删除 `.sessionFixation().none()`
- **验证**: 应用正常启动无警告

#### 3.3 RateLimitFilter semaphore 修复

- **文件**: `backend/src/main/java/com/library/system/filter/RateLimitFilter.java`
- **修改**: 删除 L326 和 L393 的 `localRateLimiter.release();`
- **验证**: Redis 不可用时限流降级正常，无 semaphore 泄漏

#### 3.4 getClientIp() IP 顺序修复

- **文件**: `backend/src/main/java/com/library/system/filter/RateLimitFilter.java`
- **修改**: L486 `ips[ips.length - 1].trim()` → `ips[0].trim()`
- **验证**: 限流基于真实客户端 IP 而非代理 IP

#### 3.5 POST params → data

- **文件**: 
- `frontend/src/api/borrow.js` L58
- `frontend/src/api/compensation.js` L30, L38, L46, L54
- `frontend/src/api/volunteer.js` L84
- `frontend/src/api/suggestion.js` (approveSuggestion/rejectSuggestion 的 params)
- **修改**: 将 `params:` 改为 `data:`
- **验证**: 各 POST 请求体数据正确发送到后端

#### 3.6 importBooks 批量插入

- **文件**: `backend/src/main/java/com/library/system/service/impl/BookServiceImpl.java`
- **修改**: L256-301 循环内收集 Book 到 List，每 100 条批量 insert
- **验证**: 导入 1000 条图书耗时显著减少

#### 3.7 importBooks 异常脱敏

- **文件**: `backend/src/main/java/com/library/system/service/impl/BookServiceImpl.java`
- **修改**: L298 `e.getMessage()` → 友好提示，详细错误写 log
- **验证**: 导入失败时前端不显示数据库内部错误信息

#### 3.8 Docker Redis 端口绑定

- **文件**: `docker-compose.yml`
- **修改**: L29 `"6379:6379"` → `"127.0.0.1:6379:6379"`
- **验证**: 外部网络无法直接访问 Redis

### 批次4: Low 修复

#### 4.1 user.js login() catch 清理

- **文件**: `frontend/src/stores/user.js`
- **修改**: L38-52 移除 try/catch，直接执行逻辑

#### 4.2 user.js fetchingUserInfo 公开

- **文件**: `frontend/src/stores/user.js` + `frontend/src/router/index.js`
- **修改**: store 中新增 `const fetchingUserInfo = ref(false)`，fetchUserInfo 中加锁；return 暴露 fetchingUserInfo；router 使用公开属性

#### 4.3 SecurityConfig HSTS 条件化

- **文件**: `backend/src/main/java/com/library/system/config/SecurityConfig.java`
- **修改**: L123 用 `@Value("${server.ssl.enabled:false}")` 控制是否添加 HSTS

#### 4.4 CaffeineConfig 提取常量

- **文件**: `backend/src/main/java/com/library/system/config/CaffeineConfig.java`
- **修改**: 提取 `DEFAULT_CACHE_NAMES` 静态数组，三个 Bean 方法共用

### 修复顺序

```
批次1 → 批次2 → (验证) → 批次3 → (验证) → 批次4 → (最终验证)
```

每批次内先修改后端，再修改前端，最后修改配置文件。
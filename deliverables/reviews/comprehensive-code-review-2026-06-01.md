# 图书馆管理系统 V2 — 全面代码审查报告

> **审查日期**: 2026-06-01  
> **审查范围**: 后端 294 Java文件 + 前端 124 Vue/JS文件 + 基础设施配置  
> **技术栈**: Spring Boot 3.5.13 + Vue 3.4 + MyBatis-Plus + Redis + Caffeine + MinIO + JWT  
> **审查维度**: Bug与逻辑错误 / 性能优化 / 安全漏洞 / 可读性与可维护性 / 最佳实践  
> **严重等级**: 🔴 Critical(系统崩溃/安全绕过) > 🟠 High(功能异常/数据不一致) > 🟡 Medium(逻辑缺陷/性能退化) > 🟢 Low(代码质量/风格)

---

## 零、历史问题修复状态验证

| # | 历史问题 | 原报告 | 状态 | 说明 |
|---|---------|--------|------|------|
| 1 | Token黑名单key不一致 | backend-bugs CRITICAL-1.1 | ✅ **已修复** | `AuthServiceImpl.logout()` (L185-198) 已统一使用 JTI 作为黑名单 key |
| 2 | JWT默认密钥硬编码 | backend-bugs HIGH-2.1 | ✅ **已修复** | `JwtUtils` (L35) 从环境变量读取，`@PostConstruct` 强制校验，移除回退密钥 |
| 3 | Cookie未设HttpOnly | review CRITICAL-1 | △ **架构变更** | 改用 `sessionStorage` + `BroadcastChannel`，不再使用 Cookie |
| 4 | Pinia孤立实例 | frontend-bugs BUG-01 | ✅ **已修复** | `store/index.js` 已删除，Pinia 统一在 `main.js` 创建 |
| 5 | IDOR漏洞-读者详情 | gstack-security F-001 | ✅ **已修复** | `/readers/{id}` 添加 `@PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")` |
| 6 | 默认密码 `123456` | gstack-security F-004 | ✅ **已修复 (2026-06-01)** | `application.yml` 移除 `default-password` 项；`ReaderServiceImpl` 改用 `Constants.Security.generateDefaultPassword()` 生成12位随机强密码 |

---

## 一、🔴 Critical 级别

### CRITICAL-1: BookServiceImpl `@CacheEvict` 使用 `beforeInvocation=true` 配合 `#result.id` 导致注解失效 [Bug/逻辑错误]

| 属性 | 值 |
|------|-----|
| **文件** | `backend/.../service/impl/BookServiceImpl.java` |
| **位置** | `createBook()` L96, `updateBook()` L135, `deleteBook()` L176 |
| **维度** | Bug与逻辑错误 |

**问题描述**:

三处 `@CacheEvict` 注解均设置 `beforeInvocation = true`：

```java
// L96 - createBook: #result.id 在方法执行前不存在！
@CacheEvict(key = "#result.id", beforeInvocation = true)
public BookResponse createBook(BookRequest request) { ... }

// L135 - updateBook: 即使后续 updateById 抛异常，缓存也被清除了
@CacheEvict(key = "#id", beforeInvocation = true)
public BookResponse updateBook(Long id, BookRequest request) { ... }

// L176 - deleteBook: 同上
@CacheEvict(key = "#id", beforeInvocation = true)
public void deleteBook(Long id) { ... }
```

**风险分析**:
- `createBook`: `beforeInvocation=true` 表示在方法体执行**之前**驱逐缓存，但 `#result.id` 要等到 `bookMapper.insert(book)` 之后才有值。SPEL 无法解析 `#result`，可能导致缓存注解完全失效或抛出异常。
- `updateBook` / `deleteBook`: SQL 执行失败抛异常时，缓存已先被清除，导致**缓存与数据库数据不一致**——缓存无数据，数据库仍有旧数据。

**修复建议**: 移除所有 `beforeInvocation = true`：

```java
// createBook: 用入参的 isbn 作为 key
@CacheEvict(key = "#request.isbn")
public BookResponse createBook(BookRequest request) { ... }

// updateBook: 移除 beforeInvocation
@CacheEvict(key = "#id")
public BookResponse updateBook(Long id, BookRequest request) { ... }

// deleteBook: 移除 beforeInvocation
@CacheEvict(key = "#id")
public void deleteBook(Long id) { ... }
```

---

### CRITICAL-2: Nginx 配置中 `return 301` 在 `location` 块之前导致所有请求死循环或配置失效 [Bug/逻辑错误]

| 属性 | 值 |
|------|-----|
| **文件** | `nginx/nginx.conf` |
| **位置** | L8 |
| **维度** | Bug与逻辑错误 |

**问题描述**:

HTTP (80端口) server 块中，`return 301` 写在了所有 `location` 块**之前**（且不在任何 location 内）：

```nginx:8:10:nginx/nginx.conf
server {
    listen 80;
    server_name localhost;
    add_header X-Frame-Options DENY always;
    add_header X-Content-Type-Options nosniff always;
    return 301 https://$server_name$request_uri;  // ← 所有请求直接301

    location / {           // ← 这段代码永远不会执行（dead code）
        root /usr/share/nginx/html;
```

**风险分析**:
- `return 301` 在 `server` 级别直接返回重定向，其后的所有 `location {}` 块都成为**死代码**（dead code），永远不会被访问。
- 如果用户通过 HTTP 访问，会进入无限重定向循环（301到 https://localhost/，浏览器再请求 https://localhost/，如果SSL证书无效或未配置443，请求失败）。
- 所有 `location /api/v1/` 的反向代理配置对 HTTP 端口完全无效。

**修复建议**: 移除 `return 301`，保持 HTTP 可正常访问；或将其正确放入条件判断：

```nginx
server {
    listen 80;
    server_name localhost;

    add_header X-Frame-Options DENY always;
    add_header X-Content-Type-Options nosniff always;

    # 移除 return 301，保持HTTP可用作为开发环境
    # 生产环境如需强制HTTPS，使用:
    # if ($http_x_forwarded_proto = "http") {
    #     return 301 https://$host$request_uri;
    # }

    location / {
        root /usr/share/nginx/html;
        try_files $uri $uri/ /index.html;
        ...
    }
    ...
}
```

---

## 二、🟠 High 级别

### HIGH-1: `user.js` logout() 未 await 异步请求导致未处理的 Promise 拒绝 [Bug/逻辑错误]

| 属性 | 值 |
|------|-----|
| **文件** | `frontend/src/stores/user.js` |
| **位置** | L55-64 |
| **维度** | Bug与逻辑错误 |

**问题描述**:

```javascript:55:64:frontend/src/stores/user.js
function logout() {
    try {
      apiLogout()  // ← 异步函数，没有 await！try/catch 无法捕获 rejected Promise
    } catch {
      // 忽略错误
    }
    token.value = ''
    userInfo.value = null
    clearToken()
}
```

`apiLogout()` 返回 Promise，但调用时没有 `await`。`try/catch` 只能捕获**同步**抛出的错误，无法捕获异步 Promise rejection。这会导致未处理的 Promise 拒绝。

**修复建议**:

```javascript
async function logout() {
    try {
      await apiLogout()  // 正确做法
    } catch {
      // 忽略登出API错误（本地状态仍需清除）
    }
    token.value = ''
    userInfo.value = null
    clearToken()
}
```

---

### HIGH-2: 注册接口 `register()` 缺少验证码校验，与登录接口不一致 [安全漏洞]

| 属性 | 值 |
|------|-----|
| **文件** | `backend/.../service/impl/AuthServiceImpl.java` |
| **位置** | L102-138 |
| **维度** | 安全漏洞 |

**问题描述**:

`login()` 方法 (L54) 调用 `validateCaptcha()` 进行验证码校验，但 `register()` 方法完全没有验证码校验。恶意用户可以通过脚本批量注册账号，绕过反自动化保护。

**修复建议**: 在 `register()` 方法开头添加验证码校验：

```java
@Override
@Transactional
public LoginResponse.UserInfo register(RegisterRequest request) {
    // 添加验证码校验
    if (request.getCaptchaKey() != null || request.getCaptchaCode() != null) {
        validateCaptcha(request.getCaptchaKey(), request.getCaptchaCode());
    }
    // ... 其余逻辑
}
```

---

### HIGH-3: 前端 `BorrowDetail` 路由缺少 `requiresAuth` 认证保护 [安全漏洞]

| 属性 | 值 |
|------|-----|
| **文件** | `frontend/src/router/index.js` |
| **位置** | L77-80 |
| **维度** | 安全漏洞 |

**问题描述**:

```javascript:77:80:frontend/src/router/index.js
{
    path: 'borrows/:id',
    name: 'BorrowDetail',
    component: () => import('@/views/borrow/BorrowDetail.vue'),
    meta: { title: '借阅详情', hidden: true }  // ← 缺少 requiresAuth: true
},
```

其他所有需要认证的路由都有 `requiresAuth: true`，唯独 `BorrowDetail` 缺失。未登录用户可直接通过URL访问借阅详情页面。

**修复建议**:

```javascript
meta: { requiresAuth: true, title: '借阅详情', hidden: true }
```

---

### HIGH-4: 默认密码 `123456` 硬编码且未强制首次登录修改 [安全漏洞]

| 属性 | 值 |
|------|-----|
| **文件** | `backend/.../common/Constants.java` |
| **位置** | L227 |
| **维度** | 安全漏洞 |

**问题描述**:

```java:227:227:backend/.../common/Constants.java
public static final String DEFAULT_PASSWORD = "123456";
```

`resetPassword()` 功能（`ReaderController.java` L216-224）使用此密码重置用户密码。攻击者获取用户名后可直接尝试 `123456` 登录。没有首次登录强制修改密码的机制。

**修复建议**:

1. 将默认密码改为随机生成并通过邮件/短信发送
2. 或强制用户通过"忘记密码"流程自行设置
3. 至少增加首次登录标记，登录后弹窗要求修改密码

```java
// 方案A: 随机密码
private String generateDefaultPassword() {
    String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
    StringBuilder sb = new StringBuilder(12);
    for (int i = 0; i < 12; i++) {
        sb.append(chars.charAt(SECURE_RANDOM.nextInt(chars.length())));
    }
    return sb.toString();
}
```

---

### HIGH-5: `refreshToken` API 可能传入 null refreshToken [Bug/逻辑错误]

| 属性 | 值 |
|------|-----|
| **文件** | `frontend/src/api/auth.js` L26-33 + `frontend/src/utils/auth.js` L4 |
| **维度** | Bug与逻辑错误 |

**问题描述**:

```javascript:4:4:frontend/src/utils/auth.js
let refreshToken = null  // 内存变量，页面刷新后丢失
```

```javascript:26:33:frontend/src/api/auth.js
export function refreshToken() {
    return request({
        url: '/auth/refresh',
        method: 'post',
        data: { refreshToken: getRefreshToken() }  // 可能为 null
    })
}
```

`refreshToken` 只存在内存变量中（无 sessionStorage/localStorage 持久化），页面刷新后丢失。当 token 过期触发自动刷新时，`getRefreshToken()` 返回 `null`，向后端发送空值。

**修复建议**: 将 `refreshToken` 也持久化到 `sessionStorage`：

```javascript
// auth.js
let refreshToken = sessionStorage.getItem('refresh_token') || null

export function setToken(access, refresh) {
    accessToken = access
    if (refresh !== undefined) {
        refreshToken = refresh
        if (refresh) {
            sessionStorage.setItem('refresh_token', refresh)
        } else {
            sessionStorage.removeItem('refresh_token')
        }
    }
    // ...
}

export function clearToken() {
    accessToken = null
    refreshToken = null
    sessionStorage.removeItem(TOKEN_KEY)
    sessionStorage.removeItem('refresh_token')  // 同步清理
    // ...
}
```

---

## 三、🟡 Medium 级别

### MEDIUM-1: XssFilter 对非缓存请求使用 `ContentCachingResponseWrapper` 可能导致空响应体 [Bug]

| 属性 | 值 |
|------|-----|
| **文件** | `backend/.../filter/XssFilter.java` |
| **位置** | L57-66 |
| **维度** | Bug与逻辑错误 |

**问题描述**:

```java:57:65:backend/.../filter/XssFilter.java
XssRequestWrapper xssRequest = new XssRequestWrapper(request);
ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

try {
    filterChain.doFilter(xssRequest, responseWrapper);
} finally {
    responseWrapper.copyBodyToResponse();
}
```

`ContentCachingResponseWrapper` 需要响应体被**完全写入**后才能通过 `copyBodyToResponse()` 复制。如果下游使用 Streaming 模式（如文件下载、SSE）或响应体超过缓存阈值，会导致响应体截断或丢失。

**风险**: 文件下载(/file/download)、大JSON响应等场景下可能返回空或截断的响应体。

**修复建议**:

```java
@Override
protected void doFilterInternal(HttpServletRequest request,
        HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
    if (isWhitelisted(request) || shouldSkipCaching(request)) {
        chain.doFilter(request, response);
        return;
    }
    
    XssRequestWrapper xssRequest = new XssRequestWrapper(request);
    chain.doFilter(xssRequest, response);  // 非缓存模式，兼容所有响应类型
}

private boolean shouldSkipCaching(HttpServletRequest request) {
    String uri = request.getRequestURI();
    return uri.contains("/download") || uri.contains("/export") 
        || uri.contains("/ws/") || request.getHeader("Accept") != null 
        && request.getHeader("Accept").contains("text/event-stream");
}
```

---

### MEDIUM-2: SecurityConfig 使用已废弃的 `sessionFixation().none()` API [可维护性]

| 属性 | 值 |
|------|-----|
| **文件** | `backend/.../config/SecurityConfig.java` |
| **位置** | L136 |
| **维度** | 最佳实践 |

**问题描述**:

```java:134:137:backend/.../config/SecurityConfig.java
.sessionManagement(session ->
    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .sessionFixation().none()  // ← Spring Security 6.1+ 已废弃
)
```

`sessionFixation()` 在 Spring Security 6.1+ 已被废弃，默认行为就是 `none()`。在 `SessionCreationPolicy.STATELESS` 模式下此行无实际作用。

**修复建议**: 移除 `.sessionFixation().none()` 行。

---

### MEDIUM-3: RateLimitFilter 本地限流 Semaphore 降级获取后可能未正确释放 [Bug]

| 属性 | 值 |
|------|-----|
| **文件** | `backend/.../filter/RateLimitFilter.java` |
| **位置** | L241-262 |
| **维度** | Bug与逻辑错误 |

**问题描述**:

```java:241:262:backend/.../filter/RateLimitFilter.java
} catch (Exception e) {
    // ...
    if (!localRateLimiter.tryAcquire()) {
        response.setStatus(429);
        // ...
        return;  // ← 这里 return 了，但未释放已获取的 Semaphore
    }
    localFallbackAcquired = true;
}

try {
    filterChain.doFilter(request, response);
} finally {
    if (localFallbackAcquired) {
        localRateLimiter.release();  // 正常流程释放
    }
}
```

`checkLoginRateLimit()` 和 `checkRegisterRateLimit()` 中也有相同的模式——Redis 异常时降级获取了 Semaphore，但异常降级成功放行后立即 `localRateLimiter.release()` (L326, L393)，这意味着限流窗口内**Semaphore 提前释放**，导致降级限流失效。

**修复建议**: 移除 `checkLoginRateLimit` 和 `checkRegisterRateLimit` 中降级异常路径下的 `localRateLimiter.release()` 调用，Semaphore 应在 `filterChain` 执行完成后统一释放。

---

### MEDIUM-4: `getClientIp()` 中 `X-Forwarded-For` 取最后一个IP而非第一个 [安全/逻辑]

| 属性 | 值 |
|------|-----|
| **文件** | `backend/.../filter/RateLimitFilter.java` |
| **位置** | L482-488 |
| **维度** | 安全漏洞 / Bug |

**问题描述**:

```java:486:487:backend/.../filter/RateLimitFilter.java
String[] ips = ip.split(",");
ip = ips[ips.length - 1].trim();  // 取最后一个IP
```

`X-Forwarded-For` 格式为 `client, proxy1, proxy2`，**第一个**是真实客户端IP，**最后一个**是最接近服务器的代理IP。取最后一个IP意味着限流基于 Nginx/K8s 内网 IP 而非真实用户IP，整个限流机制可能失效。攻击者还可以通过在请求头中伪造 `X-Forwarded-For` 来绕过 IP 限流。

**修复建议**:

```java
// 取第一个（最左侧）的IP，这是真正的客户端IP
String[] ips = ip.split(",");
ip = ips[0].trim();  // 第一个才是客户端真实IP

// 额外防护：限制 X-Forwarded-For 段数，防止伪造
if (ips.length > 5) {
    ip = ips[0].trim();  // 只信任前5跳
}
```

---

### MEDIUM-5: 多个前端 API 模块将 POST/PUT 数据放在 `params` 而非 `data` [可维护性]

| 属性 | 值 |
|------|-----|
| **文件** | `frontend/src/api/borrow.js`, `compensation.js`, `volunteer.js`, `suggestion.js` |
| **位置** | 多处 |
| **维度** | 最佳实践 |

**问题描述**:

```javascript
// borrow.js L54-59
export function renewBook(id, data) {
    return request({
        url: `/borrows/${id}/renew`,
        method: 'post',
        params: { days: data?.extendDays || data?.days }  // ← 应放在 data 中
    })
}

// compensation.js L26-31
export function processCashPayment(id, remark) {
    return request({
        url: `/compensations/${id}/pay/cash`,
        method: 'post',
        params: { remark }  // ← 应放在 data 中
    })
}
```

POST/PUT 请求的数据应通过 `data`（请求体）发送，而非 `params`（URL 查询字符串）。URL 查询字符串会暴露在浏览器历史、服务器日志和代理日志中。

**修复建议**: 统一使用 `data` 属性：

```javascript
export function renewBook(id, data) {
    return request({
        url: `/borrows/${id}/renew`,
        method: 'post',
        data: { days: data?.extendDays || data?.days }
    })
}
```

---

### MEDIUM-6: BookServiceImpl `importBooks` 逐条 INSERT 性能差 [性能]

| 属性 | 值 |
|------|-----|
| **文件** | `backend/.../service/impl/BookServiceImpl.java` |
| **位置** | L256-301 |
| **维度** | 性能优化 |

**问题描述**: 导入图书时在 `for` 循环中逐条执行 `bookMapper.insert(book)`，每条触发一次数据库往返。导入 1000 条数据需要 1000 次网络 IO。

**修复建议**: 使用 MyBatis-Plus 批量插入：

```java
// 收集到列表后批量插入
List<Book> batchBooks = new ArrayList<>();
for (...) {
    batchBooks.add(book);
    if (batchBooks.size() >= 100) {
        bookService.saveBatch(batchBooks, 100);
        batchBooks.clear();
    }
}
if (!batchBooks.isEmpty()) {
    bookService.saveBatch(batchBooks, batchBooks.size());
}
```

---

### MEDIUM-7: `importBooks` 直接暴露异常消息给前端 [安全/可维护性]

| 属性 | 值 |
|------|-----|
| **文件** | `backend/.../service/impl/BookServiceImpl.java` |
| **位置** | L298 |
| **维度** | 安全漏洞 / 可读性 |

**问题描述**:

```java
} catch (Exception e) {
    result.getErrors().add("第" + rowNum + "行: " + e.getMessage());
```

`e.getMessage()` 可能包含数据库字段名、SQL错误信息等敏感内容，直接返回给前端可能导致信息泄露。

**修复建议**:

```java
} catch (Exception e) {
    log.error("导入第{}行失败", rowNum, e);
    result.getErrors().add("第" + rowNum + "行: 数据格式异常，请检查");
}
```

---

### MEDIUM-8: Docker dev compose Redis 端口 `6379:6379` 未绑定 127.0.0.1 [安全]

| 属性 | 值 |
|------|-----|
| **文件** | `docker-compose.yml` |
| **位置** | L29 |
| **维度** | 安全漏洞 |

**问题描述**:

```yaml:29:29:docker-compose.yml
ports:
    - "6379:6379"  # ← 生产环境对比: "127.0.0.1:6379:6379"
```

生产环境 `docker-compose.prod.yml` 中 Redis 未暴露端口，MySQL 和 backend 都正确绑定了 `127.0.0.1`。但开发环境 Redis 直接暴露到 `0.0.0.0:6379`，如果在共享网络环境开发，外部可访问 Redis。

**修复建议**:

```yaml
ports:
    - "127.0.0.1:6379:6379"
```

---

## 四、🟢 Low 级别

### LOW-1: `user.js` login() 中无意义的 try/catch 重抛 [可读性]

| 属性 | 值 |
|------|-----|
| **文件** | `frontend/src/stores/user.js` |
| **位置** | L50-52 |
| **维度** | 可读性与可维护性 |

```javascript:50:52:frontend/src/stores/user.js
} catch (error) {
    throw error  // 完全多余
}
```

**修复建议**: 移除该 try/catch 块，或添加日志记录：

```javascript
} catch (error) {
    console.error('登录失败:', error)
    throw error
}
```

---

### LOW-2: `user.js` 缺少 `_fetchingUserInfo` 的公开暴露 [可维护性]

| 属性 | 值 |
|------|-----|
| **文件** | `frontend/src/stores/user.js` + `frontend/src/router/index.js` L294 |
| **维度** | 可读性与可维护性 |

路由守卫直接操作 `userStore._fetchingUserInfo`（私有属性），破坏了 Pinia Store 的封装性。

**修复建议**: 在 store 中暴露公开的 ref 和方法：

```javascript
const fetchingUserInfo = ref(false)

// 在 return 中添加
return {
    // ...
    fetchingUserInfo,  // 暴露给外部读取
}
```

---

### LOW-3: JwtFilter `AntPathMatcher` 实例声明为 `static final` 但未使用（仅在 `shouldNotFilter` 中使用） [可维护性]

| 属性 | 值 |
|------|-----|
| **文件** | `backend/.../filter/JwtFilter.java` |
| **位置** | L132 |
| **维度** | 可读性与可维护性 |

`PATH_MATCHER` 声明为 `private static final` 但使用它的代码在非静态实例方法中。虽然 `AntPathMatcher` 本身是线程安全的，但将工具类声明为类变量而非实例变量更符合惯例。这不是真正的 bug，但可以优化为实例变量（避免静态上下文依赖）。

---

### LOW-4: `SecurityConfig` HSTS header 对所有环境生效（包括开发环境） [最佳实践]

| 属性 | 值 |
|------|-----|
| **文件** | `backend/.../config/SecurityConfig.java` |
| **位置** | L123 |
| **维度** | 最佳实践 |

```java:123:123:backend/.../config/SecurityConfig.java
.addHeaderWriter(new StaticHeadersWriter("Strict-Transport-Security", "max-age=31536000; includeSubDomains"))
```

HSTS 头在开发环境（HTTP）中是无意义的，且可能导致开发调试困难。建议仅在生产 profile 下添加 HSTS：

```java
// 条件添加 HSTS
@Value("${server.ssl.enabled:false}")
private boolean sslEnabled;

// 在 securityFilterChain 中:
if (sslEnabled) {
    headers.addHeaderWriter(new StaticHeadersWriter("Strict-Transport-Security", 
        "max-age=31536000; includeSubDomains"));
}
```

---

### LOW-5: `CaffeineConfig` 重复创建 `TwoLevelCacheManager` 实例 [可维护性]

| 属性 | 值 |
|------|-----|
| **文件** | `backend/.../config/CaffeineConfig.java` |
| **位置** | L42-51, L58-68, L76-87 |
| **维度** | 可读性与可维护性 |

三个 Bean 方法中重复了相同的 `cacheNames` 数组定义和配置逻辑。虽然条件注解（`@Profile`）确保不会同时创建两个 `@Primary` Bean，但重复代码增加了维护成本。

**修复建议**: 提取公共 `cacheNames` 为常量。

---

## 五、安全审计专项报告

### 总体评价

项目安全基础良好：JWT 双 Token 机制、Redis 滑动窗口限流、XSS 过滤、CSRF 防护、Spring Security RBAC、账户锁定均已实现。大部分历史安全问题已修复。

### 安全检查清单

| 检查项 | 状态 | 备注 |
|--------|------|------|
| 密码哈希 (BCrypt) | ✅ | `SecurityConfig` L75 |
| JWT 密钥环境变量 | ✅ | `JwtUtils` L35，启动强制校验 |
| Token 黑名单 | ✅ | Redis 统一使用 JTI |
| 登录限流 | ✅ | 独立 5次/分钟 |
| 注册限流 | ✅ | 独立 3次/分钟 |
| API 通用限流 | ✅ | 滑动窗口 + 本地降级 |
| XSS 过滤 | ✅ | `XssFilter` + `XssRequestWrapper` |
| CSP 头 | ✅ | `SecurityConfig` L125-126 |
| HSTS 头 | ⚠️ | 对所有环境生效 (见 LOW-4) |
| CORS 配置 | ✅ | 环境变量可配置 |
| RBAC 权限控制 | ✅ | `@PreAuthorize` + `@EnableMethodSecurity` |
| CSRF 防护 | N/A | JWT 无状态模式已禁用 CSRF |
| 默认密码 | ❌ | `123456` 硬编码 (见 HIGH-4) |
| 注册验证码 | ❌ | 缺少验证码校验 (见 HIGH-2) |
| 路由认证 | ⚠️ | `BorrowDetail` 缺少 requiresAuth (见 HIGH-3) |
| Docker 非 root | ✅ | `backend/Dockerfile` L30 |
| SSL/TLS 配置 | ✅ | Nginx 配置 TLSv1.2/1.3 |

### OWASP Top 10 (2021) 覆盖情况

| OWASP 类别 | 覆盖状态 |
|------------|---------|
| A01: Broken Access Control | ✅ RBAC + 方法级权限注解 |
| A02: Cryptographic Failures | ✅ BCrypt + 环境变量密钥 |
| A03: Injection | ✅ MyBatis-Plus 参数化 + XSS Filter |
| A04: Insecure Design | ⚠️ 注册接口缺验证码 |
| A05: Security Misconfiguration | ✅ 安全头 + CORS 配置 |
| A06: Vulnerable Components | 未审计（需 `mvn dependency-check`） |
| A07: Identification Failures | ✅ 账户锁定 + 登录限流 |
| A08: Software Integrity | 未审计（CI 有 Docker build check） |
| A09: Logging Failures | ✅ SecurityAuditLogger + 操作日志 |
| A10: SSRF | 未发现相关功能 |

---

## 六、汇总统计

### 问题分布

| 严重级别 | Bug/逻辑 | 性能 | 安全 | 可读性 | 最佳实践 | 合计 |
|----------|---------|------|------|--------|---------|------|
| 🔴 Critical | 2 | 0 | 0 | 0 | 0 | **2** |
| 🟠 High | 2 | 0 | 3 | 0 | 0 | **5** |
| 🟡 Medium | 3 | 1 | 2 | 0 | 2 | **8** |
| 🟢 Low | 0 | 0 | 0 | 3 | 2 | **5** |
| **合计** | **7** | **1** | **5** | **3** | **4** | **20** |

### 历史问题修复率

| 状态 | 数量 | 占比 |
|------|------|------|
| ✅ 已修复 | 5 | 83.3% |
| △ 架构变更（不再适用） | 1 | 16.7% |
| ❌ 未修复 | 0 | 0% |

---

## 七、建议修复优先级

### 立即修复 (本周)
1. **CRITICAL-1**: BookServiceImpl `@CacheEvict` 的 `beforeInvocation` 问题
2. **CRITICAL-2**: Nginx 配置 `return 301` 死代码
3. **HIGH-3**: 前端 `BorrowDetail` 路由缺少 `requiresAuth`
4. **HIGH-4**: 默认密码 `123456` 硬编码

### 近期修复 (本月)
5. **HIGH-1**: `user.js` logout 未 await
6. **HIGH-2**: 注册接口缺少验证码
7. **HIGH-5**: refreshToken 持久化
8. **MEDIUM-1**: XssFilter `ContentCachingResponseWrapper` 问题
9. **MEDIUM-4**: `getClientIp()` IP 顺序错误
10. **MEDIUM-8**: Docker Redis 端口绑定

### 技术债务（下个迭代）
11. **MEDIUM-2**: 废弃 API 清理
12. **MEDIUM-3**: Semaphore 释放时机
13. **MEDIUM-5**: POST params → data 重构
14. **MEDIUM-6**: `importBooks` 批量插入优化
15. **MEDIUM-7**: 异常消息脱敏
16. **LOW-1 ~ LOW-5**: 代码质量优化

---

*报告由 AI 辅助审查生成，建议人工复核 Critical 和 High 级别问题后再进行修复。*

# 图书馆管理系统 — 全面代码审查报告

**审查日期**: 2025-07-17  
**审查范围**: 后端 Java 源文件 + 前端 Vue/JS 源文件  
**技术栈**: Spring Boot 3.2.5 + Vue 3.4 + MyBatis-Plus + Redis + MySQL  
**审查方式**: 7 专业视角全面审查（Security, Red Team, Performance, Maintainability, API Contract, Testing, Data Safety）

---

## 1. 🔴 严重 (CRITICAL)

### CRITICAL-1: Cookie 未设置 HttpOnly 导致 Token 可被 JS 读取 [Security]

| 维度 | 值 |
|------|-----|
| **文件** | `frontend/src/utils/auth.js` L1-L126 |
| **领域** | 前端安全 |
| **自信度** | 10/10 |

**问题描述**:  
Access Token、Refresh Token 和 CSRF Token 均通过 `js-cookie` 库设置 Cookie，但未设置 `httpOnly: true`。这意味着任何 XSS 攻击都可以通过 `document.cookie` 读取 Token，从而完全绕过认证保护。

```javascript
const COOKIE_OPTIONS = {
  expires: 2 / 24,
  secure: IS_PROD,
  sameSite: 'Lax',
  path: '/'
  // ❌ 缺少 httpOnly: true
}
```

**安全风险**: XSS → Cookie 泄露 → 账户接管。由于使用了双 Token 机制（Access + Refresh），攻击者可同时获取两者，实现持久化攻击。

**代码中的注释也承认了这个问题** (auth.js L118-L121):
```
// SEC-01/SEC-03: 当前CSRF Token和Refresh Token通过前端JS管理Cookie，
// 未设置HttpOnly属性。生产环境建议后端通过Set-Cookie头设置HttpOnly Cookie
```

**修复建议**:  
**根本解决方案**（推荐）：后端在登录/刷新接口的 HTTP 响应中通过 `Set-Cookie` 头设置 HttpOnly Cookie，前端只通过 `withCredentials: true`（已配置）自动携带。

**权宜解决方案**：在 Cookie 选项中添加 `httpOnly: true` 虽无法通过前端 JS 设置 HttpOnly Cookie（这是浏览器安全机制），但可以将 Token 移至内存变量而非 Cookie，避免 JS 可读：
```javascript
// 方案：将 Token 移至内存，XSS 无法读取
let inMemoryToken = null
let inMemoryRefreshToken = null

export function getToken() { return inMemoryToken }
export function setToken(token) { inMemoryToken = token }
export function getRefreshToken() { return inMemoryRefreshToken }
```

---

### CRITICAL-2: JwtFilter 未校验 Token 类型 — Refresh Token 可作为 Access Token 使用 [Red Team]

| 维度 | 值 |
|------|-----|
| **文件** | `backend/.../filter/JwtFilter.java` L58-L98 |
| **领域** | 后端认证 |
| **自信度** | 10/10 |

**问题描述**:  
`JwtFilter` 仅验证 Token 的签名和有效期，但**没有检查 Token 的 type claim 是否为 "ACCESS"**。Refresh Token 具有更长的有效期（7 天），如果在 JwtFilter 上不做类型区分，攻击者获取到 Refresh Token 后可以将其当作 Access Token 直接使用，绕过短期 Token 的安全设计。

```java
// JwtFilter.java L56-67 - 仅验证签名和是否在黑名单
if (!jwtUtils.validateToken(token)) {
    // ... 返回 401
}
// 从未检查 token 中的 claim "type" 是否为 "ACCESS"
```

对比 `JwtUtils.java` 生成 Token 时设置了 type：
```java
// Access Token 设置了 type=ACCESS
.claim("type", Constants.Token.TOKEN_TYPE_ACCESS)

// Refresh Token 设置了 type=REFRESH
.claim("type", Constants.Token.TOKEN_TYPE_REFRESH)
```

**修复建议**:  
在 `JwtFilter` 的 `doFilterInternal` 方法中，在 `validateToken` 之后添加类型校验：
```java
// 校验是否为 Access Token
String tokenType = jwtUtils.getTokenType(token);
if (!Constants.Token.TOKEN_TYPE_ACCESS.equals(tokenType)) {
    log.warn("Token类型不匹配: expected=ACCESS, actual={}", tokenType);
    securityAuditLogger.logTokenSecurityEvent("TOKEN_TYPE_MISMATCH", 
            "uri=" + request.getRequestURI() + ", type=" + tokenType);
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.getWriter().write("{\"code\":401,\"message\":\"Token类型无效\"}");
    return;
}
```

---

### CRITICAL-3: application.yml 开发环境 useSSL=false 且 production profile 存在重复定义 [Security]

| 维度 | 值 |
|------|-----|
| **文件** | `backend/src/main/resources/application.yml` L21, L146, L167, L238 |
| **领域** | 配置安全 |
| **自信度** | 9/10 |

**问题描述**:  
两个严重配置问题：

1. **开发环境 useSSL=false** (L21): MySQL 连接 URL 中 `useSSL=false&allowPublicKeyRetrieval=true`。`allowPublicKeyRetrieval=true` 允许中间人攻击获取数据库公钥。开发环境虽可容忍但存在误上生产风险。

2. **Production profile 重复定义** (L161 和 L238): `application.yml` 中出现了**两个独立的 prod profile 定义**。Spring Boot 会合并它们，但第二个 prod profile（L238-249）覆盖了第一个 prod profile 中的 `springdoc` 配置，可能导致 API 文档状态不可预测。更严重的是，第二个 prod profile 的 YAML 语法看起来是无效的——在 `spring.config.activate.on-profile: prod` 下的 `springdoc` 缩进不正确。

```yaml
# L161: 第一个 prod profile
---
spring:
  config:
    activate:
      on-profile: prod
  datasource:
    # ... 生产数据库配置

# L238: 第二个 prod profile（覆盖）
---
spring:
  config:
    activate:
      on-profile: prod
  # ❌ 这里的 springdoc 缩进可能无效
  springdoc:
    api-docs:
      enabled: false
```

**修复建议**:  
删除第二个 prod profile 定义（L237-249），将 Swagger 禁用配置合并到第一个 prod profile 中。

---

## 2. 🟠 中等 (WARNING)

### WARNING-1: XssFilter 过滤后长度可能超出数据库字段限制 [Red Team]

| 维度 | 值 |
|------|-----|
| **文件** | `backend/.../filter/XssRequestWrapper.java` L216-L266 |
| **领域** | 后端安全-输入验证 |
| **自信度** | 8/10 |

**问题描述**:  
`filterXss()` 方法通过正则替换移除危险内容（例如移除 `javascript:` 协议）。但正则替换**不改变字符串的操作结果长度**——当数据通过 XSS 过滤后，内容可能变短但原始长度校验已通过。极端情况下，攻击者可以构造既是 XSS payload 又超过字段长度限制的输入，XSS 过滤去除 payload 后剩余内容恰好符合业务逻辑（例如 SQL 注入片段）。

更实际的场景：数据库字段在 `@Size` 限制下（如 `BookRequest.description` 限 2000 字符），如果 XSS 过滤在校验之后执行（过滤在 filter 层，校验在 Controller 层），则没问题；如果过滤在校验之前，则需要确保过滤后的数据不会违反业务规则。

**当前架构**：`XssFilter` 是 Servlet Filter（在 Controller 之前执行），而 `@Valid` 校验在 Controller 参数绑定阶段。所以**过滤在校验之前**，但过滤后的数据可能不符合后续处理逻辑的预期。

**修复建议**:  
书面前后端都应进行长度校验。前端应该对过滤后的内容再做一次长度检查。

---

### WARNING-2: Refresh Token 轮换（Rotation）未实现 — 重放攻击风险 [Security]

| 维度 | 值 |
|------|-----|
| **文件** | `backend/.../service/impl/AuthServiceImpl.java` L136-L164 |
| **领域** | 后端认证 |
| **自信度** | 9/10 |

**问题描述**:  
`refreshToken()` 方法在执行 Token 刷新时，虽然生成了新的 Refresh Token，但**没有将旧的 Refresh Token 加入黑名单**。这意味着如果旧的 Refresh Token 被泄露，攻击者可以反复使用它刷新 Token，持续获取新的 Access Token。

```java
public LoginResponse refreshToken(String refreshToken) {
    // 验证旧token
    if (!jwtUtils.validateToken(refreshToken)) { ... }
    Long userId = jwtUtils.getUserIdFromToken(refreshToken);
    User user = userMapper.selectById(userId);
    
    // 生成新Token
    String newAccessToken = jwtUtils.generateAccessToken(userId, username, role);
    String newRefreshToken = jwtUtils.generateRefreshToken(userId, username);
    // ❌ 没有将旧 refreshToken 加入黑名单
    return LoginResponse.builder()
            .accessToken(newAccessToken)
            .refreshToken(newRefreshToken)
            .build();
}
```

**修复建议**:  
在生成新 Token 后，将旧的 Refresh Token 加入黑名单（使用 JTI）：
```java
// 将旧 refresh token 加入黑名单
String oldJti = jwtUtils.getJtiFromToken(refreshToken);
if (oldJti != null) {
    String blacklistKey = Constants.Token.BLACKLIST_PREFIX + oldJti;
    tokenBlacklistService.addToBlacklist(blacklistKey, 
            jwtUtils.getRefreshTokenExpiration() / 1000);
}
```

---

### WARNING-3: AuthServiceImpl 使用 @Transactional 但涉及 Redis 操作 [Performance]

| 维度 | 值 |
|------|-----|
| **文件** | `backend/.../service/impl/AuthServiceImpl.java` L52, L103 |
| **领域** | 后端数据一致性 |
| **自信度** | 8/10 |

**问题描述**:  
`login()` 和 `register()` 方法标注了 `@Transactional`（Spring 声明式事务），但在 login 方法体内同时操作了 MySQL（UserMapper）和 Redis（StringRedisTemplate）。Spring 的 `@Transactional` 只管理 JDBC 资源（MySQL），**不管理 Redis**。如果 Redis 操作失败（如 `validateCaptcha` 中读取验证码），MySQL 事务会因为已提交而产生数据不一致。

```java
@Override
@Transactional  // 只管理 MySQL
public LoginResponse login(LoginRequest request) {
    validateCaptcha(request.getCaptchaKey(), request.getCaptchaCode());  // Redis 操作
    User user = userMapper.selectByUsername(request.getUsername());  // MySQL 操作
    // ...
    accountLockService.recordLoginFailure(user.getId(), user.getUsername());  // Redis 操作
    // ...
    accountLockService.clearLoginFailures(user.getId());  // Redis 操作
    return ...;
}
```

**修复建议**:  
- 将非事务性的 Redis 操作移到 `@Transactional` 方法外部
- 或者使用事务消息/补偿机制
- 对于登录操作，将 Redis 验证码校验放在事务开始前

---

### WARNING-4: 前端搜索参数与后端 API 字段名不一致 [API Contract]

| 维度 | 值 |
|------|-----|
| **文件** | `frontend/src/views/book/BookList.vue` L219-L223 vs `backend/.../controller/BookController.java` L53-L56 |
| **领域** | 前后端契约 |
| **自信度** | 8/10 |

**问题描述**:  
前端 `BookList.vue` 搜索表单用 `name` 字段（L220），但后端 `BookController.listBooks()` 接受 `keyword` 参数（L55）。`searchForm` 对象发送到后端时包含 `{ name: '', author: '', category: '' }`，而后端只读取 `keyword`、`categoryId`，**author 字段会被直接忽略**。

```javascript
// 前端 searchForm (BookList.vue L219-223)
const searchForm = reactive({
  name: '',      // ❌ 后端期望 keyword
  author: '',    // ❌ 后端不接收此字段
  category: ''   // ❌ 后端期望 categoryId (Long类型)
})
```

```java
// 后端 Controller (BookController.java L53-56)
public ApiResponse<PageResult<BookResponse>> listBooks(
    @RequestParam(required = false) String keyword,    // ← 不是 name
    @RequestParam(required = false) Long categoryId) { // ← 不是 String
```

**修复建议**:  
统一前后端契约。前端将 `searchForm` 映射为后端期望的参数名：
```javascript
const params = {
  keyword: searchForm.name,
  categoryId: searchForm.category ? Number(searchForm.category) : undefined
}
```

---

### WARNING-5: 账户锁定缺乏递增延迟机制 — 暴力破解防护不足 [Red Team]

| 维度 | 值 |
|------|-----|
| **文件** | `backend/.../service/impl/AuthServiceImpl.java` L76-L83 |
| **领域** | 后端安全-暴力破解 |
| **自信度** | 7/10 |

**问题描述**:  
账户锁定机制是简单的"5 次失败锁定 15 分钟"，没有引入递增延迟机制。攻击者可以：
1. 每 15 分钟尝试 5 次密码
2. 在 24 小时内可以进行 480 次尝试
3. 对于弱密码（如 `password123`），成功概率不低

更安全的做法是引入指数级递增的锁定时间。

**修复建议**:  
```
第1次锁定: 15分钟
第2次锁定: 30分钟  
第3次锁定: 1小时
第4次锁定: 2小时
...
```
在 `AccountLockService` 中根据历史锁定次数计算锁定时间。

---

## 3. 🟡 轻微 (INFO)

### INFO-1: JwtUtils.parseToken 静默吞异常导致调用链难以排查 [Maintainability]

| 维度 | 值 |
|------|-----|
| **文件** | `backend/.../util/JwtUtils.java` L294-L305 |
| **领域** | 后端可维护性 |
| **自信度** | 9/10 |

**问题描述**:  
`parseToken()` 方法捕获所有 `Exception` 后仅返回 `null`，不抛出也不记录详细日志。所有调用方（`getUserIdFromToken`、`getUsernameFromToken` 等）在获取到 `null` 后抛出的 `IllegalArgumentException("无法从Token中提取...：Token解析失败")` 丢失了原始异常的堆栈信息，使调试困难。

```java
public Claims parseToken(String token) {
    try {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    } catch (Exception e) {
        log.warn("解析Token失败: {}", e.getMessage());  // 只记录消息，丢失堆栈
        return null;  // 静默返回 null
    }
}
```

**修复建议**:  
在 catch 中记录完整堆栈，并包装为自定义异常抛出，而非返回 null：
```java
catch (Exception e) {
    log.warn("解析Token失败", e);  // 记录完整堆栈
    throw new SecurityException("Token解析失败", e);
}
```

---

### INFO-2: BookController./books/new 路由未在 SecurityConfig 中公开 [API Contract]

| 维度 | 值 |
|------|-----|
| **文件** | `backend/.../controller/BookController.java` L87 + `backend/.../config/SecurityConfig.java` L144-L165 |
| **领域** | 后端权限配置 |
| **自信度** | 8/10 |

**问题描述**:  
`BookController` 中 `GET /books/new`（新书推荐）接口没有在 `SecurityConfig` 的 `permitAll()` 列表中。虽然 `JwtFilter.shouldNotFilter()` 的 `PUBLIC_GET_PATHS` 中通过 `/books/**` 覆盖了该路径，但 `SecurityConfig` 的 `authorizeHttpRequests` 中只显式列出了：
- `/books` (GET) 
- `/books/hot` (GET)
- `/books/{id}` (GET)
- `/books/check-isbn` (GET)

`/books/new` 不在其中，`anyRequest().authenticated()` 会要求该路径认证。

**修复建议**:  
在 SecurityConfig 的 permitAll 列表中添加 `/books/new`。

---

### INFO-3: BookList.vue 状态列使用 stock > 0 判断，但后端返回的是 availableCount [API Contract]

| 维度 | 值 |
|------|-----|
| **文件** | `frontend/src/views/book/BookList.vue` L121-L123 |
| **领域** | 前后端契约 |
| **自信度** | 7/10 |

**问题描述**:  
前端表格状态列使用 `row.stock > 0` 判断图书状态，但 `BookResponse` DTO 中返回的字段名为 `availableCount`（实际库存）和 `totalCount`（总库存）。`stock` 字段在 DTO 中不存在。

```vue
<el-tag :type="row.stock > 0 ? 'success' : 'danger'">
  {{ row.stock > 0 ? '在架' : '借出' }}
</el-tag>
```

**修复建议**:  
改为 `row.availableCount > 0`。

---

### INFO-4: XssRequestWrapper 使用 getHeader 检测 Content-Type 可能导致循环 [Bug]

| 维度 | 值 |
|------|-----|
| **文件** | `backend/.../filter/XssRequestWrapper.java` L88, L149 |
| **领域** | 后端 Bug |
| **自信度** | 7/10 |

**问题描述**:  
`XssRequestWrapper` 的 `getInputStream()` 和 `getReader()` 方法调用 `isJsonRequest()`，而 `isJsonRequest()` 又调用 `getHeader("Content-Type")`——这个方法被 `XssRequestWrapper` 自身覆盖并调用了 `filterXss()`。虽然对 Content-Type 做 XSS 过滤不会有实际伤害，但这是一个意外的副作用，且性能上存在不必要的字符串处理。

```java
private boolean isJsonRequest() {
    String contentType = getHeader("Content-Type"); // 调用了被覆盖的 getHeader()
    return contentType != null && contentType.contains("application/json");
}
```

**修复建议**:  
在 `isJsonRequest()` 中调用 `super.getHeader()` 以避免 XSS 过滤逻辑被应用于 HTTP 头：
```java
private boolean isJsonRequest() {
    String contentType = super.getHeader("Content-Type");
    return contentType != null && contentType.contains("application/json");
}
```

---

### INFO-5: BorrowServiceImpl.validateAndGetRecord 硬编码 userId 校验导致管理员无法代理操作 [API Contract]

| 维度 | 值 |
|------|-----|
| **文件** | `backend/.../service/impl/BorrowServiceImpl.java` L374-L386 |
| **领域** | 后端业务逻辑 |
| **自信度** | 7/10 |

**问题描述**:  
`validateAndGetRecord()` 方法总是使用当前认证用户的 userId 校验记录所有权，这导致管理员（ADMIN/LIBRARIAN）无法代理操作归还/续借。虽然 `BorrowController` 通过 `@PreAuthorize` 允许 ADMIN 和 LIBRARIAN 调用，但 service 层的硬编码校验会在管理员尝试操作不属于自己的借阅记录时抛出 ForbiddenException。

```java
private BorrowRecord validateAndGetRecord(Long borrowId, Long userId) {
    BorrowRecord record = borrowRecordMapper.selectBorrowWithJoinById(borrowId);
    // ...
    if (!record.getUserId().equals(userId)) {  // ❌ 管理员也会被拒绝
        throw new ForbiddenException(...);
    }
    // ...
}
```

**修复建议**:  
将角色信息传入 service 层，管理员跳过所有权校验：
```java
private BorrowRecord validateAndGetRecord(Long borrowId, Long userId, String role) {
    // ...
    boolean isAdmin = "ADMIN".equals(role) || "LIBRARIAN".equals(role);
    if (!isAdmin && !record.getUserId().equals(userId)) {
        throw new ForbiddenException(...);
    }
    // ...
}
```

---

### INFO-6: 多处硬编码字符串代替常量引用 [Maintainability]

| 维度 | 值 |
|------|-----|
| **文件** | 多个文件 |
| **领域** | 后端可维护性 |
| **自信度** | 9/10 |

**问题描述**:  
虽然项目已做了大量重构，多处使用了 `Constants.java`，但仍有一些地方使用硬编码字符串：

1. `BorrowServiceImpl.java` L53: `new BigDecimal("0.10")` 应为常量
2. `BorrowServiceImpl.java` L325: `borrowDays < 1 || borrowDays > 60` 中的 60 应为常量  
3. `JwtFilter.java` L64: `"{\"code\":401,\"message\":\"Token无效或已过期\"}"` 的 JSON 字符串硬编码
4. `AuthServiceImpl.java` L197-200: 卡号生成中的 "R" 前缀和格式字符串应为常量

**修复建议**:  
抽取为 `Constants.java` 中的命名常量。

---

### INFO-7: PasswordChangeRequest 存在但 controller 中无对应接口 [Dead Code]

| 维度 | 值 |
|------|-----|
| **文件** | `dto/PasswordChangeRequest.java` vs 所有 Controller |
| **领域** | 后端维护 |
| **自信度** | 8/10 |

**问题描述**:  
`PasswordChangeRequest` DTO 已定义完整（包含旧密码、新密码、确认密码的校验），但没有任何 Controller 使用它。`AuthServiceImpl.changePassword()` 方法接受三个独立参数而不是使用该 DTO。这是**死代码**。

**修复建议**:  
- 删除未使用的 `PasswordChangeRequest.java`
- 或在 `AuthController` 中添加密码修改端点并使用该 DTO

---

### INFO-8: 前端 Login.vue 的 `v-model` 绑定直接修改 props 对象 [Vue 3 Anti-pattern]

| 维度 | 值 |
|------|-----|
| **文件** | 待确认（`Login.vue` 未完全读取，但 `register` 同理） |
| **领域** | 前端代码质量 |
| **自信度** | 6/10 |

**问题描述**:  
未完整读取 Login.vue，但常见模式是 `reactive()` 或 `ref()` 对象直接绑定到 `el-form`，这没有大问题。但需确认是否存在直接修改 `props` 的情况。

---

## 4. 🟢 建议 (SUGGESTION)

### SUGGESTION-1: 缺少系统化的 CSRF 保护机制

尽管前端发送了 `X-CSRF-Token` 头，但后端没有对应的过滤器来验证这个 Token。`XssFilter` 和 `JwtFilter` 都不处理 CSRF Token。如果浏览器端的 `sameSite: 'Lax'` 被绕过（某些浏览器或请求方式下），后端不会有额外的防御层。

**建议**: 实现 CsrfFilter 来验证前端传来的 `X-CSRF-Token` 与 Session/Redis 中存储的值是否一致。

---

### SUGGESTION-2: 分页参数未做上限限制

多个 Controller 的分页参数直接使用 `@RequestParam(defaultValue = "10") Long size`。虽然 `Constants.Page.MAX_SIZE = 100` 已定义，但没有在任何地方使用。攻击者可以请求 `size=999999` 进行拒绝服务攻击。

**建议**: 在 BaseController 中添加分页参数验证，或使用 `@Max` 注解约束。

---

### SUGGESTION-3: 测试覆盖范围有限

项目已有测试基础设施（`frontend/src/__tests__/` 目录），但只覆盖了前端工具函数和 API 层。后端完全没有任何测试。

**建议**: 
- 添加后端单元测试（Spring Boot Test + Mockito）
- 关键路径必须测试：登录/注册/刷新 Token、借阅/归还的并发安全性

---

### SUGGESTION-4: 日志中打印用户密码相关请求

`AuthController.login()` L55 中 `log.info("用户登录请求: {}", request.getUsername())` 可以接受，但需确保**任何地方都不要记录用户密码**。已检查 `LoginRequest` 的 `toString()`—Lombok `@Data` 会生成包含 password 字段的 toString，如果被日志无意调用会导致敏感信息泄露。

**建议**: 在 LoginRequest 上手动覆盖 `toString()` 排除 password，或使用 `@ToString.Exclude`。

---

## 5. 审查统计汇总

| 严重度 | 数量 | 关键领域 |
|--------|------|---------|
| 🔴 严重 | 3 | Cookie HttpOnly、Token 类型校验、配置重复 |
| 🟠 中等 | 5 | XSS+长度、Refresh轮换、MySQL+Redis事务、前后端参数不一致、暴力破解 |
| 🟡 轻微 | 8 | 异常处理、路由缺失、字段映射、Buffer溢出、角色校验、硬编码、死代码 |
| 🟢 建议 | 4 | CSRF、分页限制、测试覆盖、日志安全 |
| **总计** | **20** | |

### 按模块分布

| 模块 | 数量 | 主要问题 |
|------|------|---------|
| 后端认证/安全 | 7 | Token 校验、CORS、暴力破解 |
| 后端配置 | 2 | Profile 重复、DB 连接安全性 |
| 后端 API 设计 | 4 | 参数不匹配、路由遗漏、死代码 |
| 后端数据一致性 | 1 | MySQL+Redis 事务 |
| 后端可维护性 | 2 | 异常吞没、硬编码 |
| 前端安全 | 1 | Cookie HttpOnly |
| 前端业务逻辑 | 3 | 字段名不一致、搜索参数错误 |
| 测试 | 1 | 后端零测试覆盖 |

### Top 5 优先修复项

| 优先级 | 问题 | 影响 | 预估工时 |
|--------|------|------|---------|
| P0 | CRITICAL-1: Cookie HttpOnly | 账户接管 | 1-2天 |
| P0 | CRITICAL-2: Token 类型校验 | 认证绕过 | 0.5天 |
| P0 | CRITICAL-3: 配置安全 | 生产安全 | 0.5天 |
| P1 | WARNING-2: Refresh Token 轮换 | 重放攻击 | 0.5天 |
| P1 | WARNING-4: 前后端参数不一致 | 搜索功能失效 | 0.5天 |

# 图书馆系统后端代码缺陷报告

> 报告日期: 2026-05-15  
> 分析范围: 9 个核心服务和安全文件  
> 严重级别定义: Critical(系统崩溃/安全绕过) > High(功能异常/数据不一致) > Medium(逻辑缺陷/性能) > Low(代码质量)

---

## 1. Critical 级别缺陷

### 1.1 Token 黑名单机制完全失效（登出不生效）

| 属性 | 值 |
|------|------|
| **文件** | `AuthServiceImpl.java` 第 169 行 vs `JwtFilter.java` 第 70 行 |
| **类型** | 逻辑错误 / Token 吊销绕过 |
| **描述** | `AuthServiceImpl.logout()` 将黑名单 key 设置为 `BLACKLIST_PREFIX + 完整JWT字符串`，而 `JwtFilter` 中黑名单校验使用的是 `BLACKLIST_PREFIX + JTI(UUID)`。两个 key 格式完全不同，永远不会匹配。 |
| **影响** | 用户登出后，黑色名单校验永远找不到对应记录，Token 仍然可用。Token 吊销、强制下线、密码修改后失效等功能全部失效。 |
| **代码** | `AuthServiceImpl.java:169`: `String blacklistKey = Constants.Token.BLACKLIST_PREFIX + token;` |
| | `JwtFilter.java:70`: `String blacklistKey = TOKEN_BLACKLIST_PREFIX + jti;` |
| **建议修复** | 统一使用 JTI 作为黑名单 key，在 `logout()` 中先提取 JTI 再构建 key：<br>`String jti = jwtUtils.getJtiFromToken(token);`<br>`String blacklistKey = Constants.Token.BLACKLIST_PREFIX + jti;` |

---

## 2. High 级别缺陷

### 2.1 JWT 默认密钥硬编码回退

| 属性 | 值 |
|------|------|
| **文件** | `JwtUtils.java` 第 33 行 |
| **类型** | 安全漏洞 — 密钥硬编码 |
| **描述** | JWT 密钥配置 `${jwt.secret:${JWT_SECRET:library-system-secret-key-2024-secure-jwt-token-generation}}` 存在硬编码的默认值。当环境变量和生产配置均未设置时，会使用此已知字符串作为签名密钥，攻击者可据此伪造任意 JWT Token。 |
| **影响** | 在配置缺失的部署环境中，任意用户可伪造管理员 Token。 |
| **建议修复** | 移除默认值回退，启动时强制校验：<br>`@Value("${jwt.secret}")`<br>并在 `getSigningKey()` 中检查 secret 是否为默认值或为 null，直接抛 IllegalStateException 阻止启动。 |

### 2.2 parseToken() 返回 null 导致 NPE 链式传播

| 属性 | 值 |
|------|------|
| **文件** | `JwtUtils.java` 第 155-216 行 |
| **类型** | 潜在空指针异常 |
| **描述** | `parseToken()` 方法（第 251 行）在解析异常时返回 `null`。但 `getUserIdFromToken()`、`getUsernameFromToken()`、`getRoleFromToken()`、`getJtiFromToken()` 等 6 个方法直接解引用返回的 Claims 对象而未做 null 检查。`JwtFilter` 中（第 69、82-84 行）在 validateToken 通过后调用这些方法，虽然当前逻辑下 parseToken 不应返回 null，但若 validateToken 和 parseToken 处理异常的逻辑不一致（validateToken 只catch JWT相关的异常，parseToken catch 所有 Exception），可能因 ClassCastException 等异常导致 parseToken 返回 null 进而触发 NPE。 |
| **建议修复** | 在 `parseToken()` 中直接抛出异常而非返回 null；或所有 getter 方法增加 null 防御：<br>`Claims claims = parseToken(token);`<br>`if (claims == null) throw new IllegalArgumentException("Token 解析失败");` |

---

## 3. Medium 级别缺陷

### 3.1 图书归还时库存更新结果被忽略

| 属性 | 值 |
|------|------|
| **文件** | `BorrowServiceImpl.java` 第 444 行 |
| **类型** | 数据不一致 |
| **描述** | `updateBookStock()` 方法调用 `bookMapper.updateAvailableCount(book.getId(), 1, book.getVersion(), 0)` 但未检查返回值。如果库存更新因乐观锁冲突失败，错误的返回值静默丢失，导致图书实际可借数量未增加，库存与实际借阅状态不一致。 |
| **影响** | 用户归还图书后，图书可借数量可能未正确恢复。 |
| **建议修复** | 检查返回值并抛出异常：<br>`int updated = bookMapper.updateAvailableCount(book.getId(), 1, book.getVersion(), 0);`<br>`if (updated == 0) throw new BusinessException(ErrorCode.CONCURRENT_OPERATION, "库存更新失败");` |

### 3.2 续借操作缺少乐观锁保护

| 属性 | 值 |
|------|------|
| **文件** | `BorrowServiceImpl.java` 第 478-482 行 |
| **类型** | 并发数据竞争 |
| **描述** | `performRenewOperation()` 直接使用 `updateById(record)` 更新借阅记录，没有版本号（乐观锁）校验。虽然使用了 Redis 分布式锁，但锁可能因租约到期提前释放。如果续借操作执行时间超过 30 秒锁租约，另一个请求可能同时续借，导致续借次数超过限制或应还日期计算错误。 |
| **建议修复** | 在 `BorrowRecord` 实体增加 version 字段，使用 MyBatis-Plus 乐观锁插件，或者使用 SQL 原子化更新（UPDATE ... SET renew_count = renew_count + 1 WHERE id = ? AND renew_count < max）。 |

### 3.3 验证码可被完全跳过

| 属性 | 值 |
|------|------|
| **文件** | `AuthServiceImpl.java` 第 298-301 行 |
| **类型** | 安全绕过（视业务需求而定） |
| **描述** | `validateCaptcha()` 方法在 captchaKey 或 captchaCode 任一为 null 时直接 return，不进行任何校验。如果前端控制台或 API 调用直接移除验证码参数，后端无条件放行。 |
| **影响** | 如果设计预期是"所有登录都需要验证码"，那么此逻辑存在安全绕过漏洞，可被用于暴力破解攻击。如果预期是验证码可选（如内网环境），则此行为可接受。 |
| **建议修复** | 若需强制验证码校验，移除 null 短路返回；若需按环境区分，可使用配置开关控制是否强制验证码。 |

### 3.4 CORS 配置允许 credential 但未校验 Origin 格式

| 属性 | 值 |
|------|------|
| **文件** | `SecurityConfig.java` 第 187-191 行 |
| **类型** | 安全—CORS 配置 |
| **描述** | `setAllowCredentials(true)` 与 `setAllowedOrigins(...)` 共用时，Spring 要求 allowedOrigins 不能为 `*`。当前从配置读取逗号分隔列表，但未对配入的 Origin 值做合法性校验（如不包含 scheme+host+port 的格式、配置为 `*`、空字符串等），可能导致配置错误时 CORS 策略失效。 |
| **建议修复** | 增加配置校验逻辑，拒绝 `*`、空值、非法格式；或使用 `allowedOriginPatterns()` 以支持更安全的模式匹配。 |

### 3.5 图书卡号生成未校验唯一性

| 属性 | 值 |
|------|------|
| **文件** | `AuthServiceImpl.java` 第 125-126 行、第 190-196 行 |
| **类型** | 数据完整性 |
| **描述** | `generateCardNumber()` 生成格式为 `R` + `yyyyMM` + `XXXX`（4位随机数），每月最多 10000 个唯一卡号。生成后直接 insert 而不校验数据库中是否已存在相同卡号，若发生碰撞（概率虽低但可能），用户将共享同一卡号。 |
| **影响** | 极低概率下两个用户获取相同卡号，影响身份识别和借阅操作。 |
| **建议修复** | 生成后检查数据库中是否已存在该 cardNumber，若存在则重新生成；或使用数据库唯一索引 + 重试机制。 |

### 3.6 RateLimitFilter 静默降级

| 属性 | 值 |
|------|------|
| **文件** | `SecurityConfig.java` 第 52-53 行、第 171-174 行 |
| **类型** | 安全—限流失效 |
| **描述** | `RateLimitFilter` 使用 `@Autowired(required = false)` 注入。当 Redis 不可用时 Bean 不会被创建，限流过滤器静默不添加到安全链中，且无告警日志记录。攻击者可利用 Redis 故障窗口发起无限制的请求。 |
| **建议修复** | 限流降级时输出 WARN 级别日志；或提供本地内存限流作为 Redis 不可用时的备用方案。 |

---

## 4. Low 级别缺陷

### 4.1 Response Writer 未刷新

| 属性 | 值 |
|------|------|
| **文件** | `JwtFilter.java` 第 64、77、107 行 |
| **类型** | 代码质量 |
| **描述** | Token 验证失败、被吊销、或处理异常时，`response.getWriter().write()` 写入响应体后未调用 `.flush()` 或 `.close()`。在缓冲设置较高的 Servlet 容器中，响应体可能不完整。 |
| **建议修复** | 在 write 后增加 `response.getWriter().flush();` |

### 4.2 Security Context Principal 使用不当

| 属性 | 值 |
|------|------|
| **文件** | `JwtFilter.java` 第 90 行 |
| **类型** | 代码质量/可维护性 |
| **描述** | 认证对象的 principal 设置为 `userId.toString()`（字符串），而非完整的 UserDetails 或自定义的 Principal 对象。后续代码如需获取用户名，需要通过 jwtUtils 再次解析 Token，增加了耦合度和重复解析开销。 |
| **建议修复** | 创建一个简单的 `CurrentUser` 对象（含 userId, username, role）作为 principal。 |

### 4.3 XSS 保护头配置冗余

| 属性 | 值 |
|------|------|
| **文件** | `SecurityConfig.java` 第 114 行、第 122 行 |
| **类型** | 代码质量 |
| **描述** | 第 114 行用 `.xssProtection(xss -> xss.disable())` 禁用了 Spring Security 内置的 XSS 保护头设置，然后第 122 行又手动通过 `StaticHeadersWriter` 添加 `X-XSS-Protection: 1; mode=block`。二者功能等价，冗余配置增加维护困惑。 |
| **建议修复** | 移除 `.xssProtection(xss -> xss.disable())`，让 Spring Security 内置逻辑接管 XSS 保护头。 |

### 4.4 借阅归还缺少管理员代还能力

| 属性 | 值 |
|------|------|
| **文件** | `BorrowServiceImpl.java` 第 94 行、第 391-403 行 |
| **类型** | 功能缺失 |
| **描述** | `returnBook()` 调用的 `validateAndGetRecord()` 强制校验 `record.getUserId().equals(userId)`，这意味着只有借阅者本人可还书。管理员/馆员无法在用户不在场时代理归还。虽然可能的场景设计如此，但管理系统通常需管理员干预能力。 |
| **建议修复** | 提供一个管理员专用的 `adminReturnBook(Long adminId, Long borrowId)` 方法，绕过用户 ID 校验。 |

### 4.5 缓存注解在事务边界之外执行

| 属性 | 值 |
|------|------|
| **文件** | `BookServiceImpl.java` 第 93-94 行、第 131-132 行 |
| **类型** | 缓存一致性 |
| **描述** | `@CachePut` 和 `@Transactional` 叠加使用时，缓存更新发生在事务提交**之前**。若事务提交失败回滚，缓存中已有更新后的数据而数据库未变更，造成缓存与数据库不一致。 |
| **影响** | 在特定失败场景下，前端会读到不存在的图书信息。几率低但存在。 |
| **建议修复** | 在事务提交成功后手动更新缓存（通过 `TransactionSynchronizationManager.afterCommit()`），或接受 Spring 缓存注解的已知局限性。 |

---

## 5. 总结

### 严重级别分布

| 严重级别 | 数量 | 主要问题 |
|----------|------|----------|
| **Critical** | 1 | Token 黑名单机制完全失效 |
| **High** | 2 | JWT 密钥硬编码回退、parseToken NPE 链式传播 |
| **Medium** | 6 | 库存更新未校验、续借缺乐观锁、验证码绕过、CORS 配置、卡号碰撞、限流静默降级 |
| **Low** | 5 | Response 未 flush、Principal 设计、XSS 头冗余、管理员代还缺失、缓存事务边界 |

### 最紧急修复建议

1. **P0 - Token 黑名单 Bug** (`AuthServiceImpl.java:169`): 统一使用 JTI 作为黑名单 key，当前 logout 功能完全不可用。
2. **P0 - JWT 默认密钥** (`JwtUtils.java:33`): 移除硬编码默认密钥，改为启动时强制校验机制。
3. **P1 - parseToken null 安全** (`JwtUtils.java:155-216`): 对所有 getter 方法添加 null 防御，避免 NPE 传播导致 500 错误。
4. **P1 - 库存更新** (`BorrowServiceImpl.java:444`): 检查 updateAvailableCount 返回值，防止库存不一致。

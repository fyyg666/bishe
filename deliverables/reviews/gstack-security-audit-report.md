# GStack Security Audit Report — Library Management System v2

## Meta

- **Audit mode**: Comprehensive
- **Date**: 2026-05-15
- **Scope**: Full codebase — backend (Spring Boot 3.2.5), frontend (Vue 3.4), infrastructure (Docker, Nginx, MySQL, Redis)
- **Framework**: STRIDE Threat Model + OWASP Top 10 (2021)
- **Confidence gate**: ≥ 2/10

---

## Executive Summary

The Library Management System demonstrates strong security fundamentals — JWT-based stateless authentication, XSS input filtering, CSRF token support, rate limiting via Redis sliding window, account lockout mechanisms, and well-structured RBAC authorization annotations. However, **10 findings** were identified, including **2 critical** and **3 high-severity** issues. The most urgent finding is an **IDOR vulnerability** (F-001) in the reader detail endpoint that allows any authenticated user to enumerate all user PII data. Additionally, the **frontend JWT cookie lacks HttpOnly** (F-002), making tokens readable by JavaScript in an XSS scenario. The default password `123456` (F-004) and the **optional captcha bypass** (F-003) further weaken authentication.

---

## Architecture Overview

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│   Frontend    │────▶│   Nginx      │────▶│   Backend     │
│  Vue 3 + EP   │     │  Reverse     │     │ Spring Boot   │
│  (Port 80)    │     │  Proxy       │     │ (Port 8080)   │
└──────────────┘     └──────────────┘     └───────┬───────┘
                                                   │
                    ┌───────────────────────────────┤
                    │                               │
                    ▼                               ▼
            ┌──────────────┐               ┌──────────────┐
            │   MySQL 8    │               │   Redis 7    │
            │  library_sys │               │  + Redisson  │
            └──────────────┘               └──────────────┘
```

**Trust Boundaries**:
- TB1: Browser ↔ Nginx (no HTTPS in default config)
- TB2: Nginx ↔ Backend (internal bridge network)
- TB3: Backend ↔ Database/Redis (internal network, plaintext auth fallback)

**Entry Points**:
- `/auth/login`, `/auth/register`, `/auth/refresh` — public
- `/books/**`, `/seats/**` — public GET access
- `/readers/**` — authenticated (role-gated for mutations)
- `/actuator/**` — role-gated
- `/captcha` — public
- Swagger UI — public in non-prod profiles

---

## Findings by STRIDE Category

---

### S — Spoofing (身份欺骗)

#### [F-001] 🔴 关键 IDOR — 任意用户可枚举所有读者的个人隐私信息

| Field | Value |
|-------|-------|
| **OWASP** | A01: Broken Access Control |
| **STRIDE** | Spoofing + Information Disclosure |
| **Severity** | 🔴 Critical |
| **Confidence** | 10 |
| **Location** | `ReaderController.java:74-79` |

**Description**:
The `GET /readers/{id}` endpoint in `ReaderController.getReaderById()` has **no authorization annotation** (`@PreAuthorize`). Any authenticated user (READER, VOLUNTEER, LIBRARIAN, or ADMIN) can query any reader's full profile by numeric ID, including phone number, email, real name, card number, credit score, status, and borrow count.

**Exploit Scenario**:
1. Attacker registers as a normal READER user
2. Calls `GET /readers/1` → gets admin's phone, email, real name
3. Calls `GET /readers/2` → gets librarian's details
4. Enumerates IDs sequentially to harvest all user PII

**Comparison**: Contrast with `PUT /readers/{id}` which properly checks ownership + role, and `GET /readers` which requires ADMIN/LIBRARIAN.

**Reproduction**:
```bash
# As a READER user (JWT in Authorization header)
curl -X GET http://localhost:8080/api/v1/readers/1 \
  -H "Authorization: Bearer <reader-jwt>"
# → Returns full user profile with phone, email, realName, cardNumber
```

**Remediation**:
```java
// Option A: Only ADMIN/LIBRARIAN can view any reader
@GetMapping("/{id}")
@PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
public ApiResponse<ReaderResponse> getReaderById(@PathVariable Long id) { ... }

// Option B: Allow READER to view own profile only
@GetMapping("/{id}")
@PreAuthorize("isAuthenticated()")
public ApiResponse<ReaderResponse> getReaderById(@PathVariable Long id, Authentication auth) {
    Long currentUserId = getUserIdFromAuthentication(auth);
    if (!currentUserId.equals(id) && !isAdmin(auth)) {
        throw new ForbiddenException(...);
    }
    ...
}
```

---

#### [F-002] 🟠 中危 前端 JWT Cookie 未设置 HttpOnly 标志

| Field | Value |
|-------|-------|
| **OWASP** | A05: Security Misconfiguration |
| **STRIDE** | Spoofing |
| **Severity** | 🟠 High |
| **Confidence** | 10 |
| **Location** | `frontend/src/utils/auth.js:13-17` |

**Description**:
The frontend stores JWT access tokens and refresh tokens in cookies using `js-cookie` **without the `HttpOnly` flag**. The code itself acknowledges this in the comments (lines 119-120): _"当前 CSRF Token 和 Refresh Token 通过前端 JS 管理 Cookie，未设置 HttpOnly 属性。"_ This means any XSS vulnerability would allow an attacker to steal both tokens.

**Exploit Scenario**:
1. Attacker finds any XSS vector (even a minor one)
2. Executes `document.cookie` to extract `library_token` and `library_refresh_token`
3. Uses the stolen tokens to impersonate the victim indefinitely

**Remediation**:
```javascript
// Backend must set HttpOnly cookies via Set-Cookie header
// Frontend should NOT manage tokens via JS at all
// Recommended approach:
// 1. Backend sets cookies with: Set-Cookie: library_token=<jwt>; HttpOnly; Secure; SameSite=Lax
// 2. Frontend removes all token management from auth.js
// 3. Axios config: withCredentials: true (already set)
```

---

#### [F-003] 🟠 中危 验证码可绕过 — captchaKey/captchaCode 均为 null 时跳过校验

| Field | Value |
|-------|-------|
| **OWASP** | A07: Identification and Authentication Failures |
| **STRIDE** | Spoofing |
| **Severity** | 🟠 High |
| **Confidence** | 10 |
| **Location** | `AuthServiceImpl.java:304-307` |

**Description**:
In `AuthServiceImpl.validateCaptcha()`, if both `captchaKey` and `captchaCode` are `null`, the method returns immediately without validation. This means any attacker can bypass captcha entirely by simply omitting these fields from the login request. The rate limiter (5 requests/minute) partially mitigates brute-force, but captcha is a defense-in-depth layer that is trivially bypassed.

**Exploit Scenario**:
1. Attacker sends login requests without captcha
2. Server skips captcha validation entirely
3. Only the 5/min login rate limit and 5-attempt account lockout remain as defense
4. Since rate limiter degrades to passthrough when Redis is unavailable, there's a bypass path

**Reproduction**:
```json
// Login WITHOUT captcha — bypasses validation
POST /api/v1/auth/login
{
  "username": "admin",
  "password": "admin123"
}
```

**Remediation**:
```java
// Make captcha mandatory for login
private void validateCaptcha(String captchaKey, String captchaCode) {
    if (captchaKey == null || captchaCode == null) {
        throw new BusinessException(ErrorCode.AUTH_FAILED, "验证码不能为空");
    }
    // ... existing validation logic
}
```

---

#### [F-004] 🟠 中危 默认重置密码为弱密码 "123456"

| Field | Value |
|-------|-------|
| **OWASP** | A07: Identification and Authentication Failures |
| **STRIDE** | Spoofing |
| **Severity** | 🟠 High |
| **Confidence** | 10 |
| **Location** | `Constants.java:233-234`, `ReaderServiceImpl.java:265-277` |

**Description**:
`Constants.Security.DEFAULT_PASSWORD = "123456"` defines a weak 6-character default password that does **not** meet the minimum password complexity requirements enforced by `RegisterRequest` (which requires 8+ chars including uppercase, lowercase, digit, and special char). The `resetPassword()` method at `ReaderServiceImpl.java:273` uses this default. This is a direct conflict — the system rejects weak passwords during registration but then resets accounts to a weaker password.

Additionally, the admin account's SQL seed password comment says "(密码: admin123)" — a bcrypt hash, but the plaintext is documented.

**Exploit Scenario**:
1. Admin resets a reader's password via `POST /readers/{id}/reset-password`
2. Reader's password is set to "123456"
3. Anyone who knows the default can log in as that user

**Remediation**:
```java
// In Constants.java:
public static final String DEFAULT_PASSWORD = "Library@2026Reset";

// Or better: require the admin to set a new password at reset time
// In ReaderServiceImpl.resetPassword():
public void resetPassword(Long id) {
    // Generate a random strong password
    String tempPassword = generateSecureTemporaryPassword();
    user.setPassword(passwordEncoder.encode(tempPassword));
    // Send temp password via secure channel (email/SMS)
    // Force password change on next login
}
```

---

### T — Tampering (数据篡改)

#### [F-005] 🟡 低危 XSS 过滤基于黑名单正则，存在绕过风险

| Field | Value |
|-------|-------|
| **OWASP** | A03: Injection |
| **STRIDE** | Tampering |
| **Severity** | 🟡 Medium |
| **Confidence** | 6 |
| **Location** | `XssRequestWrapper.java:216-266`, `XssFilter.java:42-47` |

**Description**:
The XSS filter uses a regex-based blacklist approach (removing `javascript:`, event handlers, `<script>`, etc.) rather than a whitelist/allowed-list approach. Blacklist-based XSS filtering is notoriously bypassable. Attackers can use:
- HTML entity encoding bypass: `&#106;avascript:alert(1)`
- Unicode normalization: `javascript\uFF1Aalert(1)`
- Nested encodings that the regex doesn't catch

The filter uses `cn.hutool.http.HtmlUtil.filter()` as a first pass, then regex removal as a second pass. While better than nothing, this is not sufficient for production-grade XSS defense.

**Note**: The project does use Content Security Policy (CSP) headers (`default-src 'self'; script-src 'self' 'unsafe-inline'`), which provides a second layer. However, `'unsafe-inline'` weakens CSP's XSS protection (see F-006).

**Remediation**:
```java
// Replace blacklist regex with a whitelist approach:
// 1. Use OWASP Java HTML Sanitizer (https://owasp.org/www-project-java-html-sanitizer/)
// 2. Or use Jsoup.clean() with a whitelist:
//    import org.jsoup.Jsoup;
//    import org.jsoup.safety.Safelist;
//    String safe = Jsoup.clean(input, Safelist.basic());
// 3. Tighten CSP: remove 'unsafe-inline' from script-src
```

---

#### [F-006] 🟡 低危 CSP 中 `'unsafe-inline'` 削弱 XSS 保护

| Field | Value |
|-------|-------|
| **OWASP** | A05: Security Misconfiguration |
| **STRIDE** | Tampering |
| **Severity** | 🟡 Medium |
| **Confidence** | 8 |
| **Location** | `SecurityConfig.java:125-126` |

**Description**:
The Content-Security-Policy header includes `'unsafe-inline'` for both `script-src` and `style-src`. This allows inline script execution, which **defeats the primary purpose of CSP** — preventing XSS via inline scripts. While Vue.js requires inline scripts during development, production builds can avoid `unsafe-inline` by using nonces or hashes.

**Remediation**:
```java
// For production profile:
// Content-Security-Policy: default-src 'self'; 
//   script-src 'self'; 
//   style-src 'self' 'unsafe-inline';  // unsafe-inline is often needed for CSS
//   img-src 'self' data:;
//   connect-src 'self'

// Use nonce-based approach for scripts:
// .addHeaderWriter(new StaticHeadersWriter(
//   "Content-Security-Policy", 
//   "default-src 'self'; script-src 'nonce-{random}'; style-src 'self' 'unsafe-inline'; ..."))
```

---

### R — Repudiation (否认抵赖)

#### [F-007] 🟢 信息 安全审计日志仅为本地文件，缺乏集中式监控

| Field | Value |
|-------|-------|
| **OWASP** | A09: Security Logging and Monitoring Failures |
| **STRIDE** | Repudiation |
| **Severity** | 🟢 Low |
| **Confidence** | 8 |
| **Location** | `SecurityAuditLogger.java:35-37`, `application.yml` |

**Description**:
The `SecurityAuditLogger` writes to a dedicated `SECURITY_AUDIT` logger, but there is no centralized SIEM integration, no log aggregation, and no alerting for security events. In a Docker Compose deployment with `restart: unless-stopped`, logs are lost when containers restart unless properly mounted as volumes. The production profile writes to `./logs/application.log`, which is a relative path and may not exist.

Additionally, there's no evidence of automated alerting on security events like:
- Multiple login failures
- Account lockouts
- Rate limiter triggers
- XSS detection events

**Remediation**:
```yaml
# In docker-compose.yml, mount logs as volumes:
services:
  backend:
    volumes:
      - ./logs:/app/logs

# In application.yml (prod):
logging:
  file:
    name: /app/logs/application.log
  logback:
    rollingpolicy:
      max-history: 30
      max-file-size: 100MB
```

---

### I — Information Disclosure (信息泄露)

#### [F-008] 🟡 低危 Actuator `/actuator/health` 和 `/actuator/health/**` 完全公开

| Field | Value |
|-------|-------|
| **OWASP** | A05: Security Misconfiguration |
| **STRIDE** | Information Disclosure |
| **Severity** | 🟡 Low |
| **Confidence** | 9 |
| **Location** | `SecurityConfig.java:153` |

**Description**:
`/actuator/health` is permissive to everyone (no authentication required), while `/actuator/**` requires ADMIN/LIBRARIAN roles. However, `health-show-details` is set to `when_authorized`, which is correct. The issue is minor — health endpoints are commonly exposed for monitoring — but the `/actuator/prometheus` endpoint, while requiring ADMIN/LIBRARIAN role, exposes internal metrics that could aid attackers in reconnaissance.

**Remediation**:
Keep as-is, but ensure the Actuator endpoints are behind the Nginx layer in production and not directly exposed to the internet.

#### [F-009] 🟡 低危 MySQL 默认连接禁用 SSL

| Field | Value |
|-------|-------|
| **OWASP** | A02: Cryptographic Failures |
| **STRIDE** | Information Disclosure |
| **Severity** | 🟡 Low |
| **Confidence** | 9 |
| **Location** | `application.yml:21` |

**Description**:
The default `application.yml` (line 21) includes `useSSL=false` in the MySQL JDBC connection string. This means database traffic between the backend and MySQL is transmitted in plaintext. In Docker Compose deployments, databases run on a shared bridge network, but in multi-host or cloud deployments, this exposes database credentials and all data to network sniffing.

The production profile (line 167) correctly sets `useSSL=true`, but it's not the default.

**Remediation**:
```yaml
# In default application.yml:
url: jdbc:mysql://localhost:3306/library_system?useUnicode=true&characterEncoding=utf-8&useSSL=true&requireSSL=true&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
```

---

### D — Denial of Service (拒绝服务)

#### [F-010] 🟡 低危 Redis 不可用时限流降级为透传模式

| Field | Value |
|-------|-------|
| **OWASP** | A04: Insecure Design |
| **STRIDE** | Denial of Service |
| **Severity** | 🟡 Medium |
| **Confidence** | 8 |
| **Location** | `RateLimitFilter.java:219-225`, `RateLimitFilter.java:280-283` |

**Description**:
When Redis is unavailable, both the generic rate limiter and the login rate limiter degrade to passthrough mode with error-level log messages. This means a **Redis outage directly disables all rate limiting**, including login brute-force protection. An attacker who can trigger a Redis disconnect (e.g., via resource exhaustion or network disruption) can then brute-force passwords without rate limiting.

Additionally, the `RateLimitFilter` is injected with `@Autowired(required = false)` in `SecurityConfig.java:52`, meaning if the bean fails to initialize, the filter is simply not added (line 172-174).

**Exploit Scenario**:
1. Attacker finds a way to exhaust Redis connections or cause a Redis crash
2. Rate limiting silently degrades to passthrough
3. Attacker brute-forces login without any rate limit restriction

**Remediation**:
```java
// Option A: Use Caffeine as a local fallback when Redis is down
private final Cache<String, AtomicInteger> localRateCache = 
    Caffeine.newBuilder()
        .expireAfterWrite(1, TimeUnit.MINUTES)
        .maximumSize(10000)
        .build();

// Option B: Rate limiting should deny-by-default on Redis failure, not allow
// No - this would cause availability issues. Better to log aggressively and alert.
// Option C: Implement a hybrid approach with local cache fallback
```

---

#### [F-011] 🟢 信息 操作日志存储完整请求参数（含敏感数据）

| Field | Value |
|-------|-------|
| **OWASP** | A09: Security Logging and Monitoring Failures |
| **STRIDE** | Information Disclosure |
| **Severity** | 🟢 Info |
| **Confidence** | 7 |
| **Location** | `sql/init.sql:247` (`sys_operation_log.params` uses `TEXT`) |

**Description**:
The `sys_operation_log.params` column stores request parameters as JSON TEXT. If the `RequestLoggingAspect` logs request bodies that include passwords or JWT tokens, these would be persisted in the database in plaintext, accessible to anyone with database read access.

**Remediation**:
```java
// In the logging aspect, mask sensitive fields before logging:
// - Mask "password" fields
// - Mask "oldPassword", "newPassword"
// - Mask "Authorization" headers
// - Truncate long token strings
```

---

#### [F-012] 🟡 低危 Refresh Token 没有轮换和旧令牌吊销

| Field | Value |
|-------|-------|
| **OWASP** | A07: Identification and Authentication Failures |
| **STRIDE** | Spoofing |
| **Severity** | 🟡 Low |
| **Confidence** | 7 |
| **Location** | `AuthServiceImpl.java:136-164` |

**Description**:
The `refreshToken()` method issues a new access token + refresh token pair but does **not** revoke the old refresh token. This means stolen refresh tokens remain valid for up to 7 days even after the legitimate user has performed a token refresh. An attacker with a stolen refresh token can continue to obtain new access tokens indefinitely until the token expires.

**Exploit Scenario**:
1. Attacker steals refresh token (via XSS — see F-002)
2. Victim refreshes their token (gets new pair)
3. Attacker's stolen token is STILL VALID for 7 days
4. Attaker continuously refreshes to maintain access

**Remediation**:
```java
public LoginResponse refreshToken(String refreshToken) {
    // ... existing validation ...
    
    // Revoke the old refresh token
    String oldJti = jwtUtils.getJtiFromToken(refreshToken);
    String oldBlacklistKey = Constants.Token.BLACKLIST_PREFIX + oldJti;
    // Calculate remaining TTL of old token and add to blacklist
    long remainingTtl = getTokenRemainingTtl(refreshToken);
    if (remainingTtl > 0) {
        tokenBlacklistService.addToBlacklist(oldBlacklistKey, remainingTtl);
    }
    
    // ... generate new tokens ...
}
```

---

## Security Posture Score

| Severity | Count |
|----------|-------|
| 🔴 Critical | 1 |
| 🟠 High | 3 |
| 🟡 Medium | 3 |
| 🟡 Low | 4 |
| 🟢 Info | 1 |
| **Overall** | **C — Needs Improvement** |

---

## STRIDE Category Summary

| Category | Count | Top Issue |
|----------|-------|-----------|
| **S**poofing | 4 | F-001: IDOR on reader details + F-002: HttpOnly missing |
| **T**ampering | 2 | F-005: XSS regex bypass + F-006: CSP unsafe-inline |
| **R**epudiation | 1 | F-007: No centralized monitoring |
| **I**nformation Disclosure | 2 | F-008: Health endpoint public + F-009: No DB SSL |
| **D**enial of Service | 2 | F-010: Rate limit degradation + F-011: Log sensitivity |
| **E**levation of Privilege | 0 | RBAC implementation is sound |

---

## Remediation Roadmap

### P0 — Immediate (this sprint)
1. **F-001**: Add `@PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")` to `ReaderController.getReaderById()` — or implement ownership check
2. **F-002**: Move JWT token handling from frontend JS cookies to backend HttpOnly `Set-Cookie` headers
3. **F-003**: Make captcha mandatory for login (remove the null-bypass path)

### P1 — Next sprint
4. **F-004**: Change default password to a strong randomly-generated value, or require admin to set password at reset time
5. **F-010**: Implement a local (Caffeine) fallback rate limiter when Redis is unavailable
6. **F-012**: Implement refresh token rotation with old token revocation

### P2 — Next release
7. **F-005**: Upgrade XSS filter from regex blacklist to OWASP HTML Sanitizer whitelist
8. **F-009**: Enable MySQL SSL by default (set `useSSL=true` in default application.yml)
9. **F-006**: Remove `'unsafe-inline'` from CSP script-src using nonce-based approach

### P3 — Backlog
10. **F-007**: Integrate with centralized log aggregation and set up alerting rules
11. **F-011**: Add sensitive data masking in operation log aspect
12. **F-008**: Review actuator endpoint exposure for production

---

## Trend Comparison

| Metric | Value |
|--------|-------|
| First audit | Yes (baseline) |
| Previous finding count | N/A |
| New findings | 12 |
| Resolved | 0 |
| Regressions | N/A |

---

## Positive Security Practices Observed

The system has several well-implemented security controls worth noting:

1. ✅ **BCryptPasswordEncoder** for password hashing (not MD5/SHA)
2. ✅ **JWT with JTI (jti claim)** for token blacklisting support
3. ✅ **Refresh token rotation** (partially — missing revocation of old tokens)
4. ✅ **Redis-based sliding window rate limiter** with separate login limits
5. ✅ **Account lockout** after 5 failed attempts (15-minute lock)
6. ✅ **XSS filter** with JSON body support and request wrapper
7. ✅ **CSRF token** support in frontend requests
8. ✅ **Security headers**: HSTS, X-Frame-Options, X-Content-Type-Options, Referrer-Policy
9. ✅ **Parameterized queries** via MyBatis-Plus (no raw SQL injection risk)
10. ✅ **Global exception handler** hides internal errors from clients
11. ✅ **Input validation** with Jakarta Bean Validation annotations
12. ✅ **Distributed locking** (Redisson) for concurrent borrow operations
13. ✅ **Bloom filter** for cache penetration prevention
14. ✅ **Two-level caching** (Caffeine + Redis) for performance
15. ✅ **Data masking** for phone and email in AuthService responses

---

*Report generated by GStack Chief Security Officer (gstack-security-officer)*
*Framework: OWASP Top 10 (2021) + STRIDE Threat Model*

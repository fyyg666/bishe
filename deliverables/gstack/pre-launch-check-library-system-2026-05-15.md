# 上线前全检报告 — 图书馆管理系统 v2.0.0

**日期**：2026-05-15
**场景**：上线前全检（代码审查 + 安全审计 + QA测试）
**参与成员**：产品评审员（gstack-product-reviewer）+ 安全官（gstack-security-officer）+ QA与发布负责人（gstack-qa-lead）
**项目路径**：`c:\Users\12856\Desktop\论文实现\library-system-v2`
**技术栈**：Spring Boot 3.2.5 / Vue 3.4 + Element Plus / MyBatis-Plus / MySQL 8 / Redis 7 / Docker Compose

---

## 📌 TL;DR（执行摘要）

- **整体结论**：🔴 **不通过 — 阻塞项未清零前严禁上线**
- **阻塞项数量**：6 个（🔴 严重）
- **健康评分**：QA 62/100
- **核心问题**：注册功能完全不可用、密码修改 404、图书搜索参数不匹配、Reader IDOR 泄露全体用户 PII、Cookie 未 HttpOnly 可被 XSS 窃取、JWT Token 类型未校验可 Refresh 冒充 Access
- **下一步**：优先修复 6 个 P0 阻塞项，修复后需回归验证的模块：**认证 → 图书搜索 → 读者管理**，一个接一个验证

---

## 🎯 核心结论卡片

| 项目 | 内容 |
|------|------|
| Go / No-Go | 🔴 **No-Go** |
| 严重度分布 | 🔴 6 / 🟠 6 / 🟡 6 / 🟢 4 |
| 关键行动项 | 12 条 |
| 建议负责人 | 后端 2 人 + 前端 1 人 |
| 预估修复周期 | 2-3 个工作日 |

---

## 1. 各成员核心结论

### 🔍 产品评审员（代码审查）
- **核心判断**：代码架构扎实，但存在 3 个 P0 严重问题（Cookie 未 HttpOnly、JWT Token 类型未校验、prod profile 重复定义），另有前后端契约不一致（搜索参数、字段映射）和 Refresh Token 未轮换
- **亮点**：已完成 XSS 过滤、JWT 双Token、布隆过滤器、分布式锁、滑动窗口限流等安全基建
- **关键建议**：优先修复 Token 类型校验和 Cookie HttpOnly，这两个直接关系到系统安全基线

### 🛡️ 安全官（OWASP + STRIDE 审计）
- **核心判断**：安全控制基础良好（BCrypt、JWT/JTI、参数化查询、安全头等 15 项正面控制），但 IDOR 漏洞（`/readers/{id}` 无权限校验）是最严重的安全问题，允许任意枚举全体用户 PII
- **关键建议**：P0 三连 — IDOR 加 `@PreAuthorize`、Cookie 迁移到后端 HttpOnly、验证码不可绕过

### ✅ QA与发布负责人（系统化 QA 测试）
- **核心判断**：**3 个关键功能完全不可用**（注册、改密码、图书搜索），核心功能链断裂。健康评分仅 62/100，DO NOT SHIP
- **关键建议**：前后端契约对齐是最大短板，三个 critical bug 全部是前端-后端参数/端点不匹配导致

---

## 2. 综合审查发现（去重合并后按严重度排序）

| # | 严重度 | 类别 | 位置 | 问题描述 | 建议 | 来源成员 |
|---|--------|------|------|---------|------|---------|
| 1 | 🔴 | 功能阻断 | `Register.vue` L183 + `RegisterRequest.java` | 前端 `delete submitData.confirmPassword` 删除了 confirmPassword，但后端 DTO 有 `@NotBlank` 校验 → **注册永远 400 失败** | 前端停止删除 confirmPassword，或后端去掉 `@NotBlank` | QA门神 |
| 2 | 🔴 | 功能阻断 | `auth.js` L36-42 + `ReaderController.java` L154-170 | 前端调 `PUT /auth/password`，后端实际端点 `POST /readers/{id}/password` → **修改密码永远 404** | 统一端点路径和方法 | QA门神 |
| 3 | 🔴 | 功能阻断 | `BookList.vue` L219-223 + `BookController.java` L53-56 | 前端发 `{name, author, category}`，后端收 `{keyword, categoryId}` → **图书搜索/筛选完全失效** | 前端将参数映射为后端期望的 keyword/categoryId | 产品评审员 + QA门神 |
| 4 | 🔴 | 安全-IDOR | `ReaderController.java` L74-79 | `GET /readers/{id}` 无 `@PreAuthorize`，任意认证用户可枚举全体用户 PII（手机、邮箱、真实姓名） | 加 `@PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")` 或所有权检查 | 安全官 |
| 5 | 🔴 | 安全-Cookie | `frontend/src/utils/auth.js` L13-17 | Token 存储在 JS 可读的 Cookie 中，未设置 HttpOnly → XSS 可窃取 Token 实现账户接管 | 后端通过 `Set-Cookie` HttpOnly 头设置 Cookie，前端移除 Token 管理 | 产品评审员 + 安全官 |
| 6 | 🔴 | 安全-认证 | `JwtFilter.java` L58-98 | `JwtFilter` 未校验 Token type claim，Refresh Token（7 天有效）可直接当 Access Token 使用 | 在 JwtFilter 中添加 token type == "ACCESS" 校验 | 产品评审员 |
| 7 | 🟠 | 配置安全 | `application.yml` L161-L249 | prod profile 重复定义，Swagger 禁用配置可能失效 | 合并两个 prod profile | 产品评审员 |
| 8 | 🟠 | 安全-验证码 | `AuthServiceImpl.java` L304-307 | captchaKey/captchaCode 均为 null 时跳过验证码校验 → 验证码可绕过 | 增加 null 检查，验证码不可为空 | 安全官 |
| 9 | 🟠 | 安全-弱密码 | `Constants.java` L233 + `ReaderServiceImpl.java` L265-277 | 默认重置密码 `"123456"` 不符合注册时的密码复杂度要求（8位+大小写+特殊字符） | 改为随机强密码或管理员输入 | 安全官 |
| 10 | 🟠 | 安全-Token | `AuthServiceImpl.java` L136-164 | Refresh Token 刷新时未将旧 Token 加入黑名单 → 重放攻击 | 刷新时通过 JTI 将旧 Refresh Token 加入黑名单 | 产品评审员 + 安全官 |
| 11 | 🟠 | 权限配置 | `SecurityConfig.java` L156-158 | `/categories` GET 端点未加入 `permitAll()`，匿名用户访问返回 401 | 在 permitAll 列表中添加 `GET /categories` | QA门神 |
| 12 | 🟠 | 配置 | `frontend/` | 缺少 `.env`、`.env.development`、`.env.production` 环境配置 | 创建环境配置文件 | QA门神 |
| 13 | 🟡 | API文档 | `application.yml` springdoc | Swagger 分组路径不匹配实际 Controller 路径（如 `/borrow-records/**` vs `/borrows/**`） | 修正 springdoc group-configs 路径 | QA门神 |
| 14 | 🟡 | 安全-CSP | `SecurityConfig.java` L125-126 | CSP `script-src` 含 `'unsafe-inline'`，削弱 XSS 保护 | 生产环境用 nonce 方式替代 | 安全官 |
| 15 | 🟡 | 数据一致性 | `AuthServiceImpl.java` L52, L103 | `@Transactional` 混用 MySQL + Redis，Redis 失败时数据不一致 | 将 Redis 操作移到事务外部 | 产品评审员 |
| 16 | 🟡 | 安全-限流 | `RateLimitFilter.java` L219-225 | Redis 不可用时率限制降级为透传 | 用 Caffeine 做本地降级缓存 | 安全官 |
| 17 | 🟡 | 前后端校验 | `Register.vue` L140 + `RegisterRequest.java` | 前端密码最小 6 位，后端要求 8 位+特殊字符，一致性差 | 统一为后端标准 | QA门神 |
| 18 | 🟡 | 字段映射 | `BookList.vue` L121-123 | 前端用 `row.stock` 判断库存，后端返回字段为 `availableCount` | 改为 `row.availableCount` | 产品评审员 |
| 19 | 🟢 | 建议 | `XssRequestWrapper.java` L88, L149 | `isJsonRequest()` 调用被覆盖的 `getHeader()`，性能浪费 | 改为 `super.getHeader()` | 产品评审员 |
| 20 | 🟢 | 建议 | `PasswordChangeRequest.java` | 定义的 DTO 没有任何 Controller 使用（死代码） | 删除或对接现有 Controller | 产品评审员 |
| 21 | 🟢 | 建议 | `RateLimitFilter.java` 等 | 分页参数无上限限制、硬编码字符串多处 | 抽取常量，加 `@Max` 校验 | 产品评审员 |
| 22 | 🟢 | 建议 | 全项目 | 后端零测试覆盖 | 添加 Spring Boot Test + Mockito 测试 | 产品评审员 |

---

## ✅ 行动清单

| # | 行动 | 负责方 | 紧急度 | 期望完成 |
|---|------|--------|--------|---------|
| 1 | **修复注册功能**：去掉前端 `delete confirmPassword` 或后端去掉 `@NotBlank` | 前端+后端 | P0 | 1h |
| 2 | **修复修改密码**：前端改为调 `POST /readers/{id}/password`，后端考虑提供统一的 `/auth/password` 端点 | 前端+后端 | P0 | 1h |
| 3 | **修复图书搜索**：前端将 `{name, author, category}` 映射为 `{keyword, categoryId}` | 前端 | P0 | 1h |
| 4 | **修复 IDOR**：`ReaderController.getReaderById()` 加 `@PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")` | 后端 | P0 | 0.5h |
| 5 | **Cookie HttpOnly**：后端在登录/刷新响应中通过 `Set-Cookie` 头设置 HttpOnly Cookie，前端移除 Token Cookie 管理 | 后端+前端 | P0 | 2天 |
| 6 | **JWT Token 类型校验**：`JwtFilter` 中校验 token type == "ACCESS"，拒绝 REFRESH 类型 | 后端 | P0 | 0.5h |
| 7 | 合并 prod profile，修正 application.yml 中的重复定义 | 后端 | P1 | 0.5h |
| 8 | 修复 captcha 绕过：`validateCaptcha()` 增加 null 检查 | 后端 | P1 | 0.5h |
| 9 | Refresh Token 轮换：刷新时将旧 Token（JTI）加入黑名单 | 后端 | P1 | 1h |
| 10 | `/categories` 加入 SecurityConfig permitAll 列表 | 后端 | P1 | 0.3h |
| 11 | 前端创建 `.env.development` / `.env.production` 配置文件 | 前端 | P2 | 0.5h |
| 12 | 修复前端 `stock` → `availableCount` 字段映射 | 前端 | P2 | 0.3h |

---

## ⚠️ 待完善 / 已知局限

- **本次未覆盖集成测试**：所有分析基于静态代码审查和配置检查，未在真实运行环境中验证
- **依赖安全未扫描**：未使用 OWASP Dependency-Check 或 Snyk 扫描第三方库已知漏洞（如 Spring Boot 3.2.5、jjwt 0.12.6 等）
- **性能压测未执行**：未测试并发借阅场景下的分布式锁表现和 Redis 缓存穿透保护
- **Nginx 配置未审查**：未检查 `nginx/` 目录下的反向代理配置

---

## 📚 成员产出索引

- **gstack-product-reviewer（产品评审员）原始产出**：`c:\Users\12856\Desktop\论文实现\library-system-v2\review-report.md`
- **gstack-security-officer（安全官）原始产出**：`c:\Users\12856\Desktop\论文实现\library-system-v2\gstack-security-audit-report.md`
- **gstack-qa-lead（质量门神）原始产出**：`c:\Users\12856\Desktop\论文实现\library-system-v2\qa-report-library-system.md`

---

> 本报告由软件工坊 AI 协作生成，关键决策请由工程负责人复核。

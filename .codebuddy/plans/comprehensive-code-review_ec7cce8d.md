---
name: comprehensive-code-review
overview: 对 library-system-v2 全栈项目进行 5 维度全面代码审查：Bug与逻辑错误、性能优化、安全漏洞、可读性与可维护性、最佳实践，按严重程度分级输出审查报告。
todos:
  - id: tca-scan
    content: 使用 [mcp:TCA MCP Server] 启动全量代码自动化静态分析扫描，获取基线缺陷数据
    status: completed
  - id: verify-historical-fixes
    content: 对照历史审查报告关键问题清单，逐一验证修复状态并记录差异
    status: completed
  - id: security-audit
    content: 使用 [skill:Security Auditor] 对认证授权、输入校验、数据安全模块进行专项安全审计
    status: completed
  - id: backend-review
    content: 使用 [subagent:code-explorer] 分批审查后端Controller/Service/Config层代码，覆盖Bug、性能、最佳实践维度
    status: completed
    dependencies:
      - tca-scan
  - id: frontend-review
    content: 使用 [subagent:code-explorer] 审查前端Store/API/路由/View层代码，覆盖Bug、性能、安全维度
    status: completed
    dependencies:
      - tca-scan
  - id: infra-review
    content: 审查Docker/Nginx/CI配置的基础设施层面问题
    status: completed
  - id: synthesize-report
    content: 汇总所有发现，生成按严重程度分级的综合审查报告，包含修改建议和代码示例
    status: completed
    dependencies:
      - security-audit
      - backend-review
      - frontend-review
      - infra-review
      - verify-historical-fixes
---

## 用户需求

对 library-system-v2 图书馆管理系统进行全量、多维度的代码审查，输出结构化的审查报告。

## 审查范围

- **后端**：294个Java文件（36个Controller、42对Service接口/实现、15个Config、4个Interceptor/Filter、Entity/Mapper等）
- **前端**：124个文件（41个Vue页面、5个Pinia Store、10个API模块、15个组件、路由/工具函数）
- **基础设施**：Dockerfile、docker-compose、Nginx配置、GitHub Actions CI/CD

## 审查维度（5个核心维度）

1. **潜在Bug与逻辑错误**：边界条件、异常处理、空值引用、并发问题、事务边界
2. **性能优化**：内存泄漏、N+1查询、低效算法、缓存策略、不必要的计算/渲染
3. **安全漏洞**：注入风险（SQL/XSS/命令）、认证绕过、权限缺失、数据校验不足、敏感信息泄露
4. **可读性与可维护性**：命名规范、代码结构、模块解耦、注释质量、重复代码
5. **最佳实践**：设计模式运用、语言规范遵循、Spring Boot/Vue 3约定、错误处理模式

## 审查策略

- **先做差异分析**：对比历史审查报告（backend-bugs-report.md、frontend-bugs-report.md、gstack-security-audit-report.md），确认已发现问题是否已修复
- **再进行全新深度审查**：对关键模块逐文件审查，不依赖历史报告
- **按严重程度分级**：Critical（系统崩溃/安全绕过）> High（功能异常/数据不一致）> Medium（逻辑缺陷/性能退化）> Low（代码质量/风格问题）
- **每个问题提供**：问题描述 + 风险分析 + 具体文件路径和行号 + 修复建议 + 优化后代码示例

## 输出物

一份完整的代码审查报告（Markdown格式），包含问题分级汇总表和各维度详细分析。

## 技术方案

### 审查方法

本次审查采用"自动化工具扫描 + 人工深度审查"的双轨策略：

**第一轨：自动化扫描**

- 使用 TCA MCP Server 对全量代码进行静态分析扫描，自动检出常见缺陷模式
- 覆盖空指针、资源泄露、SQL注入、XSS、硬编码密钥等基础问题

**第二轨：人工深度审查**

- 由 Security Auditor 技能对关键安全模块（认证、授权、加密、输入校验）进行专项审计
- 对Controller/Service层逐模块审查业务逻辑、事务边界、并发安全
- 对前端Store/API/路由层审查状态管理、Token处理、权限控制

### 审查优先级

按模块风险等级排定审查深度：

- **P0 最高优先级**：AuthController/AuthService、JwtFilter、SecurityConfig、TokenBlacklist、RateLimiter、auth.js、permission.js、request.js
- **P1 高优先级**：所有Controller、所有ServiceImpl、Pinia Store、API模块
- **P2 中优先级**：Config类、Interceptor/Filter、工具类、Vue组件
- **P3 基础优先级**：Entity、Mapper、DTO、常量、Docker/Nginx配置

### 历史问题修复状态验证

从已有报告提取关键问题清单，逐一比对当前代码验证修复状态：

1. Token黑名单key不一致（backend-bugs-report: CRITICAL-1.1）
2. JWT默认密钥硬编码（backend-bugs-report: HIGH-2.1）
3. Cookie未设HttpOnly（review-report: CRITICAL-1）
4. IDOR漏洞-读者详情接口（gstack-security-audit: F-001）
5. Pinia孤立实例（frontend-bugs-report: BUG-01）

### 收敛边界

- 仅审查业务代码（backend/src/main/java、frontend/src），不审查测试代码和构建产物
- 引用历史报告中发现的问题时，需标注是否已修复及修复状态
- 新发现的问题需提供完整的上下文（文件路径+行号+代码片段）

## 使用的扩展能力

### MCP

- **TCA MCP Server**
- 用途：对项目代码执行自动化静态分析扫描，检出空指针、资源泄露、SQL注入、XSS等常见缺陷
- 预期结果：获取自动化扫描发现的问题列表，作为审查报告的基线数据

### Skill

- **Security Auditor**
- 用途：对认证模块（AuthController/AuthService/JwtFilter）、授权模块（SecurityConfig/权限注解）、数据安全（输入校验/SQL注入/XSS防护）进行专项安全审计
- 预期结果：输出安全漏洞清单，包含风险评级和修复方案

### SubAgent

- **code-explorer**
- 用途：对Controller层、Service实现层、前端关键模块分批次进行深度代码探索，提取关键代码片段用于人工审查
- 预期结果：获取每个模块的关键代码逻辑，识别潜在缺陷模式
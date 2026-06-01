# Project Memory — library-system-v2

## 项目约定
- 后端：Spring Boot 3.5.13, MyBatis-Plus, Redis, Caffeine L1缓存, JWT双Token, Redisson分布式锁
- 前端：Vue 3 (Composition API), Pinia, Element Plus, Axios, Vite
- API 前缀：`/api/v1`
- Token 存储：sessionStorage + BroadcastChannel (非Cookie)
- 数据库：MySQL 8.0

## 2026-06-01 全面代码审查与修复
- 审查发现 20 个问题：Critical 2, High 5, Medium 8, Low 5
- 全部 20 个问题已修复完毕
- 修改文件:
  - 后端: BookServiceImpl.java, AuthServiceImpl.java, Constants.java, ReaderServiceImpl.java, RegisterRequest.java, SecurityConfig.java, RateLimitFilter.java, XssFilter.java, CaffeineConfig.java
  - 前端: stores/user.js, utils/auth.js, api/auth.js, api/borrow.js, api/compensation.js, api/volunteer.js, api/suggestion.js, router/index.js
  - 配置: nginx/nginx.conf, docker-compose.yml
- 审查报告: comprehensive-code-review-2026-06-01.md

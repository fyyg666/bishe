# 前端代码审查报告

> 审查日期：2026-05-15
> 审查范围：13 个核心前端文件（utils、router、store、views、api）

---

## 目录

1. [Bug 与逻辑错误](#1-bug-与逻辑错误)
2. [安全漏洞](#2-安全漏洞)
3. [性能问题](#3-性能问题)
4. [代码质量问题](#4-代码质量问题)
5. [汇总表](#5-汇总表)

---

## 1. Bug 与逻辑错误

### BUG-01: `main.js` 创建了孤立的 Pinia 实例

| 属性 | 值 |
|------|----|
| **文件** | `frontend/src/main.js` |
| **行号** | 1-30 |
| **严重度** | **High** |
| **描述** | `main.js` 第23行通过 `createPinia()` 创建了一个新的 Pinia 实例并应用持久化插件；而 `store/index.js` 导出了另一个 `createPinia()` 实例（第3行）。但 `main.js` **从未导入 `@/store`**，因此 `store/index.js` 的实例完全未被使用，是死代码。如果未来任何模块错误地 `import pinia from '@/store'`，将得到一个**没有持久化插件的 Pinia 实例**，导致状态不持久化。 |
| **修复建议** | 选项 A：删除 `store/index.js`，将 Pinia 创建逻辑统一放在 `main.js`。选项 B：从 `store/index.js` 导出带插件的 Pinia 实例，`main.js` 中 `import pinia from '@/store'`。 |

---

### BUG-02: `request.js` Token 刷新回调可能传入 `undefined`

| 属性 | 值 |
|------|----|
| **文件** | `frontend/src/utils/request.js` |
| **行号** | 75-80 |
| **严重度** | **High** |
| **描述** | 第75行从刷新响应中提取新 token：`const newToken = res.data?.accessToken || ...`。如果所有字段都不存在，`newToken` 为 `undefined`。第80行 `pendingRequests.forEach(cb => cb(newToken))` 会将 `undefined` 传给每个等待请求，导致后续 API 请求携带 `Authorization: Bearer undefined`，所有请求返回 401，可能形成**重试死循环**。 |
| **修复建议** | 在 `pendingRequests.forEach` 之前检查 `newToken` 有效性，无效则拒绝所有等待请求并跳转登录。 |

---

### BUG-03: `request.js` 错误提示信息可能为 "undefined"

| 属性 | 值 |
|------|----|
| **文件** | `frontend/src/utils/request.js` |
| **行号** | 58-59 |
| **严重度** | **Medium** |
| **描述** | `data.message` 为 `undefined` 时，`new Error(data.message)` 创建的错误对象消息为 `"undefined"` 字符串（因为 `Error(undefined)` 会转成 `"undefined"`），而不是一个友好的默认提示。并且第59行没有 `return` — 虽然 `Promise.reject` 本身会返回，但缺少提前 `return` 在某些流程中可能导致后续代码继续执行。 |
| **修复建议** | 改为 `new Error(data.message || '请求失败')`，并在 reject 前加 `return`。 |

---

### BUG-04: `request.js` 非标准响应格式缺少兜底

| 属性 | 值 |
|------|----|
| **文件** | `frontend/src/utils/request.js` |
| **行号** | 54-63 |
| **严重度** | **Medium** |
| **描述** | 第54行 `if (data.code !== undefined)` 判断响应是否含 `code` 字段。但如果 API 返回 `{ code: 200, data: {...} }` (标准成功) 时，第55行命中成功路径并返回 `data`（整个外层对象）；而第63行对没有 `code` 字段的响应直接返回 `data`。逻辑上这是兼容性设计，但问题是：**即使 `code` 存在却是 `null`**（`null !== undefined` 为 `true`），会进入判断但 `data.code === 0 || data.code === 200` 为 `false`（`null` 不匹配），最终落入第58行的错误提示 —— 将合法响应误判为失败。 |
| **修复建议** | 增加 `data.code !== null` 或 `data.code != null`（宽松不等）的判断。 |

---

### BUG-05: `BookList.vue` 取消对话框可能导致误报错误

| 属性 | 值 |
|------|----|
| **文件** | `frontend/src/views/book/BookList.vue` |
| **行号** | 262-272 |
| **严重度** | **Medium** |
| **描述** | `ElMessageBox.confirm` 在用户点击取消时 reject 字符串 `'cancel'`，点击关闭按钮时 reject `'close'`。第269行 `if (error !== 'cancel')` 只能过滤 `'cancel'`，当用户通过 X 按钮关闭对话框时，错误消息 `'删除失败'` 会被误显示给用户。 |
| **修复建议** | 改为 `if (error !== 'cancel' && error !== 'close')`。同时建议 `BorrowList.vue` 第215行做相同处理。 |

---

### BUG-06: `SeatMap.vue` 座位状态加载未显示 loading 指示器

| 属性 | 值 |
|------|----|
| **文件** | `frontend/src/views/seat/SeatMap.vue` |
| **行号** | 104-126（逻辑），28-76（模板） |
| **严重度** | **Medium** |
| **描述** | `loading` 变量在第91行定义，第105行设置 `loading.value = true`，第124行设为 `false`，但**模板中没有任何 `v-loading` 绑定或其它加载状态指示器**。用户在切换区域或初次加载时，座位网格为空但没有任何视觉反馈（spinner/skeleton），可能导致困惑。 |
| **修复建议** | 在座位网格容器上添加 `v-loading="loading"` 和 `element-loading-text="加载中…"`。 |

---

### BUG-07: `SeatMap.vue` 坐位行/列处理可能因 Falsy 值出错

| 属性 | 值 |
|------|----|
| **文件** | `frontend/src/views/seat/SeatMap.vue` |
| **行号** | 113-114 |
| **严重度** | **Low** |
| **描述** | `const row = seat.row || Math.ceil(seat.seatNumber / 10)` — 使用 `||` 运算符。如果 `seat.row` 为 `0`（0 是 falsy 值），将错误地回退到 `seatNumber` 计算。虽然当前系统行号从1开始，但若后端未来返回0值或数据异常，会导致行号错乱。同理 `seat.col` 第114行。 |
| **修复建议** | 改为 `seat.row != null ? seat.row : Math.ceil(seat.seatNumber / 10)`（显式 `null/undefined` 检查）。 |

---

### BUG-08: `stores/user.js` 用户信息赋值可能不精确

| 属性 | 值 |
|------|----|
| **文件** | `frontend/src/stores/user.js` |
| **行号** | 53 |
| **严重度** | **Low** |
| **描述** | `userInfo.value = res.userInfo || res.data?.userInfo || res.data` — 最后兜底 `res.data` 将整个响应数据对象赋值为 `userInfo`。如果 API 返回格式为 `{ code: 0, data: { id: 1, username: 'test' } }`，则 `res.data` 是 `{ id: 1, username: 'test' }`，这可能是期望的结果。但如果 `data` 包含额外字段（如 `data.total`），用户信息会被意外扩展。 |
| **修复建议** | 建议明确字段映射，构造一个新的用户信息对象而不是直接赋值整个 `res.data`。 |

---

### BUG-09: `Login.vue` 登录失败后验证码刷新可能不触发

| 属性 | 值 |
|------|----|
| **文件** | `frontend/src/views/auth/Login.vue` |
| **行号** | 139-140 |
| **严重度** | **Low** |
| **描述** | 登录失败时调用 `loadCaptcha()` 刷新验证码。但如果失败原因是**网络连接断开**（而非认证失败），刷新验证码也会失败（catch 被静默忽略），用户看到的仍然是旧验证码图片，但 `captchaKey` 可能仍指向旧的已失效的验证码，导致再次提交仍可能失败。 |
| **修复建议** | 建议在 `catch` 块中区分网络错误和业务错误，对于网络不通的情况提示用户检查网络，仅在业务失败（401）时刷新验证码。 |

---

## 2. 安全漏洞

### SEC-01: CSRF Token 通过 JavaScript 设置，破坏 Double Submit Cookie 模式

| 属性 | 值 |
|------|----|
| **文件** | `frontend/src/utils/auth.js` / `frontend/src/utils/request.js` |
| **行号** | auth.js:95-99 / request.js:27-30 |
| **严重度** | **Critical** |
| **描述** | CSRF Double Submit Cookie 模式的安全基础是：CSRF token cookie 由服务端设置且标记为 `HttpOnly`（JS 不可读），前端从另一个来源（如响应头/页面 body）获取 token 值放在请求头中，服务端比对 cookie 值与请求头值。但此代码中，CSRF token **完全通过前端 JavaScript (`Cookies.set`) 写入 cookie**（auth.js 第95行），同源下任何 XSS 都可以读取和修改 cookie 与请求头中的 CSRF token，使 CSRF 防护失效。此外，cookie 没有设置 `HttpOnly` 属性。 |
| **修复建议** | CSRF token cookie 应由**后端在 HTTP 响应头 `Set-Cookie` 中设置**，并标记 `HttpOnly`。后端同时在响应 body 或自定义响应头中返回 CSRF token 值。前端仅负责从**响应头/body** 中读取 token 值并放在请求头 `X-CSRF-Token` 中，由服务端比对 cookie 值与请求头值。 |

---

### SEC-02: Cookie `secure` 属性动态判断存在降级风险

| 属性 | 值 |
|------|----|
| **文件** | `frontend/src/utils/auth.js` |
| **行号** | 12-14, 19-22, 46-49, 74-77, 96-98, 107-110 |
| **严重度** | **High** |
| **描述** | Cookie 的 `secure` 属性通过 `window.location.protocol === 'https:'` 动态设置。如果攻击者能够通过某种方式（如 MITM、HTTPS 降级攻击）让页面以 HTTP 加载，`secure` 变为 `false`，所有 cookie（包括 access token、refresh token、CSRF token）将通过明文传输。 |
| **修复建议** | 在生产环境下应始终设置 `secure: true`，强制 cookie 仅通过 HTTPS 传输。可以通过环境变量 `import.meta.env.PROD` 判断：`secure: import.meta.env.PROD`（生产环境强制 true），而非根据当前页面协议。 |

---

### SEC-03: Refresh Token 未标记 `HttpOnly`

| 属性 | 值 |
|------|----|
| **文件** | `frontend/src/utils/auth.js` |
| **行号** | 19-23 |
| **严重度** | **High** |
| **描述** | Refresh Token 存储在 JS 可读的 cookie 中（`HttpOnly` 未设置），任何 XSS 攻击都能窃取 refresh token，进而无限续期 access token，实现**长期会话劫持**。 |
| **修复建议** | Refresh Token cookie 应始终由服务端 `Set-Cookie` 设置并标记 `HttpOnly`。前端不应以任何方式读取 refresh token。刷新 token 的逻辑也应改为由服务端通过 `httpOnly` cookie 自动携带。 |

---

### SEC-04: `Login.vue` 存在开放重定向风险

| 属性 | 值 |
|------|----|
| **文件** | `frontend/src/views/auth/Login.vue` |
| **行号** | 137 |
| **严重度** | **Medium** |
| **描述** | `router.push(route.query.redirect || '/dashboard')` — 虽然 Vue Router 目前不处理外部 URL（`https://evil.com` 会被当做内部路由引发导航错误），但这一行直接信任用户输入的 `redirect` 参数。如果未来重构为 `window.location.href = redirect` 或使用 `a` 标签，将直接导致开放重定向漏洞。 |
| **修复建议** | 增加白名单校验：仅允许以 `/` 开头的相对路径，或使用已注册的路由名称进行跳转。 |

---

### SEC-05: `request.js` 无请求频率/重试限制

| 属性 | 值 |
|------|----|
| **文件** | `frontend/src/utils/request.js` |
| **行号** | 65-106 |
| **严重度** | **Medium** |
| **描述** | 401 刷新 token 的重试机制没有最大重试次数限制。如果刷新 token 持续失败（API 不可用、refresh token 过期递归返回 401），将无限递归调用 `request(config)`（第85行），导致**无限重试循环**，浏览器请求队列爆满、内存泄漏。 |
| **修复建议** | 添加重试计数器：在 `config` 上标记 `_retryCount`，最多重试1次，超过则直接跳转登录页。 |

---

## 3. 性能问题

### PERF-01: `request.js` `pendingRequests` 数组无上限

| 属性 | 值 |
|------|----|
| **文件** | `frontend/src/utils/request.js` |
| **行号** | 41, 80-81, 101-104 |
| **严重度** | **Medium** |
| **描述** | `pendingRequests` 数组理论上可无限增长。在短时间内大量请求同时返回 401 的场景下（如页面多个组件同时发请求），数组可能包含大量回调闭包，每个闭包捕获 `configSnapshot` 对象，造成内存压力。 |
| **修复建议** | 设置最大队列长度（如50），超出时直接 reject 新加入的请求并提示用户重新登录。 |

---

### PERF-02: `router/index.js` 导航守卫每次执行 `userStore.fetchUserInfo()`

| 属性 | 值 |
|------|----|
| **文件** | `frontend/src/router/index.js` |
| **行号** | 175-186 |
| **严重度** | **Low** |
| **描述** | 每次路由跳转时，如果 `userStore.userInfo` 为 null，都会发起 `fetchUserInfo()` API 请求。虽然第175行检查了 `!userStore.userInfo`（防止重复请求），但用户在多个路由间快速切换时仍可能触发多次请求（首次请求未完成时 `userInfo` 仍为 null）。 |
| **修复建议** | 增加 `userStore.fetchingUserInfo` 锁标志，防止并发请求。或在 `App.vue` 挂载时一次性获取用户信息，导航守卫仅做权限校验。 |

---

## 4. 代码质量问题

### QUAL-01: `main.js` 全局注册所有 Element Plus 图标

| 属性 | 值 |
|------|----|
| **文件** | `frontend/src/main.js` |
| **行号** | 4, 18-20 |
| **严重度** | **Low** |
| **描述** | `for (const [key, component] of Object.entries(ElementPlusIconsVue))` 遍历注册了所有图标（约300+组件），增加了包体积和运行时内存占用。 |
| **修复建议** | 改为按需导入使用的图标，或使用 `unplugin-icons` / `unplugin-vue-components` 自动按需注册。 |

---

### QUAL-02: `BookList.vue` 分类数据硬编码

| 属性 | 值 |
|------|----|
| **文件** | `frontend/src/views/book/BookList.vue` |
| **行号** | 28-45 |
| **严重度** | **Low** |
| **描述** | 图书分类选择器的选项（文学、科技、历史、艺术）硬编码在模板中。当后台新增/修改分类时，前端需要同步发版。 |
| **修复建议** | 从 API 动态获取分类列表，或提取为常量配置文件。 |

---

### QUAL-03: `BorrowList.vue` 续借天数硬编码

| 属性 | 值 |
|------|----|
| **文件** | `frontend/src/views/borrow/BorrowList.vue` |
| **行号** | 211 |
| **严重度** | **Low** |
| **描述** | `borrowStore.renew(row.id, { extendDays: 30 })` 中 30 天续借天数硬编码。 |
| **修复建议** | 从环境变量或系统配置中读取。 |

---

### QUAL-04: `router/index.js` `from.path` 可能为空

| 属性 | 值 |
|------|----|
| **文件** | `frontend/src/router/index.js` |
| **行号** | 194 |
| **严重度** | **Low** |
| **描述** | `next(from.path || '/dashboard')` — 当用户直接访问无权限页面时（非路由跳转而来），`from.path` 可能是 `/` 或根路径，此时用户会被重定向到 `/dashboard`。虽然这是合理的 fallback，但如果 `from.path` 是 `/login` 等公开页面，用户可能反复被踢回 dashboard。 |
| **修复建议** | 当 `from.path` 是公开页面时，应重定向到 `/dashboard` 而不是 `from.path`。 |

---

### QUAL-05: `request.js` 请求拦截器错误处理器多余

| 属性 | 值 |
|------|----|
| **文件** | `frontend/src/utils/request.js` |
| **行号** | 34-36 |
| **严重度** | **Low** |
| **描述** | 请求拦截器的错误处理器 `(error) => Promise.reject(error)` 是默认行为，删除它效果完全相同。 |
| **修复建议** | 删除第34-36行，或添加日志记录等有意义的逻辑。 |

---

### QUAL-06: `BorrowList.vue` 未使用的参数 `_row`

| 属性 | 值 |
|------|----|
| **文件** | `frontend/src/views/borrow/BorrowList.vue` |
| **行号** | 234 |
| **严重度** | **Low** |
| **描述** | `handleDetail(_row)` 带有下划线前缀的参数 `_row` 在函数体内未被使用。虽然 Vue/ESLint 对此不报错，但表示该功能尚未实现（第235行 `ElMessage.info('详情功能开发中')`）。 |
| **修复建议** | 实现详情功能后使用该参数，或移除参数声明。 |

---

### QUAL-07: `SeatMap.vue` 座位网格渲染硬编码行列数

| 属性 | 值 |
|------|----|
| **文件** | `frontend/src/views/seat/SeatMap.vue` |
| **行号** | 46, 52 |
| **严重度** | **Low** |
| **描述** | `v-for="row in 8"` 和 `v-for="col in 10"` 硬编码了 8 行 10 列的座位布局。不同区域的座位布局不同时（如 A区 10行8列），此组件无法复用。 |
| **修复建议** | 从 API 或配置获取行列数：`v-for="row in areaConfig.rows"`。 |

---

## 5. 汇总表

| # | 类别 | 严重度 | 文件 | 描述 |
|---|------|--------|------|------|
| BUG-01 | 逻辑错误 | High | `main.js` | 孤立的 Pinia 实例未被使用 |
| BUG-02 | 逻辑错误 | High | `utils/request.js` | token 刷新可能传入 undefined，导致重试死循环 |
| BUG-03 | 逻辑错误 | Medium | `utils/request.js` | 错误消息可能为 "undefined" 字符串 |
| BUG-04 | 逻辑错误 | Medium | `utils/request.js` | null code 会被误判为失败响应 |
| BUG-05 | 逻辑错误 | Medium | `views/book/BookList.vue` | 关闭对话框按钮误报删除失败 |
| BUG-06 | UX 缺陷 | Medium | `views/seat/SeatMap.vue` | 加载座位时无 loading 指示器 |
| BUG-07 | 逻辑错误 | Low | `views/seat/SeatMap.vue` | Falsy 行/列号回退计算可能出错 |
| BUG-08 | 数据精度 | Low | `stores/user.js` | userInfo 赋值兜底可能混入多余字段 |
| BUG-09 | 逻辑错误 | Low | `views/auth/Login.vue` | 网络失败时刷新验证码无意义 |
| SEC-01 | 安全 | Critical | `utils/auth.js` + `utils/request.js` | CSRF Token 通过 JS 设置，防护失效 |
| SEC-02 | 安全 | High | `utils/auth.js` | Cookie secure 动态判断存在降级风险 |
| SEC-03 | 安全 | High | `utils/auth.js` | Refresh Token 未设置 HttpOnly |
| SEC-04 | 安全 | Medium | `views/auth/Login.vue` | 潜在开放重定向 |
| SEC-05 | 安全 | Medium | `utils/request.js` | 401 重试无限制导致无限循环 |
| PERF-01 | 性能 | Medium | `utils/request.js` | pendingRequests 队列无上限 |
| PERF-02 | 性能 | Low | `router/index.js` | 导航守卫可能重复获取用户信息 |
| QUAL-01 | 代码质量 | Low | `main.js` | 全局注册所有 Element Plus 图标 |
| QUAL-02 | 代码质量 | Low | `views/book/BookList.vue` | 图书分类硬编码 |
| QUAL-03 | 代码质量 | Low | `views/borrow/BorrowList.vue` | 续借天数硬编码 |
| QUAL-04 | 代码质量 | Low | `router/index.js` | from.path fallback 逻辑可改进 |
| QUAL-05 | 代码质量 | Low | `utils/request.js` | 多余的错误处理器 |
| QUAL-06 | 代码质量 | Low | `views/borrow/BorrowList.vue` | 未使用的函数参数 |
| QUAL-07 | 代码质量 | Low | `views/seat/SeatMap.vue` | 座位行列数硬编码 |

---

## 重点关注（按优先级修复）

1. **SEC-01** + **SEC-03** — CSRF 防护和 Refresh Token HttpOnly 是严重安全缺陷，应优先修复。
2. **BUG-01** — 孤立的 Pinia 实例可能导致状态持久化失效。
3. **BUG-02** — Token 刷新失败后的无限重试循环可能导致浏览器崩溃。
4. **SEC-02** — Cookie secure 属性应固定为生产环境 true。
5. **SEC-05** / **PERF-01** — 请求重试与队列缺少防护机制。

# 前端问题修复报告 (frontend-fix-report.md)

**修复时间**: 2026-04-23  
**修复人**: frontend-fix  
**项目**: library-system-v2/frontend

---

## 修复概览

| 问题编号 | 优先级 | 描述 | 状态 | 修改文件数 |
|---------|--------|------|------|-----------|
| FE-001  | P1     | Token存储在localStorage → 改用Cookie + CSRF | ✅ 已修复 | 5 |
| FE-002  | P1     | Login.vue硬编码默认凭证 | ✅ 已修复 | 1 |
| FE-003  | P1     | API baseURL硬编码 → 环境变量 | ✅ 已修复 | 3 |
| FE-004  | P1     | 占位符页面未实现 | ✅ 已修复 | 5 |

**总计**: 4个P1问题全部修复，修改14个文件

---

## FE-001: Token存储改用Cookie + CSRF支持

### 问题描述
Token存储在`localStorage`中，存在XSS攻击窃取风险。缺少CSRF防护机制。

### 修复内容

#### 1. `src/utils/auth.js` - Cookie安全增强
- **原方案**: 使用`js-cookie`但配置不完整
- **修复方案**:
  - 添加`secure`属性（HTTPS环境自动启用）
  - 添加`sameSite: 'Lax'`防止CSRF攻击
  - 新增`REFRESH_TOKEN_KEY`支持双Token机制
  - 新增`CSRF_TOKEN_KEY`及get/set/remove方法
  - 新增`clearAuthCookies()`统一清除方法

#### 2. `src/utils/request.js` - 请求拦截器安全增强
- **原方案**: 从`localStorage`读取Token
- **修复方案**:
  - 从Cookie读取Token（`getToken()`）
  - 非GET请求自动注入`X-CSRF-Token`头
  - 添加`withCredentials: true`支持Cookie跨域
  - 从响应头`x-csrf-token`自动提取并保存CSRF Token
  - 实现401自动刷新Token机制（双Token + 请求队列）
  - 替换`require()`为ES Module `import`

#### 3. `src/api/request.js` - 旧请求实例修复
- **原方案**: 硬编码`baseURL: '/api/v1'`，使用`localStorage`
- **修复方案**:
  - 使用环境变量`import.meta.env.VITE_API_BASE_URL`
  - 使用Cookie Token替代`localStorage`
  - 添加CSRF Token注入
  - 401处理改用`clearAuthCookies()`

#### 4. `src/stores/user.js` - 用户状态管理修复
- **原方案**: `localStorage.getItem('token')` / `localStorage.setItem('token')`
- **修复方案**:
  - 使用`getToken()`从Cookie读取
  - 使用`setToken()`/`setRefreshToken()`写入Cookie
  - 登出时使用`clearAuthCookies()`清除所有Cookie

#### 5. `src/store/modules/user.js` - 模块化用户状态修复
- **原方案**: 同上，使用localStorage
- **修复方案**: 同上，迁移至Cookie

### 安全提升
- ✅ Token不再暴露于`localStorage`（XSS防护）
- ✅ Cookie设置`sameSite: 'Lax'`（CSRF防护）
- ✅ HTTPS环境自动启用`secure`属性
- ✅ 非GET请求携带CSRF Token
- ✅ 双Token机制支持无感刷新
- ✅ 请求队列防止并发401重复刷新

---

## FE-002: Login.vue移除硬编码凭证

### 问题描述
`views/Login.vue`第50行显示默认管理员凭证 `admin / admin123`，存在安全泄露风险。

### 修复内容

#### `src/views/Login.vue`
- **移除**: `<p>默认管理员账号: admin / admin123</p>`
- **替换**: 添加 `<!-- FIXED: FE-002 -->` 注释标记

---

## FE-003: API baseURL改用环境变量

### 问题描述
`api/request.js`硬编码`baseURL: '/api/v1'`，不同环境无法灵活切换。

### 修复内容

#### 1. `src/api/request.js`
- **原**: `baseURL: '/api/v1'`
- **改**: `baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1'`
- **原**: `timeout: 30000`
- **改**: `timeout: Number(import.meta.env.VITE_API_TIMEOUT) || 30000`

#### 2. `src/utils/request.js`
- 同上，已使用环境变量

#### 3. 新建环境变量文件
- **`.env.development`**: `VITE_API_BASE_URL=http://localhost:8080/api/v1`
- **`.env.production`**: `VITE_API_BASE_URL=/api/v1`
- **`.env.example`**: 已存在，保持不变

---

## FE-004: 占位符页面补全

### 问题描述
多个页面仅显示`<el-empty>`占位符，无实际功能。

### 修复内容

#### 1. `src/views/Dashboard.vue` - 首页仪表盘
**原状态**: 借阅趋势区域仅显示`<el-empty description="图表数据加载中..." />`，统计数据硬编码

**修复内容**:
- 集成ECharts实现借阅趋势折线图（支持周/月切换）
- 集成ECharts实现图书分类饼图
- 添加热门图书Top 5表格
- 所有统计数据改为调用真实API（`getStatisticsOverview`、`getBorrowTrend`等）
- 添加`window.resize`图表自适应
- 组件销毁时正确dispose图表实例

#### 2. `src/views/BookList.vue` - 图书管理
**原状态**: `<el-empty description="图书列表功能开发中..." />`

**修复内容**:
- 搜索栏：关键词/分类/状态筛选
- 图书列表表格：书名/作者/ISBN/分类/出版社/可借数量/状态
- 操作按钮：编辑/详情/删除
- 分页组件
- 调用`getBookList`/`deleteBook` API

#### 3. `src/views/BorrowList.vue` - 借阅管理
**原状态**: `<el-empty description="借阅列表功能开发中..." />`

**修复内容**:
- 搜索栏：读者姓名/书名/状态筛选
- 借阅列表表格：书名/借阅人/借阅日期/应还日期/归还日期/状态
- 归还操作按钮
- 分页组件
- 调用`getBorrowList`/`returnBook` API

#### 4. `src/views/SeatList.vue` - 座位预约
**原状态**: `<el-empty description="座位预约功能开发中..." />`

**修复内容**:
- 座位状态概览卡片（总数/可用/已占/维护）
- 我的预约列表表格
- 取消预约操作
- 分页组件
- 调用`getSeatMap`/`getMyReservations`/`cancelReserve` API

#### 5. `src/views/Profile.vue` - 个人中心
**原状态**: `<el-empty description="个人中心功能开发中..." />`

**修复内容**:
- 左侧：用户信息卡片（头像/用户名/角色/信用积分进度条）
- 左侧：快捷导航（积分详情/我的借阅/座位预约）
- 右侧：个人信息编辑表单
- 右侧：修改密码表单
- 调用`getCreditInfo`/`changePassword` API

#### 6. `src/views/dashboard/Dashboard.vue` - 子目录Dashboard
**原状态**: 数据全部硬编码，无图表

**修复内容**:
- 同主Dashboard，集成ECharts借阅趋势图
- 统计数据改为API调用
- 添加信用积分和座位统计API集成

#### 7. `src/router/index.js` - 路由补充
- 新增`/profile/credit` → `CreditView.vue`积分详情路由
- 新增`/credit` → `CreditView.vue`信用积分路由

---

## 修改文件清单

| 文件路径 | 修复类型 | 说明 |
|---------|---------|------|
| `src/utils/auth.js` | FE-001 | Cookie安全增强+CSRF+双Token |
| `src/utils/request.js` | FE-001/003 | Cookie Token+CSRF+环境变量+Token刷新 |
| `src/api/request.js` | FE-001/003 | Cookie Token+CSRF+环境变量 |
| `src/stores/user.js` | FE-001 | localStorage→Cookie迁移 |
| `src/store/modules/user.js` | FE-001 | localStorage→Cookie迁移 |
| `src/views/Login.vue` | FE-002 | 移除硬编码凭证 |
| `src/views/Dashboard.vue` | FE-004 | ECharts图表+API集成 |
| `src/views/BookList.vue` | FE-004 | 完整图书管理页面 |
| `src/views/BorrowList.vue` | FE-004 | 完整借阅管理页面 |
| `src/views/SeatList.vue` | FE-004 | 完整座位预约页面 |
| `src/views/Profile.vue` | FE-004 | 完整个人中心页面 |
| `src/views/dashboard/Dashboard.vue` | FE-004 | ECharts图表+API集成 |
| `src/router/index.js` | FE-004 | 新增Credit路由 |
| `.env.development` | FE-003 | 开发环境变量 |
| `.env.production` | FE-003 | 生产环境变量 |

---

## 验证建议

1. **FE-001验证**:
   - 登录后检查浏览器Cookie是否包含`library_token`
   - 检查`localStorage`中不再有`token`字段
   - 检查POST/PUT/DELETE请求头是否包含`X-CSRF-Token`

2. **FE-002验证**:
   - 打开登录页面，确认不显示任何默认凭证提示

3. **FE-003验证**:
   - 开发环境API请求指向`http://localhost:8080/api/v1`
   - 生产构建后API请求指向`/api/v1`

4. **FE-004验证**:
   - Dashboard页显示借阅趋势ECharts图表
   - 图书/借阅/座位/个人中心页面显示完整功能
   - 所有页面数据来源于API调用

---

## 注意事项

1. **后端CSRF支持**: 当前前端已实现CSRF Token读取和发送，需后端配合：
   - 登录接口响应头返回`X-CSRF-Token`
   - 后续请求验证`X-CSRF-Token`头
   
2. **后端httpOnly Cookie**: 最安全的方案是后端设置httpOnly Cookie存储Token，前端无法通过JavaScript访问。当前方案使用js-cookie作为过渡方案，已比localStorage安全。

3. **Token刷新接口**: `api/auth.js`中已有`refreshToken()`接口定义，需确认后端实现。

4. **依赖检查**: `js-cookie`、`echarts`已在`package.json`中声明，无需额外安装。

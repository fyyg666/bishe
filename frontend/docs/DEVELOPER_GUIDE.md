# 前端开发指南

本文档详细介绍图书馆管理系统V2.0前端的技术架构、组件说明和开发规范。

## 版本信息

- 文档版本：2.0.0
- 前端框架：Vue 3.4
- 构建工具：Vite 5.2
- UI组件库：Element Plus 2.6
- 最后更新：2026-04-23

---

## 目录

1. [技术架构](#1-技术架构)
2. [项目结构](#2-项目结构)
3. [路由配置](#3-路由配置)
4. [组件说明](#4-组件说明)
5. [API封装](#5-api封装)
6. [状态管理](#6-状态管理)
7. [开发规范](#7-开发规范)

---

## 1. 技术架构

### 1.1 技术栈

| 分类 | 技术 | 版本 | 说明 |
|------|------|------|------|
| 核心框架 | Vue | 3.4.21 | 渐进式JavaScript框架 |
| 构建工具 | Vite | 5.2.8 | 下一代前端构建工具 |
| UI组件库 | Element Plus | 2.6.3 | Vue 3 组件库 |
| 路由管理 | Vue Router | 4.3.0 | Vue官方路由管理器 |
| 状态管理 | Pinia | 2.1.7 | Vue状态管理库 |
| HTTP客户端 | Axios | 1.6.8 | Promise HTTP客户端 |
| 图表库 | ECharts | 5.5.0 | 数据可视化图表库 |
| 工具库 | Day.js | 1.11.10 | 轻量级日期处理库 |
| 进度条 | NProgress | 0.2.0 | 页面加载进度条 |

### 1.2 架构特点

```
┌─────────────────────────────────────────────────────────┐
│                    Vue 3 Application                    │
├─────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐ │
│  │   Views     │  │ Components  │  │     Layouts     │ │
│  │  (页面视图)  │  │  (可复用组件) │  │     (布局组件)   │ │
│  └──────┬──────┘  └──────┬──────┘  └────────┬────────┘ │
│         │                │                    │          │
│  ┌──────┴────────────────┴────────────────────┴────────┐ │
│  │                    Pinia Store                     │ │
│  │                  (全局状态管理)                      │ │
│  └────────────────────────┬───────────────────────────┘ │
│                           │                             │
│  ┌────────────────────────┴───────────────────────────┐ │
│  │                    Vue Router                       │ │
│  │                   (路由管理)                        │ │
│  └────────────────────────┬───────────────────────────┘ │
│                           │                             │
│  ┌────────────────────────┴───────────────────────────┐ │
│  │                      Axios                          │ │
│  │                   (HTTP请求)                        │ │
│  └─────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────┐
│                    Backend API                          │
│              http://localhost:8080/api                   │
└─────────────────────────────────────────────────────────┘
```

---

## 2. 项目结构

```
frontend/
├── public/                      # 静态资源目录
│   └── favicon.ico              # 网站图标
│
├── src/
│   ├── api/                     # API接口封装
│   │   ├── auth.js              # 认证接口
│   │   ├── book.js              # 图书接口
│   │   ├── borrow.js            # 借阅接口
│   │   ├── reader.js            # 读者接口
│   │   ├── seat.js              # 座位接口
│   │   ├── announcement.js      # 公告接口
│   │   ├── volunteer.js         # 志愿者接口
│   │   ├── statistics.js        # 统计接口
│   │   ├── credit.js            # 积分接口
│   │   └── request.js           # Axios实例封装
│   │
│   ├── assets/                  # 静态资源
│   │   └── images/              # 图片资源
│   │
│   ├── components/              # 公共组件
│   │   ├── layout/              # 布局相关组件
│   │   ├── Breadcrumb.vue       # 面包屑导航
│   │   ├── EmptyState.vue       # 空状态组件
│   │   └── TableSkeleton.vue    # 表格骨架屏
│   │
│   ├── layouts/                 # 页面布局
│   │   └── MainLayout.vue       # 主布局组件
│   │
│   ├── router/                  # 路由配置
│   │   └── index.js             # 路由定义
│   │
│   ├── store/                   # Pinia状态管理
│   │   ├── modules/             # Store模块
│   │   │   ├── user.js          # 用户状态
│   │   │   └── app.js           # 应用状态
│   │   └── index.js             # Store导出
│   │
│   ├── styles/                  # 全局样式
│   │   ├── variables.scss       # SCSS变量
│   │   └── global.scss          # 全局样式
│   │
│   ├── utils/                   # 工具函数
│   │   ├── auth.js              # 认证工具
│   │   └── format.js            # 格式化工具
│   │
│   ├── views/                   # 页面视图
│   │   ├── auth/                # 认证相关页面
│   │   │   └── Login.vue        # 登录页
│   │   ├── book/                # 图书相关页面
│   │   ├── borrow/              # 借阅相关页面
│   │   ├── dashboard/           # 仪表盘页面
│   │   ├── layout/              # 布局组件
│   │   ├── profile/             # 个人中心页面
│   │   │   └── CreditView.vue   # 积分详情页
│   │   ├── seat/                # 座位相关页面
│   │   ├── AnnouncementList.vue  # 公告列表页
│   │   ├── BookList.vue         # 图书列表页
│   │   ├── BorrowList.vue       # 借阅列表页
│   │   ├── Dashboard.vue        # 仪表盘页
│   │   ├── Login.vue            # 登录页
│   │   ├── NotFound.vue         # 404页面
│   │   ├── Profile.vue          # 个人中心页
│   │   ├── ReaderList.vue       # 读者列表页
│   │   ├── SeatList.vue         # 座位列表页
│   │   ├── Statistics.vue       # 统计分析页
│   │   └── VolunteerList.vue    # 志愿者列表页
│   │
│   ├── App.vue                  # 根组件
│   └── main.js                  # 应用入口
│
├── docs/                        # 开发文档
│   └── DEVELOPER_GUIDE.md       # 本文档
│
├── index.html                   # HTML入口
├── package.json                 # 依赖配置
├── vite.config.js               # Vite配置
└── .env.example                 # 环境变量模板
```

---

## 3. 路由配置

### 3.1 路由结构

```javascript
// src/router/index.js
const routes = [
  // 公开路由
  { path: '/login', name: 'Login', component: Login, meta: { public: true } },
  
  // 受保护的路由（需登录）
  {
    path: '/',
    component: MainLayout,
    children: [
      { path: '/dashboard', name: 'Dashboard', component: Dashboard },
      { path: '/books', name: 'Books', component: BookList },
      { path: '/readers', name: 'Readers', component: ReaderList },
      { path: '/borrows', name: 'Borrows', component: BorrowList },
      { path: '/seats', name: 'Seats', component: SeatList },
      { path: '/announcements', name: 'Announcements', component: AnnouncementList },
      { path: '/volunteers', name: 'Volunteers', component: VolunteerList },
      { path: '/statistics', name: 'Statistics', component: Statistics },
      { path: '/credit', name: 'Credit', component: CreditView },
      { path: '/profile', name: 'Profile', component: Profile },
    ]
  },
  
  // 404路由
  { path: '/:pathMatch(.*)*', name: 'NotFound', component: NotFound }
]
```

### 3.2 路由说明

| 路径 | 名称 | 说明 | 权限 |
|------|------|------|------|
| /login | Login | 登录页 | 公开 |
| /dashboard | Dashboard | 首页仪表盘 | 登录用户 |
| /books | Books | 图书管理列表 | 登录用户 |
| /readers | Readers | 读者管理列表 | 管理员/馆员 |
| /borrows | Borrows | 借阅管理 | 登录用户 |
| /seats | Seats | 座位预约 | 登录用户 |
| /announcements | Announcements | 公告管理 | 登录用户 |
| /volunteers | Volunteers | 志愿服务 | 登录用户 |
| /statistics | Statistics | 统计分析 | 管理员/馆员 |
| /credit | Credit | 信用积分 | 登录用户 |
| /profile | Profile | 个人中心 | 登录用户 |
| /profile/credit | CreditDetail | 积分详情 | 登录用户 |

### 3.3 路由守卫

```javascript
router.beforeEach(async (to, from, next) => {
  // 1. 显示加载进度
  NProgress.start()
  
  // 2. 设置页面标题
  document.title = to.meta.title ? `${to.meta.title} - 图书馆管理系统` : '图书馆管理系统'
  
  // 3. 检查是否需要认证
  if (!to.meta.public) {
    const userStore = useUserStore()
    
    if (!userStore.token) {
      next('/login')
      return
    }
    
    // 4. 检查权限
    if (to.meta.roles && to.meta.roles.length > 0) {
      const hasPermission = to.meta.roles.includes(userStore.userInfo?.role)
      if (!hasPermission) {
        ElMessage.error('您没有权限访问该页面')
        next(from.path || '/dashboard')
        return
      }
    }
  }
  
  next()
})
```

### 3.4 路由元信息 (meta)

```typescript
interface RouteMeta {
  title: string        // 页面标题
  icon?: string        // 菜单图标
  public?: boolean     // 是否公开（无需登录）
  hidden?: boolean     // 是否在菜单隐藏
  roles?: string[]     // 允许访问的角色
  keepAlive?: boolean  // 是否缓存
}
```

---

## 4. 组件说明

### 4.1 布局组件

#### MainLayout.vue
主布局组件，包含侧边栏、顶栏和内容区域。

```
┌──────────────────────────────────────────────────┐
│                   Header Bar                      │
│  Logo  │  Breadcrumb  │  Search  │  User Info  │
├────────┬─────────────────────────────────────────┤
│        │                                         │
│  Side  │           Main Content Area             │
│  bar   │                                         │
│        │                                         │
│  Nav   │                                         │
│  Menu  │                                         │
│        │                                         │
└────────┴─────────────────────────────────────────┘
```

**属性：**
| 属性名 | 类型 | 说明 |
|--------|------|------|
| - | - | - |

**事件：**
| 事件名 | 说明 |
|--------|------|
| - | - |

### 4.2 公共组件

#### Breadcrumb.vue
面包屑导航组件，显示当前页面路径。

**使用示例：**
```vue
<template>
  <Breadcrumb :routes="breadcrumbRoutes" />
</template>

<script setup>
import Breadcrumb from '@/components/Breadcrumb.vue'

const breadcrumbRoutes = [
  { path: '/dashboard', title: '首页' },
  { path: '/books', title: '图书管理' },
  { title: '图书列表' }
]
</script>
```

#### EmptyState.vue
空状态组件，当列表为空时显示。

**属性：**
| 属性名 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| title | String | '暂无数据' | 标题 |
| description | String | '' | 描述 |
| icon | String | 'empty' | 图标名 |
| actionText | String | '' | 操作按钮文本 |
| action | Function | null | 操作回调 |

**使用示例：**
```vue
<template>
  <EmptyState 
    title="暂无图书" 
    description="快去添加一本图书吧"
    actionText="添加图书"
    @action="handleAdd" 
  />
</template>
```

#### TableSkeleton.vue
表格骨架屏组件，加载数据时显示占位。

**属性：**
| 属性名 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| rows | Number | 5 | 骨架行数 |
| columns | Number | 4 | 列数 |
| loading | Boolean | true | 是否显示骨架 |

### 4.3 业务组件

| 组件 | 路径 | 说明 |
|------|------|------|
| BookList | views/BookList.vue | 图书列表，含搜索、分页、操作 |
| ReaderList | views/ReaderList.vue | 读者列表，管理读者信息 |
| BorrowList | views/BorrowList.vue | 借阅列表，显示借阅记录 |
| SeatList | views/SeatList.vue | 座位预约，选座和预约 |
| AnnouncementList | views/AnnouncementList.vue | 公告列表，显示公告 |
| VolunteerList | views/VolunteerList.vue | 志愿者管理，志愿服务 |
| Statistics | views/Statistics.vue | 统计分析，图表展示 |
| Dashboard | views/Dashboard.vue | 首页仪表盘 |
| Profile | views/Profile.vue | 个人中心 |
| CreditView | views/profile/CreditView.vue | 积分详情 |

---

## 5. API封装

### 5.1 请求实例封装

```javascript
// src/api/request.js
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/modules/user'
import router from '@/router'

const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 30000
})

// 请求拦截器
service.interceptors.request.use(
  (config) => {
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers['Authorization'] = `Bearer ${userStore.token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器
service.interceptors.response.use(
  (response) => {
    const { code, message, data } = response.data
    if (code === 200) {
      return data
    }
    ElMessage.error(message || '请求失败')
    return Promise.reject(new Error(message))
  },
  (error) => {
    if (error.response) {
      const { status, data } = error.response
      switch (status) {
        case 401:
          ElMessage.error('登录已过期，请重新登录')
          useUserStore().logout()
          router.push('/login')
          break
        case 403:
          ElMessage.error('没有权限访问')
          break
        case 404:
          ElMessage.error('请求的资源不存在')
          break
        case 500:
          ElMessage.error('服务器错误')
          break
        default:
          ElMessage.error(data.message || '请求失败')
      }
    }
    return Promise.reject(error)
  }
)

export default service
```

### 5.2 API模块示例

```javascript
// src/api/book.js
import request from './request'

// 获取图书列表
export function getBookList(params) {
  return request({
    url: '/books',
    method: 'get',
    params
  })
}

// 获取图书详情
export function getBook(id) {
  return request({
    url: `/books/${id}`,
    method: 'get'
  })
}

// 添加图书
export function addBook(data) {
  return request({
    url: '/books',
    method: 'post',
    data
  })
}

// 更新图书
export function updateBook(id, data) {
  return request({
    url: `/books/${id}`,
    method: 'put',
    data
  })
}

// 删除图书
export function deleteBook(id) {
  return request({
    url: `/books/${id}`,
    method: 'delete'
  })
}
```

### 5.3 API模块列表

| 模块 | 文件 | 接口数量 |
|------|------|----------|
| 认证 | auth.js | 5 |
| 图书 | book.js | 8 |
| 读者 | reader.js | 5 |
| 借阅 | borrow.js | 6 |
| 座位 | seat.js | 6 |
| 公告 | announcement.js | 6 |
| 志愿者 | volunteer.js | 8 |
| 统计 | statistics.js | 9 |
| 积分 | credit.js | 4 |

---

## 6. 状态管理

### 6.1 Store模块结构

```
store/
├── modules/
│   ├── user.js      # 用户状态模块
│   └── app.js       # 应用状态模块
└── index.js         # Store导出
```

### 6.2 用户状态模块 (user.js)

```javascript
// src/store/modules/user.js
import { defineStore } from 'pinia'
import { login, logout, getUserInfo } from '@/api/auth'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: null,
    userInfo: null
  }),
  
  getters: {
    isLoggedIn: (state) => !!state.token,
    userRole: (state) => state.userInfo?.role,
    isAdmin: (state) => state.userInfo?.role === 'ADMIN',
    isLibrarian: (state) => ['ADMIN', 'LIBRARIAN'].includes(state.userInfo?.role)
  },
  
  actions: {
    async login(username, password) {
      const data = await login({ username, password })
      this.token = data.accessToken
      return data
    },
    
    async getUserInfo() {
      const info = await getUserInfo()
      this.userInfo = info
      return info
    },
    
    logout() {
      this.token = null
      this.userInfo = null
    }
  }
})
```

### 6.3 应用状态模块 (app.js)

```javascript
// src/store/modules/app.js
import { defineStore } from 'pinia'

export const useAppStore = defineStore('app', {
  state: () => ({
    sidebarOpened: true,
    device: 'desktop'
  }),
  
  actions: {
    toggleSidebar() {
      this.sidebarOpened = !this.sidebarOpened
    }
  }
})
```

---

## 7. 开发规范

### 7.1 Vue组件规范

**组件命名：**
- 组件名使用 PascalCase 或 kebab-case
- 公共组件放在 `components/` 目录
- 业务组件放在 `views/` 对应目录

**组件结构：**
```vue
<template>
  <div class="component-name">
    <!-- 模板内容 -->
  </div>
</template>

<script setup>
// 1. 引入
import { ref, computed } from 'vue'

// 2. Props
const props = defineProps({
  title: {
    type: String,
    default: ''
  }
})

// 3. Emits
const emit = defineEmits(['update', 'delete'])

// 4. 响应式数据
const loading = ref(false)
const data = ref([])

// 5. 计算属性
const isEmpty = computed(() => data.value.length === 0)

// 6. 方法
function handleSubmit() {
  emit('update', data.value)
}

// 7. 生命周期
import { onMounted } from 'vue'
onMounted(() => {
  // 初始化
})
</script>

<style scoped>
.component-name {
  /* 样式 */
}
</style>
```

### 7.2 命名规范

| 类型 | 规范 | 示例 |
|------|------|------|
| 组件文件 | PascalCase.vue | BookList.vue |
| API函数 | camelCase | getBookList |
| 常量 | UPPER_SNAKE_CASE | API_BASE_URL |
| 变量 | camelCase | userName |
| 类名/类型 | PascalCase | UserInfo |

### 7.3 样式规范

- 使用 `<style scoped>` 隔离组件样式
- 使用 SCSS 预处理器
- 参考 `styles/variables.scss` 定义变量

```scss
// styles/variables.scss
$primary-color: #409eff;
$success-color: #67c23a;
$danger-color: #f56c6c;

:export {
  primaryColor: $primary-color;
}
```

### 7.4 Git提交规范

```
feat: 新功能
fix: 修复bug
docs: 文档变更
style: 代码格式
refactor: 重构
perf: 性能优化
test: 测试
chore: 构建/工具变更
```

**提交示例：**
```
feat(book): 添加图书批量导入功能

- 添加xlsx解析功能
- 添加导入进度显示
- 添加导入结果反馈

closes #123
```

---

## 附录：常用命令

```bash
# 安装依赖
npm install

# 启动开发服务器
npm run dev

# 构建生产版本
npm run build

# 预览生产构建
npm run preview

# 代码检查
npm run lint

# 代码格式化
npm run format
```

---

## 附录：环境变量

### 开发环境 (.env.development)

```bash
# API地址
VITE_API_BASE_URL=http://localhost:8080/api

# 环境标识
VITE_APP_ENV=development

# 是否启用Mock数据
VITE_USE_MOCK=false

# 是否启用调试模式
VITE_DEBUG=true
```

### 生产环境 (.env.production)

```bash
# API地址（生产环境必须使用HTTPS）
VITE_API_BASE_URL=https://api.library.com/api

# 环境标识
VITE_APP_ENV=production

# 是否启用Mock数据（生产环境必须为false）
VITE_USE_MOCK=false

# 是否启用调试模式（生产环境必须为false）
VITE_DEBUG=false

# CDN地址（可选）
VITE_CDN_URL=https://cdn.library.com

# 百度统计ID（可选）
VITE_BAIDU_ANALYTICS_ID=xxxxxxxx
```

### 环境变量完整说明

| 变量名 | 必需 | 说明 | 示例值 |
|--------|------|------|--------|
| VITE_API_BASE_URL | 是 | 后端API地址 | http://localhost:8080/api |
| VITE_APP_ENV | 是 | 环境标识 | development / production |
| VITE_USE_MOCK | 否 | 是否使用Mock数据 | true / false |
| VITE_DEBUG | 否 | 是否启用调试 | true / false |
| VITE_CDN_URL | 否 | 静态资源CDN | https://cdn.example.com |
| VITE_BAIDU_ANALYTICS_ID | 否 | 百度统计ID | 1234567890 |

### 环境变量使用示例

```javascript
// 在代码中访问环境变量
const apiBaseUrl = import.meta.env.VITE_API_BASE_URL
const isProd = import.meta.env.VITE_APP_ENV === 'production'
const isDebug = import.meta.env.VITE_DEBUG === 'true'

// 不同环境下的API配置
const config = {
  baseURL: apiBaseUrl,
  timeout: isDebug ? 60000 : 30000,
  enableMock: !isProd && import.meta.env.VITE_USE_MOCK === 'true'
}
```

### 构建时环境变量

Vite在**构建时**读取环境变量，这意味着修改 `.env` 文件后需要重新构建才能生效。

```bash
# 开发环境
npm run dev  # 读取 .env.development

# 生产环境构建
npm run build        # 读取 .env.production
npm run build:test   # 读取 .env.test
```

### 注意事项

1. **安全性**：环境变量以 `VITE_` 开头的变量会打包到客户端代码中，不要在客户端暴露敏感信息（如后端密钥）
2. **即时性**：Vite不运行时监听文件变化，修改后需重启开发服务器
3. **优先级**：`.env.local` 会覆盖默认 `.env` 文件（用于本地调试）
4. **TypeScript支持**：创建 `src/env.d.ts` 文件获得环境变量类型提示

# 图书馆管理系统 - 前端

基于 Vue 3 + Vite + Element Plus 的现代化图书馆管理系统前端。

## 技术栈

- **框架**: Vue 3.4+ (Composition API)
- **构建工具**: Vite 6.4+
- **UI组件库**: Element Plus 2.9+
- **状态管理**: Pinia 2.3+
- **路由**: Vue Router 4.5+
- **HTTP客户端**: Axios 1.7+
- **图表**: ECharts 5.5+

## 项目结构

```
frontend/
├── src/
│   ├── api/              # API请求封装
│   ├── assets/           # 静态资源
│   ├── components/       # 公共组件
│   ├── layouts/          # 布局组件
│   ├── router/          # 路由配置
│   ├── stores/          # Pinia状态管理
│   ├── utils/           # 工具函数
│   ├── views/           # 页面组件
│   ├── App.vue         # 根组件
│   └── main.js         # 入口文件
├── public/             # 公共资源
├── dist/               # 构建产物
├── index.html          # HTML模板
├── vite.config.js      # Vite配置
└── package.json       # 依赖配置
```

## 快速开始

### 环境要求

- Node.js 18+
- npm 9+ 或 pnpm 8+

### 安装依赖

```bash
npm install
# 或
pnpm install
```

### 开发模式

```bash
npm run dev
```

访问 `http://localhost:5173`

### 生产构建

```bash
npm run build
```

构建产物位于 `dist/` 目录。

### 预览生产构建

```bash
npm run preview
```

## 脚本命令

| 命令 | 说明 |
|------|------|
| `npm run dev` | 启动开发服务器 |
| `npm run build` | 生产环境构建 |
| `npm run preview` | 预览生产构建 |
| `npm run lint` | 代码检查和修复 |

## 开发指南

### 添加新页面

1. 在 `src/views/` 创建Vue组件
2. 在 `src/router/index.js` 添加路由
3. 如需权限控制，添加 `meta.requiresAuth` 和 `meta.roles`

### API请求封装

所有API请求封装在 `src/api/` 目录：

```javascript
// src/api/book.js
import request from '@/utils/request'

export function getBookList(params) {
  return request({
    url: '/books',
    method: 'get',
    params
  })
}
```

### 状态管理

使用Pinia管理全局状态，Store位于 `src/stores/`：

- `user.js` - 用户信息、权限
- `settings.js` - 应用设置
- `notice.js` - 公告数据

### 权限控制

- 路由级别：`meta.roles` 控制页面访问权限
- 组件级别：`v-if="userStore.isAdmin"` 或 `v-if="userStore.isLibrarian"`

## 环境变量

| 变量 | 说明 | 默认值 |
|------|------|---------|
| `VITE_API_BASE_URL` | 后端API地址 | `http://localhost:8080/api/v1` |

创建 `.env.development` 和 `.env.production` 配置不同环境的API地址。

## 浏览器支持

- Chrome 90+
- Firefox 88+
- Safari 14+
- Edge 90+

## 常见问题

### 启动失败 - 端口被占用

修改 `vite.config.js` 中的 `server.port` 配置。

### API请求失败

检查 `.env.development` 中的 `VITE_API_BASE_URL` 是否正确指向后端服务。

### 构建后页面空白

检查 `vite.config.js` 中的 `base` 配置是否正确。

## 相关文档

- [后端API文档](../../API.md)
- [项目README](../../README.md)
- [开发指南](../../DEVELOPMENT.md)
- [部署指南](../../DEPLOYMENT.md)

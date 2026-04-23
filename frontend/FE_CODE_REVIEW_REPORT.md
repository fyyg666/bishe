# 图书馆管理系统 V2.0 - 前端代码审查报告

**审查时间**: 2026-04-24
**审查范围**: `frontend/` 全部源代码
**审查文件数**: API层 10个、Store层 4个、路由 1个、Vue组件 24个、配置文件 3个

---

## 一、发现的问题

### 1.1 严重问题 (P0/P1)

| 严重度 | 文件 | 问题 | 修复建议 |
|--------|------|------|----------|
| **P0** | `src/stores/book.js` | 导入不存在的函数 `addBook`，API中实际是 `createBook` | 将 `addBook` 改为 `createBook` |
| **P0** | `src/api/request.js` | 与 `src/utils/request.js` 重复，且缺少 Token 刷新逻辑 | 删除此文件，统一使用 `utils/request.js` |
| **P0** | 多组件混用 Store | `Login.vue`, `ReaderList.vue`, `VolunteerList.vue` 使用 `@/store/modules/user` (旧)；`BookAdd.vue` 使用 `@/stores/book` (新) | 统一使用新版 store |
| **P1** | `src/store/modules/user.js` | 与 `src/stores/user.js` 功能重叠，导致路径混乱 | 删除旧版 store，统一使用新版 |
| **P1** | `src/stores/user.js` | `isAdmin` 计算属性只检查 `ADMIN` 角色，遗漏 `LIBRARIAN` | 改为 `['ADMIN', 'LIBRARIAN'].includes(userInfo.value?.role)` |

### 1.2 中等问题 (P2/P3)

| 严重度 | 文件 | 问题 | 修复建议 |
|--------|------|------|----------|
| P2 | `src/utils/request.js` | `pendingRequests` 队列在 Token 刷新失败后可能残留 | 添加 `pendingRequests = []` 到 catch 块 |
| P2 | `src/views/SeatList.vue` | 调用 `getSeatMap()` 未传 `area` 参数，API 端要求必填 | 修改为 `getSeatMap(null)` 或确保有默认值 |
| P2 | `src/stores/user.js` | `fetchUserInfo` 失败时调用 `logout()` 但未捕获自身错误 | 添加 try-catch 包裹 logout 调用 |
| P3 | `src/views/BookList.vue` | `availableCopies`/`totalCopies` 字段与后端 `availableCount`/`stock` 不一致 | 确认字段映射关系 |
| P3 | `src/views/Dashboard.vue` | `recentNotices` 使用固定假数据，未调用 API | 实现公告列表 API 调用 |
| P3 | `src/store/modules/user.js` | `logout` 中 `token.value = null` 后 `isLoggedIn` 仍为 true | 改为 `token.value = ''` |

---

## 二、代码质量评分

| 模块 | 评分 | 说明 |
|------|------|------|
| **API 层** | 7/10 | 基础结构良好，但存在重复文件、字段命名不一致 |
| **状态管理** | 6/10 | Store 分散在两个目录，功能重叠严重，需统一 |
| **路由** | 8/10 | 路由配置完整，权限校验逻辑正确 |
| **组件** | 7/10 | 组件结构清晰，但部分字段名与后端不一致 |
| **依赖** | 8/10 | 依赖版本合理，无已知安全漏洞 |

**综合评分**: 7.2/10 (B级)

---

## 三、立即修复项（必须修复）

### 1. 修复 Store 导入错误
**文件**: `src/stores/book.js`
```javascript
// 错误
import { getBookList, getBookDetail, addBook, updateBook, deleteBook } from '@/api/book'

// 正确
import { getBookList, getBookDetail, createBook, updateBook, deleteBook } from '@/api/book'
```

### 2. 统一 Store 路径
**删除**: `src/store/modules/user.js`
**修改**:
- `Login.vue`: 改用 `@/stores/user`
- `ReaderList.vue`: 改用 `@/stores/user`
- `VolunteerList.vue`: 改用 `@/stores/user`
- 确保所有组件使用同一套 store

### 3. 删除重复文件
**删除**: `src/api/request.js`（与 `src/utils/request.js` 重复）

### 4. 修复 User Store 计算属性
**文件**: `src/stores/user.js`
```javascript
// 错误
const isAdmin = computed(() => ['ADMIN', 'LIBRARIAN'].includes(userInfo.value?.role))

// 正确（isAdmin 应该只检查 ADMIN）
const isAdmin = computed(() => userInfo.value?.role === 'ADMIN')
```

### 5. Token 刷新失败处理
**文件**: `src/utils/request.js`
```javascript
// 在 401 catch 块中添加
} catch (refreshError) {
  pendingRequests = []  // 添加此行
  clearAuthCookies()
  // ...
}
```

---

## 四、建议优化项

1. **字段映射一致性**: 前后端字段名统一（如 `availableCopies` vs `availableCount`）
2. **Store 持久化**: 考虑使用 Pinia 插件实现 Cookie 状态自动同步
3. **错误处理**: 增强 `fetchUserInfo` 的错误处理逻辑
4. **组件复用**: 将 `EmptyState` 组件统一应用到更多页面
5. **类型检查**: 考虑添加 TypeScript 支持以获得更好的类型安全

---

## 五、总结

前端代码整体结构清晰，采用了 Vue 3 + Pinia + Element Plus 的现代化技术栈。主要问题集中在：

1. **Store 管理混乱**：两套 store 并存，导致不同组件使用不同版本
2. **重复文件**：存在功能重叠的文件
3. **字段命名不一致**：部分前后端字段名不匹配

建议优先修复 P0/P1 问题，统一 store 管理，再逐步优化其他问题。

---

**报告生成**: frontend-reviewer

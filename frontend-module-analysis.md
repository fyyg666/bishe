# 前端模块完整性分析报告

## 1. 路由完整性分析

**路由定义文件**: `frontend/src/router/index.js`

| 路由路径 | 视图文件 | 状态 | 问题 |
|---------|---------|------|------|
| `/login` | `views/auth/Login.vue` | ✅ 完整 | - |
| `/register` | `views/auth/Register.vue` | ✅ 完整 | - |
| `/dashboard` | `views/dashboard/Dashboard.vue` | ✅ 完整 | - |
| `/books` | `views/book/BookList.vue` | ✅ 完整 | - |
| `/books/add` | `views/book/BookAdd.vue` | ✅ 完整 | - |
| `/books/:id` | `views/book/BookDetail.vue` | ✅ 完整 | - |
| `/readers` | `views/ReaderList.vue` | ✅ 完整 | - |
| `/borrows` | `views/borrow/BorrowList.vue` | ✅ 完整 | - |
| `/borrows/page` | `views/borrow/BorrowPage.vue` | ✅ 完整 | - |
| `/seats` | `views/SeatList.vue` | ✅ 完整 | - |
| `/seats/reserve` | `views/seat/SeatReserve.vue` | ✅ 完整 | - |
| `/seats/map` | `views/seat/SeatMap.vue` | ✅ 完整 | - |
| `/announcements` | `views/AnnouncementList.vue` | ✅ 完整 | - |
| `/volunteers` | `views/VolunteerList.vue` | ✅ 完整 | - |
| `/statistics` | `views/Statistics.vue` | ✅ 完整 | - |
| `/profile` | `views/profile/Profile.vue` | ✅ 完整 | - |
| `/profile/credit` | `views/profile/CreditView.vue` | ✅ 完整 | - |
| `/credit` | `views/profile/CreditView.vue` | ✅ 完整 | - |
| `/compensations` | `views/compensation/CompensationList.vue` | ✅ 完整 | - |
| `/:pathMatch(.*)*` | `views/NotFound.vue` | ✅ 完整 | - |

**结论**: 所有路由都有对应的视图文件，路由配置完整。

---

## 2. API 层完整性分析

### auth.js
| API函数 | 端点 | 是否使用 | 说明 |
|---------|------|---------|------|
| `login()` | POST `/auth/login` | ✅ Login.vue / user store | - |
| `logout()` | POST `/auth/logout` | ✅ user store | - |
| `getUserInfo()` | GET `/auth/info` | ✅ user store | - |
| `refreshToken()` | POST `/auth/refresh` | ✅ 定义但未确认使用 | 可能通过 request 拦截器使用 |
| `changePassword()` | PUT `/auth/password` | ✅ Profile.vue | - |
| `register()` | POST `/auth/register` | ✅ Register.vue | - |
| `getCaptcha()` | GET `/captcha` | ✅ Login.vue | - |

### book.js
| API函数 | 端点 | 是否使用 | 说明 |
|---------|------|---------|------|
| `getBookList()` | GET `/books` | ✅ book store | - |
| `getBookDetail()` | GET `/books/{id}` | ✅ book store | - |
| `createBook()` / `addBook()` | POST `/books` | ✅ book store | - |
| `updateBook()` | PUT `/books/{id}` | ✅ book store | - |
| `deleteBook()` | DELETE `/books/{id}` | ✅ book store | - |

**遗漏**: 无"热门/新书推荐"的API（getHotBooks定义在statistics.js中，但无专门的新书推荐接口），无分类列表API（BookList中的分类是硬编码的）。

### borrow.js
| API函数 | 端点 | 是否使用 | 说明 |
|---------|------|---------|------|
| `getBorrowList()` | GET `/borrows` | ✅ borrow store | 定义但前端BorrowList只用getMyBorrows |
| `getBorrowDetail()` | GET `/borrows/{id}` | ❌ **未使用** | 无前端组件消费 |
| `borrowBook()` | POST `/borrows` | ✅ borrow store | - |
| `returnBook()` | POST `/borrows/{id}/return` | ✅ borrow store | - |
| `renewBook()` | POST `/borrows/{id}/renew` | ✅ borrow store | - |
| `getMyBorrows()` | GET `/borrows/my` | ✅ borrow store/BorrowList.vue | - |

**遗漏**: 无"逾期罚款"相关API端点。

### seat.js
| API函数 | 端点 | 是否使用 | 说明 |
|---------|------|---------|------|
| `getSeatMap()` | GET `/seats` | ✅ SeatList.vue, SeatMap.vue, seat store | - |
| `getSeatDetail()` | GET `/seats/{id}` | ❌ **未使用** | API定义但无视图或store消费 |
| `reserveSeat()` | POST `/seats/reserve` | ✅ seat store | - |
| `cancelReserve()` | POST `/seats/cancel/{id}` | ✅ seat store, SeatList.vue, SeatReserve.vue | - |
| `getMyReservations()` | GET `/seats/my` | ✅ seat store, SeatList.vue | - |
| `checkIn()` | POST `/seats/checkin/{id}` | ❌ **未使用** | API定义但无任何地方调用 |
| `checkOut()` | POST `/seats/checkout/{id}` | ❌ **未使用** | API定义但无任何地方调用 |
| `checkAvailability()` | GET `/seats/check-availability` | ❌ **未使用** | API定义但无任何地方调用 |

### reader.js
| API函数 | 端点 | 是否使用 | 说明 |
|---------|------|---------|------|
| `getReaderList()` | GET `/readers` | ✅ ReaderList.vue | - |
| `getReaderDetail()` | GET `/readers/{id}` | ✅ ReaderList.vue | - |
| `getCurrentReader()` | GET `/readers/me` | ❌ **未使用** | API定义但无任何地方调用 |
| `registerReader()` | POST `/readers` | ✅ ReaderList.vue | - |
| `updateReader()` | PUT `/readers/{id}` | ✅ ReaderList.vue, Profile.vue | - |
| `changePassword()` | POST `/readers/{id}/password` | ❌ **未使用** | auth.js中有同名函数，reader.js这个未使用 |
| `deleteReader()` | DELETE `/readers/{id}` | ❌ **未使用** | ReaderList只有禁用/启用，无删除功能 |
| `resetPassword()` | POST `/readers/{id}/reset-password` | ❌ **未使用** | API定义但无视图调用 |
| `updateReaderStatus()` | POST `/readers/{id}/status` | ✅ ReaderList.vue | - |

### credit.js
| API函数 | 端点 | 是否使用 | 说明 |
|---------|------|---------|------|
| `getCreditInfo()` | GET `/credits` | ✅ CreditView.vue, Dashboard.vue | - |
| `getCreditRecords()` | GET `/credits/logs` | ✅ CreditView.vue | - |
| `getCreditRules()` | (硬编码，无实际API) | ✅ CreditView.vue | 返回Promise.resolve，无实际网络请求 |

### statistics.js
| API函数 | 端点 | 是否使用 | 说明 |
|---------|------|---------|------|
| `getStatisticsOverview()` | GET `/statistics/overview` | ✅ Dashboard.vue, Statistics.vue | - |
| `getBorrowStatistics()` | GET `/statistics/borrows` | ❌ **未使用** | 定义但未被view/store消费 |
| `getBookStatistics()` | GET `/statistics/books` | ❌ **未使用** | 定义但未被view/store消费 |
| `getReaderStatistics()` | GET `/statistics/readers` | ❌ **未使用** | 定义但未被view/store消费 |
| `getSeatStatistics()` | GET `/statistics/seats` | ✅ Dashboard.vue | - |
| `getBorrowTrend()` | GET `/statistics/borrow-trend` | ✅ Dashboard.vue, Statistics.vue | - |
| `getHotBooks()` | GET `/statistics/hot-books` | ✅ Statistics.vue | - |
| `getCategoryDistribution()` | GET `/statistics/category-distribution` | ✅ Statistics.vue | - |
| `getMonthlyStats()` | GET `/statistics/monthly` | ✅ Statistics.vue | - |
| `getSeatHeatmap()` | GET `/statistics/seat-heatmap` | ✅ Statistics.vue | - |

### volunteer.js
| API函数 | 端点 | 是否使用 | 说明 |
|---------|------|---------|------|
| `getVolunteerList()` | GET `/volunteers` | ❌ **未使用** | 定义但未被view/store消费 |
| `getMyVolunteers()` | GET `/volunteers/my` | ✅ VolunteerList.vue | - |
| `getVolunteerDetail()` | GET `/volunteers/{id}` | ❌ **未使用** | 定义但未被view/store消费 |
| `createVolunteer()` | POST `/volunteers` | ✅ VolunteerList.vue | - |
| `updateVolunteer()` | PUT `/volunteers/{id}` | ❌ **未使用** | 定义但未被view/store消费 |
| `cancelVolunteer()` | POST `/volunteers/{id}/cancel` | ✅ VolunteerList.vue | - |
| `reviewVolunteer()` | POST `/volunteers/{id}/review` | ✅ VolunteerList.vue | - |
| `getPendingVolunteers()` | GET `/volunteers/pending` | ✅ VolunteerList.vue | - |
| `deleteVolunteer()` | DELETE `/volunteers/{id}` | ❌ **未使用** | 定义但未被view/store消费 |
| `getVolunteerStats()` | GET `/volunteers/stats` | ✅ VolunteerList.vue | - |

### announcement.js
| API函数 | 端点 | 是否使用 | 说明 |
|---------|------|---------|------|
| `getAnnouncementList()` | GET `/announcements` | ✅ AnnouncementList.vue | - |
| `getAnnouncementDetail()` | GET `/announcements/{id}` | ✅ AnnouncementList.vue | - |
| `getLatestAnnouncements()` | GET `/announcements/latest` | ✅ Dashboard.vue | - |
| `createAnnouncement()` | POST `/announcements` | ✅ AnnouncementList.vue | - |
| `updateAnnouncement()` | PUT `/announcements/{id}` | ✅ AnnouncementList.vue | - |
| `publishAnnouncement()` | POST `/announcements/{id}/publish` | ✅ AnnouncementList.vue | - |
| `deleteAnnouncement()` | DELETE `/announcements/{id}` | ✅ AnnouncementList.vue | - |

---

## 3. Store 完整性分析

### user store (stores/user.js)
| 方法 | 是否被使用 | 说明 |
|------|-----------|------|
| `initUser()` | ❌ main.js中未调用 | 定义了但main.js中没有调用初始化 |
| `fetchUserInfo()` | ✅ router/index.js (导航守卫) | - |
| `login()` | ✅ Login.vue | - |
| `logout()` | ✅ Header.vue | - |
| `setToken()` | ✅ 定义 | 由refreshToken逻辑调用 |

### book store (stores/book.js)
| 方法 | 是否被使用 | 说明 |
|------|-----------|------|
| `fetchBooks()` | ✅ BookList.vue | - |
| `fetchBookDetail()` | ✅ BookDetail.vue, BookAdd.vue (编辑模式) | - |
| `createBook()` | ✅ BookAdd.vue | - |
| `editBook()` | ✅ BookAdd.vue | - |
| `removeBook()` | ✅ BookList.vue | - |
| `clearCurrentBook()` | ❌ **未使用** | 定义但未在任何view调用 |

### borrow store (stores/borrow.js)
| 方法 | 是否被使用 | 说明 |
|------|-----------|------|
| `fetchBorrows()` | ❌ **未使用** | 定义但未在任何view调用（前端用fetchMyBorrows） |
| `fetchMyBorrows()` | ✅ BorrowList.vue | - |
| `borrow()` | ✅ BorrowPage.vue | - |
| `returnBookById()` | ✅ BorrowList.vue | - |
| `renew()` | ✅ BorrowList.vue | - |

### seat store (stores/seat.js)
| 方法 | 是否被使用 | 说明 |
|------|-----------|------|
| `fetchSeatMap()` | ❌ **未使用** | SeatMap.vue直接调用API而非store方法 |
| `reserve()` | ✅ SeatReserve.vue | - |
| `cancel()` | ✅ SeatReserve.vue | - |
| `fetchMyReservations()` | ✅ SeatReserve.vue, seat store | - |

---

## 4. 视图完整性分析

### 4.1 Login.vue
- ✅ Loading 状态 (loading按钮)
- ✅ 验证码刷新
- ✅ 表单验证
- ✅ 错误处理
- ✅ 注册链接
- **遗漏**: ❌ 密码强度校验提示（只检查最小长度6位，无大小写/数字/特殊字符要求）

### 4.2 Register.vue
- ✅ 完整注册表单（用户名、密码、确认密码、邮箱、手机号、真实姓名）
- ✅ 表单验证
- ✅ Loading 状态
- ✅ 注册成功跳转
- **遗漏**: ❌ 密码强度校验（仅检查长度≥6，无复杂度校验）
- **遗漏**: ❌ 无验证码

### 4.3 Dashboard.vue
- ✅ 统计卡片（图书总数、在借数量、可用座位、我的积分）
- ✅ ECharts 借阅趋势图（仅管理员）
- ✅ 快捷入口
- ✅ 最近公告
- ✅ 座位预约情况
- **遗漏**: ❌ 热门图书推荐（已在API层实现getHotBooks但Dashboard未使用）
- **遗漏**: ❌ 新书推荐

### 4.4 BookList.vue
- ✅ 搜索筛选（书名、作者、分类）
- ✅ 表格展示
- ✅ 分页
- ✅ 加载状态
- ✅ 添加/编辑/删除操作
- ✅ Empty State（无数据时不显示表格仅显示分页）
- **问题**: ❌ handleExport提示"导出功能开发中"
- **遗漏**: ❌ 分类选项硬编码（文学/科技/历史/艺术），缺少从API动态获取分类

### 4.5 BookDetail.vue
- ✅ 图书详情展示
- ✅ Loading状态
- ✅ 借阅此书按钮（跳转到借阅页）
- **遗漏**: ❌ 无封面图片展示
- **遗漏**: ❌ 无相关图书/同作者图书推荐
- **遗漏**: ❌ 无借阅历史展示

### 4.6 BookAdd.vue
- ✅ 完整表单
- ✅ 编辑/新增双模式
- ✅ 表单验证
- ✅ 分类选择
- **遗漏**: ❌ 分类选项硬编码，无API获取

### 4.7 BorrowList.vue
- ✅ 借阅记录列表
- ✅ 搜索过滤
- ✅ 分页
- ✅ 续借/还书操作
- ✅ Loading状态
- ❌ `handleDetail` 提示"详情功能开发中"
- **遗漏**: ❌ 无逾期罚款金额显示
- **遗漏**: ❌ 无逾期天数提醒

### 4.8 BorrowPage.vue
- ✅ 借阅表单
- ✅ 从BookDetail传递bookId
- ✅ 借阅天数选择（最大90天）
- **遗漏**: ❌ 无选择读者（仅输入bookId，缺少readerId/读者选择）
- **遗漏**: ❌ 无借阅前信用检查
- **遗漏**: ❌ 无逾期罚款预览

### 4.9 SeatList.vue
- ✅ 座位统计概览
- ✅ 我的预约列表
- ✅ 取消预约功能
- ✅ 骨架屏加载
- ✅ EmptyState
- ✅ 分页
- ✅ 可视化选座入口
- ❌ **签到/签退按钮缺失**: 只有取消预约操作，无checkIn/checkOut操作
- **遗漏**: ❌ 无阅览室管理UI

### 4.10 SeatMap.vue
- ✅ 区域切换（A/B/C区）
- ✅ 座位网格展示（8排×10列）
- ✅ 座位状态颜色区分
- ✅ 选择座位并跳转到预约页
- ✅ 图例说明
- **遗漏**: ❌ 无阅览室概念（座位仅仅按区域分组，无阅览室管理）

### 4.11 SeatReserve.vue
- ✅ 预约表单（区域、座位号、日期、时间）
- ✅ 从SeatMap联动
- ✅ 我的预约列表
- ✅ 取消预约
- ✅ 表单验证
- **遗漏**: ❌ 无签到按钮（签到仅在后端有checkIn API，前端无界面）
- **遗漏**: ❌ 无签退按钮

### 4.12 ReaderList.vue
- ✅ 读者列表
- ✅ 搜索筛选（关键词、角色、状态）
- ✅ 分页
- ✅ 创建/编辑对话框
- ✅ 详情对话框
- ✅ 禁用/启用操作
- ✅ Loading/Empty/Skeleton状态
- ❌ **遗漏**: 无删除读者操作（只能禁用）
- ❌ **遗漏**: 无积分等级显示（只显示积分数值，无铜牌/银牌/金牌/白金标签）
- ❌ **遗漏**: 无重置密码操作（resetPassword API存在但无界面）

### 4.13 AnnouncementList.vue
- ✅ 公告CRUD（创建、编辑、查看、删除）
- ✅ 搜索筛选（关键词、类型、状态）
- ✅ 分页
- ✅ Loading/Skeleton/Empty状态
- ✅ 发布/归档状态
- ✅ 详情查看

### 4.14 VolunteerList.vue
- ✅ 志愿服务申请
- ✅ 我的服务记录
- ✅ 待审核列表（管理员）
- ✅ 审核通过/拒绝
- ✅ 取消申请
- ✅ 统计卡片
- ✅ 搜索筛选
- ✅ 分页
- ✅ Loading/Skeleton/Empty状态
- ✅ 详情查看

### 4.15 Statistics.vue
- ✅ 统计概览卡片
- ✅ ECharts图表（借阅趋势、分类分布、座位热力图、月度统计）
- ✅ 热门图书Top10表格
- ✅ 详细统计数据（借阅、图书、读者、座位）
- **遗漏**: ❌ 图书热力推荐(新书/热门) - 统计页虽然有热门图书但Dashboard无显示

### 4.16 Profile.vue
- ✅ 个人信息展示
- ✅ 编辑信息表单
- ✅ 修改密码
- ✅ 表单验证
- **遗漏**: ❌ 无信用等级显示（铜牌/银牌/金牌/白金）
- **遗漏**: ❌ 无信用积分入口按钮/链接
- **遗漏**: ❌ 无头像上传功能

### 4.17 CreditView.vue
- ✅ 积分概览（当前积分、等级）
- ✅ 积分规则表格（硬编码数据）
- ✅ 积分记录分页列表
- **遗漏**: ❌ `borrowCount` 和 `returnOnTimeRate` 数据字段在后端可能不存在（代码中用默认值0和100%）
- **遗漏**: ❌ 无信用等级对应的图标/勋章展示

### 4.18 CompensationList.vue
- ✅ 赔偿单列表
- ✅ 创建赔偿单对话框
- ✅ 处理支付（现金、积分抵扣、志愿服务抵扣）
- ✅ 取消赔偿单
- ✅ 详情查看
- **遗漏**: ❌ 无过期刊显示和直接支付入口（必须在赔偿管理中处理）
- **遗漏**: ❌ 无分页控制（@size-change和@current-change事件未绑定到fetchList）

### 4.19 NotFound.vue
- ✅ 404页面
- ✅ 返回首页按钮

### 4.20 Layout.vue
- ✅ 整体布局（Sidebar + Header + Content）
- ✅ keep-alive缓存列表页

### 4.21 Header.vue
- 搜索图书（搜索关键词跳转）
- 用户下拉菜单（个人信息、积分、退出）
- ❌ `showNotifications()` 方法为 `// TODO` - 通知功能未实现

---

## 5. 用户流程链分析

### 流程1: 登录 → 仪表盘 → 图书列表 → 图书详情 → 借阅 → 归还
```
Login.vue → Dashboard.vue → BookList.vue → BookDetail.vue → BorrowPage.vue → BorrowList.vue
```
- ✅ Login到Dashboard: 正常
- ✅ Dashboard到BookList: 导航到位
- ✅ BookList到BookDetail: `router.push(/books/${id})`
- ✅ BookDetail到BorrowPage: 带bookId参数
- ✅ BorrowPage到BorrowList: 借阅成功跳转
- ✅ BorrowList还书: 正常
- **问题**: BorrowPage只输入bookId和天数，无读者身份验证

### 流程2: 登录 → 座位预约 → 可视化选座 → 预约 → 签到 → 签退
```
Login.vue → SeatList.vue → SeatMap.vue → SeatReserve.vue → (checkIn) → (checkOut)
```
- ✅ SeatList到SeatMap/SeatReserve: 正常
- ✅ SeatMap到SeatReserve: 传递座位参数
- ❌ **断裂**: checkIn（签到）和checkOut（签退）API存在但前端**无任何UI**实现
- ❌ 用户预约座位后无法签到/签退，流程不完整

### 流程3: 登录 → 个人中心 → 信用积分
```
Login.vue → Profile.vue → CreditView.vue
```
- ✅ Header下拉菜单提供入口
- ✅ Sidebar提供信用积分入口
- **不足**: Profile.vue页面未显示信用等级，需要手动点击"我的积分"才能查看

### 流程4: 管理员 - 登录 → 读者管理 → 公告管理 → 统计分析
```
Login.vue → ReaderList.vue → AnnouncementList.vue → Statistics.vue
```
- ✅ 三个页面路由均有roles: ['ADMIN', 'LIBRARIAN'] 权限控制
- ✅ 完整CRUD操作
- ✅ 图表展示

### 流程5: 志愿者 - 登录 → 志愿服务 → 申请 → 查看时长
```
Login.vue → VolunteerList.vue → 申请对话框 → 待审核列表 → 统计
```
- ✅ 完整流程
- ✅ 申请、审核、统计

### 流程6: 赔偿管理
```
Login.vue → CompensationList.vue → 创建/处理
```
- ✅ 完整CRUD
- **问题**: 分页事件未正确绑定

---

## 6. 缺失页面/桩功能

| 位置 | 类型 | 描述 |
|------|------|------|
| `BookList.vue:277` | 桩函数 | `handleExport()` → `ElMessage.info('导出功能开发中')` |
| `BorrowList.vue:235` | 桩函数 | `handleDetail()` → `ElMessage.info('详情功能开发中')` |
| `Header.vue:91` | 空函数 | `showNotifications()` → `// TODO` (通知功能未实现) |
| `api/credit.js:48-62` | 桩数据 | `getCreditRules()` → `Promise.resolve(硬编码数据)`，非实际API调用 |

**注意**: SeatList和VolunteerList中对比其他模块有完整的三态（Skeleton/Empty/Table）展示，是做得最好的。

---

## 7. 死代码分析

### 7.1 未使用的组件
| 文件 | 说明 |
|------|------|
| `components/TableSkeleton.vue` | 从未被任何文件import |
| `components/ImageLazyLoad.vue` | 从未被任何文件import |

### 7.2 已定义但未在视图/Store中消费的API函数
| API文件 | 函数 | 说明 |
|---------|------|------|
| `borrow.js` | `getBorrowDetail()` | 定义但前端无调用 |
| `seat.js` | `checkIn()`, `checkOut()`, `checkAvailability()`, `getSeatDetail()` | 签到/签退/检查可用性均无前端UI |
| `reader.js` | `deleteReader()`, `resetPassword()`, `getCurrentReader()`, `changePassword()`(reader版本) | 删除读者/重置密码无界面入口 |
| `volunteer.js` | `deleteVolunteer()`, `updateVolunteer()`, `getVolunteerDetail()`, `getVolunteerList()` | 删除/更新/详情无界面调用 |
| `statistics.js` | `getBorrowStatistics()`, `getBookStatistics()`, `getReaderStatistics()` | 已通过getStatisticsOverview聚合获取 |

### 7.3 Store中未使用的方法
| Store | 方法 | 说明 |
|-------|------|------|
| `stores/book.js` | `clearCurrentBook()` | 定义但未在任何view中调用 |
| `stores/borrow.js` | `fetchBorrows()` | 定义但未调用（实际使用fetchMyBorrows） |
| `stores/user.js` | `initUser()` | 定义但main.js未调用初始化 |

---

## 8. 功能缺口分析（基于README特性对比）

### 8.1 密码强度校验 ❌ MISSING
- README §1中提到"密码强度校验"
- Login.vue和Register.vue中仅验证密码长度≥6，**无大小写、数字、特殊字符等强度校验**
- 前端应添加密码复杂度指示器

### 8.2 热门/新书推荐 ❌ PARTIAL
- `getHotBooks` API存在（statistics.js中），Statistics.vue使用
- **但** Dashboard.vue未展示热门图书/新书推荐
- **无"新书推荐"** 的独立API或视图展示

### 8.3 逾期罚款显示 ❌ MISSING
- 逾期罚款（overdue fine）在README中提到
- BorrowList.vue中无逾期罚款金额列
- 无用户在Dashboard看到欠款/罚款的入口
- 逾期天数显示缺失

### 8.4 阅览室管理 ❌ MISSING
- README §4"阅览室管理"
- 前端只有座位区域（A/B/C区）概念，**无阅览室管理UI**
- 无创建/编辑/删除阅览室的界面
- SeatMap只是代码网格，无阅览室层面

### 8.5 可视化选座（座位地图） ✅ IMPLEMENTED
- SeatMap.vue实现了8×10网格座位地图
- 区域切换、状态颜色区分、选择跳转

### 8.6 信用等级显示（铜牌/银牌/金牌/白金） ❌ PARTIAL
- CreditView.vue中实现了等级计算逻辑 `getCreditLevel()`，显示"当前等级"
- **但**: Profile.vue无信用等级展示
- ReaderList.vue只显示积分数值，无等级标签
- Sidebar/dashboard中无等级勋章图标

### 8.7 志愿者时长追踪 ✅ IMPLEMENTED
- VolunteerList.vue完整实现
- 统计卡片显示累计服务次数、累计时长(h)、待审核数

### 8.8 统计图表 ✅ IMPLEMENTED
- Statistics.vue 实现完整
- ECharts：借阅趋势（折线图）、分类分布（饼图）、座位热力图、月度统计（柱状图）
- Dashboard.vue中ECharts借阅趋势图（仅管理员）

### 8.9 签到/签退功能 ❌ MISSING
- 后端提供 `checkIn` 和 `checkOut` API
- 前端在SeatList.vue和SeatReserve.vue中**无签到/签退按钮**
- 用户预约座位后无法通过前端完成签到和签退流程

### 8.10 通知系统 ❌ MISSING
- Header.vue中通知图标 `showNotifications()` 是 `// TODO`
- `notificationCount` 硬编码为0

---

## 9. 总结：优先级修复建议

### P0 - 断裂流程（用户无法完成操作）
1. **签到/签退UI缺失**: seat store中已定义checkIn/checkOut API，但SeatList和SeatReserve无对应按钮
2. **逾期罚款无显示**: 用户无法在借阅列表或Dashboard看到欠款

### P1 - 功能缺口
1. **密码强度校验**: 注册/修改密码时无强度指示
2. **Dashboard无热门/新书推荐**: 统计API已存在但Dashboard未使用
3. **信用等级在Profile/ReaderList无展示**: 等级逻辑已实现但其他页面未复用
4. **BorrowList分页事件未绑定**: CompensationList缺少@size-change和@current-change绑定

### P2 - 界面完整性
1. **桩函数待实现**: BookList导出、BorrowList详情、"功能开发中"提示
2. **Empty/Skeleton状态不一致**: BookAdd/BookDetail/BorrowPage等页面缺少EmptyState组件
3. **Header通知功能未实现**: showNotifications只留白

### P3 - 死代码清理
1. `TableSkeleton.vue` 和 `ImageLazyLoad.vue` 从未使用
2. 多个API函数从未被前端消费（checkIn, checkOut, deleteReader, resetPassword等）

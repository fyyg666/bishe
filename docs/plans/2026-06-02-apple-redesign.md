# 图书馆管理系统 · Apple 风格全站翻新设计方案

> 设计方向：**渐进融合风（Route C）**——保留 Element Plus 组件基础，用 Apple 设计原则重新定义样式令牌
> 参考风格：Apple Human Interface Guidelines + apple.com 网页设计语言
> 日期：2026-06-02

---

## 一、色彩体系转型

### 1.1 主色调

从"石板蓝 oklch(0.52 0.2 248)"转为 **Apple Blue `#0071E3`**。

```scss
$primary: #0071E3;
$primary-light: #40A9FF;
$primary-lighter: #E6F4FF;
$primary-dark: #005BB5;
```

### 1.2 中性色——冷灰体系

从暖沙色底调转为 Apple 标准冷灰：

| Token | 旧（oklch 暖灰） | 新（Apple Gray） |
|---|---|---|
| 页面背景 | `oklch(0.975 0.004 55)` | `#F5F5F7` |
| 卡片背景 | `oklch(0.995 0.002 55)` | `#FFFFFF` |
| 侧边栏背景 | `oklch(0.3 0.025 34)` 深色 | `rgba(245,245,247,0.8)` 毛玻璃 |
| Header 玻璃 | `rgba(255,255,255,0.88)` | `rgba(255,255,255,0.72)` |
| 输入框背景 | 白色 | `#F2F2F7` |

### 1.3 语义色——Apple 标准色

| Token | 颜色 | 用途 |
|---|---|---|
| Success | `#34C759` | 成功状态 |
| Warning | `#FF9500` | 警告状态 |
| Danger | `#FF3B30` | 错误/危险 |

---

## 二、字体与间距

### 2.1 字号体系（1.2 倍率）

| Token | 字号 | 用途 |
|---|---|---|
| xs | 12px | 辅助标签、badge |
| sm | 13px | 表单辅助文字 |
| base | 14px | 正文、表格 |
| lg | 16px | 卡片标题、表单标签 |
| xl | 20px | 弹窗标题 |
| 2xl | 28px | 页面主标题（weight: 700, spacing: -0.022em） |
| 3xl | 40px | 仪表盘大数字 |

### 2.2 间距——8pt 网格加大 padding

- 卡片 padding：16px → 24px
- 表单 label 间距：8px → 12px
- 页面内容区 padding：16px → 32px
- 表格单元格 padding：保持 12px

---

## 三、组件质感

### 3.1 圆角

| Token | 新值 | 用途 |
|---|---|---|
| sm | 6px | 标签、badge |
| md | 10px | 按钮、输入框 |
| lg | 16px | 卡片、面板 |
| xl | 20px | 模态框 |

### 3.2 阴影——尽量不用

- 卡片：1px 极浅边框 `#E5E5EA`，默认无阴影
- 悬停：边框微微加深至 `#C7C7CC`，不上浮、不加阴影
- 弹出层（dropdown/dialog）：保留轻阴影做空间层次

### 3.3 玻璃质感

- Header：`rgba(255,255,255,0.72)` + `blur(20px)` + 0.5px 底部分隔线
- 侧边栏：`rgba(245,245,247,0.8)` + `blur(24px)`
- 弹出面板：`rgba(255,255,255,0.88)` + `blur(16px)`

### 3.4 组件规格

- **按钮**：Primary 纯色背景无边框，hover 不变色仅微调，按下 scale(0.97) + 60ms 弹性恢复
- **输入框**：背景 `#F2F2F7`，聚焦白底 + 蓝光环
- **表格**：表头纯白 + 细底边，行悬停 `#F5F5F7`，行高 44px
- **通知中心**：item 未读态用左侧蓝点而非蓝色背景条

---

## 四、布局与导航

### 4.1 侧边栏

- 背景：`rgba(245,245,247,0.8)` + `backdrop-filter: blur(24px)`
- 菜单项：深色文字 + 极浅 hover 背景
- 激活态：左侧 3px 蓝竖线 + 浅蓝底色，不用整条填充
- Logo：纯圆形图标，更像 Apple app icon
- 折叠态宽度：64px

### 4.2 顶栏

- 高度：56px → 52px
- 玻璃：`rgba(255,255,255,0.72)` + `blur(20px)`
- 底部分隔：0.5px `#E5E5EA`

### 4.3 内容区

- padding：`32px 40px`（桌面）
- 最大宽度：1200px
- 页面标题区：去底部分隔线，靠留白区隔

---

## 五、动效与微交互

### 5.1 缓动曲线

| 用途 | 曲线 |
|---|---|
| 出站（展开/出现） | `cubic-bezier(0, 0, 0.2, 1)` |
| 入站（收起/消失） | `cubic-bezier(0.4, 0, 1, 1)` |
| 弹性反馈 | `cubic-bezier(0.34, 1.56, 0.64, 1)` |

### 5.2 关键动画

- 页面切换：20px 右滑 + 淡入
- 侧边栏折叠：文字缩小消失 + 300ms spring
- 按钮点击：scale(0.97) + 200ms 弹性恢复
- 表格行悬停：120ms 背景淡入淡出

---

## 六、核心页面改造

### 6.1 仪表盘 Dashboard

- 统计卡片：纯白、去图标、大数字 40px w700、标题 13px 灰
- ECharts：去网格线、单色蓝渐变、无图例边框
- 快捷入口：横向卡片横滑（Horizontal Scroll Snap）

### 6.2 图书管理列表

- 筛选：单搜索条 + 可折叠筛选面板（默认收起）
- 表格：行高 44px，操作列图标化 + tooltip
- 分页：简约数字页码 + 前后箭头

### 6.3 统一检索

- 居中大搜索框：40px 高度、圆角 20px、背景 `#F2F2F7`
- 结果：卡片布局，封面缩略图 + 信息 + 状态

### 6.4 弹窗 / 表单

- 弹窗圆角 20px，标题区无底边
- 输入框背景 `#F2F2F7`，label 13px semibold
- 提交按钮右对齐，取消用文字链接

---

## 七、实施路线图

| 阶段 | 内容 | 文件 |
|---|---|---|
| P1 - 设计令牌 | variables.scss v4.0 | `src/styles/variables.scss` |
| P1 - 全局样式 | index.scss 组件覆盖 | `src/styles/index.scss` |
| P1 - Mixins | 动效缓动、玻璃效果升级 | `src/styles/mixins.scss` |
| P2 - 布局 | Sidebar + Header + Layout | `src/components/layout/` |
| P2 - 仪表盘 | Dashboard.vue | `src/views/dashboard/` |
| P3 - 列表页 | BookList.vue | `src/views/book/` |
| P3 - 检索页 | UnifiedSearch.vue | `src/views/search/` |
| P3 - 通用组件 | Dialog、Form、Table 覆盖 | `src/styles/index.scss` |

---

## 八、设计原则速查

1. **去阴影、用细线**——靠边框和留白区分层级
2. **大圆角、上玻璃**——16-20px 圆角 + backdrop-filter
3. **大字重、大留白**——700 weight 标题 + 32px+ padding
4. **单色调、高克制**——一个蓝色做所有强调，不多用色彩
5. **微动效、有物理**——spring 缓动 + scale 反馈

---

*UI Designer | 2026-06-02*

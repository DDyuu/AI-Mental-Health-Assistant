# 网站整体样式焕新 — 设计文档

- 日期：2026-09-15
- 状态：**已评审通过**（2026-09-15）。评审后修订：补全 6 个样式文件的架构与 `@use` 编译单元约束（4.1）、Element 色阶改为 1–9 全阶（4.4）、图表色板镜像与内联样式规则（4.5 第 7/8 条）、硬指标改为实测口径并记录构建的沙箱前提（7.1）
- 范围：`vue/` 前端全部样式层（3 套布局 + 13 个页面 + 8 个组件）
- 不在范围：Spring Boot 后端、业务逻辑、接口、路由行为、数据库

---

## 1. 背景：为什么需要整体样式更新

当前前端不存在统一的样式体系，视觉问题有明确的量化证据：

| 问题 | 证据（均为实测值，可复现） |
|---|---|
| 硬编码色值散落各处 | `vue/src/**` 中共 **319 处色值字面量**：`.vue` 文件中 216 处十六进制 + 81 处 `rgb()/rgba()` = 297 处；`style.css` 中 22 处。分布在 17 个 `.vue` 文件中 |
| 多套配色体系并存 | **5 套**互不相干的配色：① 绿色系（首页/登录页）② 蓝色系（前台导航/articleDialog/选中态）③ Flat-UI 明亮色板（后台 dashboard）④ 橙紫渐变（知识库/文章详情/情绪日记）⑤ 暖纸色（consultation 气泡与鼓励卡） |
| Vite 脚手架残留污染全站 | `vue/src/style.css` 是脚手架示例（`.hero`/`.counter`/`#next-steps`/`.ticks` 全站无引用），但其中 3 处在真实生效，见 4.3 |
| Element Plus 主题未定制 | 未覆盖任何 `--el-*` 变量，导致后台呈"原生 Element"观感；`emotions.vue` 甚至把 Element 默认色（`#409eff`/`#67c23a`/`#f56c6c`）手抄进 scoped 样式 |
| 视觉层级倒挂 | `Navbar.vue` 页面标题 `26px`，大于页面内容标题 |
| 交互状态缺失 | 前台导航无 `router-link-active` 选中态，用户无法判断当前所在页面 |
| 魔法数字与硬编码 | 首页 `min-height: calc(100vh - 215px)` 把导航+页脚高度写死；`Navbar` 的 `margin-right: 50px` |
| 强制样式覆盖 | `!important` **6 处**：`Sidebar.vue:103`、`consultation.vue:1126,1127`、`dashboard.vue:696,697`、`emotions.vue:380` |
| 颜色写进 template 内联 style | 含色值的内联 `style="..."` **4 处**：`articleDialog.vue:41,43,52`、`home.vue:14` |

各文件色值字面量分布（`Hex` = `#rrggbb` 形式，`RGB` = `rgb()/rgba()` 形式）：

| 文件 | Hex | RGB | 文件 | Hex | RGB |
|---|---|---|---|---|---|
| `views/consultation.vue` | 54 | 44 | `components/FrontendLayout.vue` | 7 | 0 |
| `views/dashboard.vue` | 47 | 14 | `components/articleDialog.vue` | 5 | 0 |
| `components/MarkdownRenderer.vue` | 22 | 0 | `components/Sidebar.vue` | 4 | 0 |
| `views/emotions.vue` | 18 | 0 | `components/AuthLayout.vue` | 3 | 8 |
| `views/consultations.vue` | 16 | 0 | `views/home.vue` | 2 | 8 |
| `views/articleDetail.vue` | 13 | 2 | `components/Navbar.vue` | 2 | 1 |
| `views/emotionDiary.vue` | 13 | 1 | `views/register.vue` | 1 | 0 |
| `style.css`（将删除） | 12 | 10 | `components/BackendLayout.vue` | 1 | 0 |
| `views/frontendKnowledge.vue` | 8 | 3 | `views/knowledge.vue` | 0 | 0 |

---

## 2. 目标与非目标

### 目标

1. 建立**全站唯一的设计令牌源**，色板、圆角、阴影、间距、字号全部变量化。
2. 通过覆盖 Element Plus 的 `--el-*` 变量，使 Element 组件自动跟随新色板，而非逐个组件写 `:deep()`。
3. 将 5 套配色收敛为 1 套，硬编码色值归零（可自动化复核）。
4. 引入现代心理健康产品的柔和治愈视觉语言：低饱和、大圆角、充足留白、统一交互状态。
5. 修复现存样式缺陷：深色模式混色、失效的 `color="transparent"`、导航无选中态、层级倒挂。

### 非目标

- 不改动 `<script>` 中的业务逻辑、接口调用、路由跳转行为、数据结构。
- 不新增任何 npm 依赖（不引入 Tailwind / UnoCSS / 动画库）。
- 不引入外链字体（避免加载失败与授权问题），沿用系统字体栈。
- 不做深色模式（仅做浅色，但全部变量驱动，为未来留好扩展点）。
- 不重构后端、不改数据库。

---

## 3. 设计令牌

### 3.1 色板 — 双阶主色

**设计约束的发现过程**：初版主色 `#5B9E8F` 经 WCAG 对比度核算属中间明度色——白字在其上仅 3.12:1，深字也仅 3.93:1，**两者都无法达到 AA 4.5:1**。这是该色调的数学限制，非参数可调。因此采用行业常规的双阶主色方案：柔和感由大面积色块承载，小面积交互元素用深一阶保证可读性。

| 令牌 | 色值 | 对比度 | 用途 |
|---|---|---|---|
| `--color-primary` | `#40776B` | 白字 5.16:1 ✅ / 浅底上 4.56:1 ✅ | 按钮底、链接文字、选中态文字、图标 |
| `--color-primary-soft` | `#5B9E8F` | 与白 3.12:1 ✅（非文字元素标准 3:1） | hero 渐变、登录页左栏、装饰图标、hover 增亮 |
| `--color-primary-light` | `#E9F3F0` | — | 主色浅底：选中背景、标签底、引用块底 |
| `--color-primary-dark` | `#2F5A51` | — | active / 按下态 |
| `--color-accent` | `#C97B4A` | 与白 3.27:1 ✅（非文字元素标准 3:1） | 情绪标签底、鼓励卡片底、装饰图标 |
| `--color-accent-text` | `#A85F2E` | 与白 4.84:1 ✅ | accent 系**文字**（情绪分值、鼓励文案） |
| `--color-accent-light` | `#FBF0E8` | — | 暖橙色浅底 |

> 说明：`--color-accent` 同样遵循双阶原则——`#C97B4A` 作底/图标（3.27:1 达标），文字场景必须用 `#A85F2E`。

### 3.2 背景与表面

| 令牌 | 色值 | 用途 |
|---|---|---|
| `--color-bg` | `#F7F9F8` | 页面底色（替代大面积纯白，使白卡片产生层次） |
| `--color-surface` | `#FFFFFF` | 卡片、面板、弹窗、表格 |
| `--color-border` | `#E6EDEA` | 分割线、卡片描边、输入框边框 |
| `--color-border-light` | `#F0F4F2` | 表格行线、极弱分隔、行内代码底 |

### 3.3 文字（带绿调深灰，不用纯黑）

| 令牌 | 色值 | 在 `#F7F9F8` 上的对比度 |
|---|---|---|
| `--color-text` | `#2C3A36` | 11.2:1 ✅ |
| `--color-text-secondary` | `#5F6F6A` | 5.0:1 ✅ |
| `--color-text-placeholder` | `#9AA8A3` | 2.3:1（占位符，属非必要信息，可接受） |

> 次级文字由初版 `#6B7B76`（4.45:1，差一点不达标）微调为 `#5F6F6A`。

### 3.4 语义色

语义色的取值标准是**「可以直接当文字用」**（≥4.5:1 on 白），因此整体偏深、偏灰——这是刻意选择，避免"错误提示像警报"的刺眼感。浅色变体仅用于标签底、提示条底。

| 令牌 | 深阶（可作文字） | 与白对比度 | 浅阶（仅作底色） |
|---|---|---|---|
| success | `#3D7A5F` | 5.06:1 ✅ | `#E7F2EC` |
| warning | `#8A6220` | 5.46:1 ✅ | `#F7EFE0` |
| danger | `#B85A52` | 4.54:1 ✅ | `#F8E9E7` |
| info | `#4A6E91` | 5.36:1 ✅ | `#E9F0F6` |

### 3.5 对比度规则（实施时逐处适用）

所有颜色使用必须满足以下规则，这是一条**机械可判定**的约束，不依赖主观判断：

- **作为文字**（字号 < 24px 常规，或 < 18.66px 加粗）→ 与背景对比度必须 **≥ 4.5:1**
- **作为大字号文字**（≥24px 常规 或 ≥18.66px 加粗）→ **≥ 3:1**
- **作为非文字元素**（图标、边框、图形、大面积色块）→ **≥ 3:1**

**已核算通过的组合**（实施时直接可用）：

| 前景 | 背景 | 对比度 |
|---|---|---|
| `#2C3A36` | `#F7F9F8` | 11.25:1 ✅ |
| `#2C3A36` | `#FFFFFF` | 11.90:1 ✅ |
| `#5F6F6A` | `#F7F9F8` | 5.00:1 ✅ |
| `#40776B` | `#FFFFFF` | 5.16:1 ✅ |
| `#40776B` | `#E9F3F0` | 4.56:1 ✅ |
| `#FFFFFF` | `#40776B` | 5.16:1 ✅ |
| `#5B9E8F` | `#FFFFFF` | 3.12:1 ✅（仅非文字/大字号） |
| `#A85F2E` | `#FFFFFF` | 4.84:1 ✅ |

实施阶段每引入一个新的「前景/背景」配对，都按上述公式核算；若某个语义色在其浅阶底上不足 4.5:1，则改用深阶文字配浅阶底时提高对比度（例如把浅阶再调浅一档），而非放宽标准。

### 3.6 形状、节奏、字体

| 类别 | 令牌 | 值 |
|---|---|---|
| 圆角 | `--radius-lg` / `--radius-md` / `--radius-sm` / `--radius-pill` | `16px` / `10px` / `8px` / `999px` |
| 阴影 | `--shadow-sm` / `--shadow-md` / `--shadow-lg` | `0 2px 8px rgba(44,58,54,.06)` / `0 8px 24px rgba(44,58,54,.08)` / `0 16px 40px rgba(44,58,54,.10)` |
| 间距 | `--space-1` … `--space-8` | `4 / 8 / 12 / 16 / 24 / 32 / 48 / 64 px` |
| 字号 | `--font-xs` … `--font-3xl` | `12 / 14 / 16 / 18 / 24 / 32 / 44 px` |
| 过渡 | `--transition-base` | `0.2s ease` |
| 字体 | `--font-sans` | `system-ui, -apple-system, 'Segoe UI', 'Microsoft YaHei', sans-serif` |

阴影刻意使用带绿调的深色 `rgba(44,58,54,·)` 而非纯黑，避免灰脏感。

---

## 4. 技术架构

### 4.1 文件结构（新增 6 个文件）

```
vue/src/styles/
├── tokens.scss          # 唯一令牌源：SCSS 变量（供 element-theme 计算）+ :root CSS 变量
├── _mixins.scss         # 断点变量 + 响应式 mixin，零 CSS 输出（唯一可被 .vue @use 的文件）
├── element-theme.scss   # Element Plus --el-* 覆盖，色阶由 tokens 的 SCSS 变量计算得出
├── base.scss            # 极简重置 + 4 个通用类
├── index.scss           # 统一入口，按固定顺序 @use
└── chart-palette.js     # ECharts 色板镜像（ECharts 无法消费 CSS 变量，详见 4.5 第 7 条）
```

**重要约束：`.vue` 文件永远不要 `@use '../styles/tokens'`。** 每个 SFC 的 `<style>` 是**独立编译单元**，`@use` 会把 `:root { ... }` 重复注入该组件的 scoped CSS 中（并被加上 scope 属性选择器而失效）。组件通过 CSS 自定义属性的**全局继承**读取令牌，无需任何 import；只有断点 mixin 需要 `@use '../styles/_mixins'`，因为该文件零 CSS 输出，重复引入无副作用。

### 4.2 引入顺序

```js
// vue/src/main.js
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import './styles/index.scss'   // 替换原来的 './style.css'
```

必须在 Element Plus CSS **之后**引入，否则 `--el-*` 覆盖不生效。此顺序通过实际渲染验证，不依赖假设。

### 4.3 `style.css` 的处置：删除

该文件是 Vite 脚手架残留（`.hero`/`.counter`/`#next-steps`/`.ticks` 全站无引用），但有 3 处在真实生效，删除后行为会变化。这些变化是缺陷修复，需明确记录：

| 现在生效的内容 | 删除后的变化 | 判定 |
|---|---|---|
| `:root { font: 18px/145% }` | 全站基准字号 18px → 16px | **修复**：18px 对管理后台明显偏大，且与 Element 自身 14px 基准长期冲突 |
| `#app { text-align: center }` | 文字不再被全局强制居中 | **修复**：大量页面现在需额外写 `text-align: left` 对抗它 |
| `#app { border-inline }` + `@media (prefers-color-scheme: dark)` | 左右边框消失；系统深色模式下不再变色 | **修复**：这是深色模式混色 bug 的根源 |

删除后执行逐页回归；若某页确实依赖居中，在该页显式声明，而非保留全局副作用。

### 4.4 Element Plus 覆盖策略

覆盖 `--el-*` 变量而非逐组件写 `:deep()`：

- `--el-color-primary` 及 `light-1` … `light-9`、`dark-2`：**不手写色值**，用 SCSS `color.mix()` 按 Element 官方配比从主色自动计算（`light-N = mix(#fff, primary, N*10%)`、`dark-2 = mix(#000, primary, 20%)`），未来换主色仅改一个变量。生成完整 1–9 阶而非仅 3/5/7/8/9，是因为 Element 内部部分组件会引用中间阶，缺阶会回退到默认色而造成偏色
- `--el-border-radius-base: 10px`（默认 4px，这是"圆润感"的关键改动点）
- `--el-text-color-primary / regular / secondary / placeholder`
- `--el-border-color / -light / -lighter`、`--el-fill-color / -light / -lighter / -blank`
- `--el-box-shadow / -light / -lighter`
- `--el-color-success / warning / danger / info` 及其 light 阶

覆盖范围覆盖 `el-button`、`el-input`、`el-select`、`el-table`、`el-menu`、`el-dialog`、`el-card`、`el-tag`、`el-pagination`、`el-form`、`el-dropdown`、`el-avatar` 等全部在用组件。

### 4.5 各 `.vue` 文件的改造规则

1. scoped 样式中的硬编码色值 → `var(--color-*)`
2. 圆角 / 阴影 / 间距 → `var(--radius-*)` / `var(--shadow-*)` / `var(--space-*)`
3. **结构性样式保留**：flex/grid、尺寸、定位等与视觉方向无关的声明不动，压缩改动面
4. `<template>` 仅做加容器、加分区、调层级；`<script>` 中业务逻辑、接口、路由行为不改
5. **允许替换 `<script>` 中的颜色字面量**（ECharts 配色、`getScoreColor()` 等），但数据结构、图表配置项结构、计算逻辑保持原样（用户已确认）
6. 响应式断点统一为 `768px` / `1024px` 两档，以 `_mixins.scss` 的 mixin 表达（CSS 变量无法用于媒体查询）
7. **`<script>` 中的图表色值集中到 `styles/chart-palette.js`**：ECharts 的 `color`/`borderColor` 参数需要真实色值，无法传入 `var(--token)`。因此设立单一边界的色板模块作为令牌镜像，`dashboard.vue` 与 `emotions.vue` 从它导入；该文件与 `tokens.scss` 的色值一致性由实施计划中的**校验步骤**机械保证，而非依赖注释约定
8. **含颜色值的内联 `style="..."` 全部收进 scoped 样式**：共 4 处，分布在 `articleDialog.vue:41,43,52` 与 `home.vue:14`。不含颜色值的内联样式（纯尺寸/间距）不在本次范围

### 4.6 通用类（刻意保持极少）

`base.scss` 仅提供 4 个，避免演化成"第二套体系"：
`.page-container`、`.app-card`、`.section-title`、`.text-secondary`。

---

## 5. 逐布局、逐页面改造方案

### 5.1 布局层（3 个）

**`FrontendLayout.vue`（前台导航 + 页脚）**

- 导航 `#fff → #4096ff` 蓝渐变 → 白色半透明玻璃条 + 底部 `--color-border` 细线；绿色让给 hero 与主按钮，视觉重心清晰
- **新增导航选中态**：`router-link-active` + `--color-primary-light` 圆角药丸，解决"用户不知道在哪一页"
- 品牌名 `24px → 20px`，颜色改 `--color-text`
- 页脚 `#1f2937` 深色大块 → 浅底 + 顶部细线 + 次要文字色（深色页脚在治愈向产品中过重）
- `height: 30px` + `padding: 20px` 写死高度 → `min-height` 自适应
- "退出登录"裸按钮 → 次级描边按钮

**`AuthLayout.vue`（登录 / 注册）**

- 左栏平面绿渐变 → `--color-primary-soft` → `--color-primary` 渐变 + 一层极淡径向光斑，营造安静质感
- 文案容器 `width: 460px` → `max-width`（窄屏防溢出）
- 右栏 `#fff` → `--color-surface`
- robot 圆形玻璃卡保留，阴影替换为令牌

**`BackendLayout.vue` + `Sidebar.vue` + `Navbar.vue`（后台）**

- 主内容底 `#e4e7ec` → `--color-bg`；内容区从"无圆角纯白大块" → `--radius-lg` 圆角白卡 + `--shadow-sm`
- 侧边栏选中态 `#e6ecf7`（蓝调，且带 `!important`）→ `--color-primary-light` 底 + 左侧 3px 主色竖条 + 主色文字；菜单项 `--radius-md`、左右内缩 8px
- 侧边栏品牌区 3 处硬编码色 → 令牌
- 顶部栏页面标题 `26px → 20px`（修复层级倒挂）；阴影换令牌；移除 `margin-right: 50px` 硬编码
- 清除全部 `!important`

### 5.2 前台页面（5 个）

| 页面 | 改造动作 |
|---|---|
| `home.vue` | 渐变换新双阶主色；`#ffd700` 金色高亮 → `--color-accent`；**修复 `color="transparent"`**（Element 的 `color` 仅接受颜色值，该写法失效）改为幽灵按钮；`calc(100vh - 215px)` 魔法数字 → flex 自适应；**新增"信任点"区块**（隐私保护 / 随时可用 / 专业内容，用户已确认） |
| `consultation.vue` | 消除第 ⑤ 套暖纸色：用户气泡 → `--color-primary` 底白字，AI 气泡 → `--color-surface` 底 + 细描边；`#999/#666/#333` 系列灰 → 三级文字令牌；粉色情绪选择区 `#ff9a9e → #fecfef` → accent 系；`#fb923c → #f59e0b` 橙渐变 → 主色系 |
| `emotionDiary.vue` | 背景三色渐变 → `--color-bg`；标题渐变 `#7ed321 → #f5a623` → 主色/accent 系；情绪选中态 `#7ed321` / `#f0fdf4` → 主色令牌；卡片圆角统一 `--radius-lg` |
| `frontendKnowledge.vue` + `articleDetail.vue` | 消除第 ④ 套橙紫渐变 `#f59e0b → #8b5cf6`；`border-left: 4px solid #f59e0b` → accent 令牌；卡片 `--radius-lg` + hover 轻抬升（`translateY(-2px)` + `--shadow-md`） |
| `MarkdownRenderer.vue` | GitHub 风格色值（`#3b82f6` 链接、`#eff6ff` 蓝底、`#e11d48` 删除线、`#f6f8fa` 代码底）→ 映射为令牌；正文行高提至 `1.75` 改善阅读；仅换色与排版，不改结构 |

### 5.3 后台页面（4 个）

| 页面 | 改造动作 |
|---|---|
| `knowledge.vue` + `articleDialog.vue` | PageHead / 表格 / 弹窗统一令牌；`articleDialog.vue` 中 **3 处写在 template 内的内联 `style`**（`#dcdfe6` 边框）收进 scoped 样式 |
| `consultations.vue` | `#f8f9fa/#e9ecef/#e8f4fd/#f0f9f0` 浅灰蓝绿一整套 → 全部换令牌；用户/AI 气泡区分色改主色浅底 |
| `emotions.vue` | 删除手抄的 Element 默认色（`#409eff/#67c23a/#f56c6c/#ebeef5/#303133/#909399/#606266`），改为继承覆盖后的 Element 变量；`getScoreColor()` 中 4 个硬编码色值 → 语义令牌 |
| `dashboard.vue` | 消除第 ③ 套 Flat-UI 色板：4 个指标卡渐变（`#667eea→#764ba2`、`#f093fb→#f5576c`、`#4facfe→#00f2fe`、`#43e97b→#38f9d7`）→ 主色/accent 系；ECharts 配色（`#2d3436`、`#fab1a0`、`#ffeaa7`、`#74b9ff`、`#a29bfe`、`#fdcb6e`、`#00b894` 等 20 余处）→ 令牌化色值；仅替换颜色字面量，图表配置结构不变 |

---

## 6. 已确认的决策清单

| # | 决策项 | 结论 |
|---|---|---|
| 1 | 目标方向 | 视觉焕新，现代心理健康产品的柔和治愈风 |
| 2 | 覆盖范围 | 三套布局全改：前台 + 登录注册 + 后台 |
| 3 | 深色模式 | 只做浅色，但全部变量驱动，并修复现有深色变量冲突 |
| 4 | 改动自由度 | 可调 template 结构与新增样式组件，不改业务逻辑与接口 |
| 5 | 技术方案 | 方案 A：设计令牌 + Element Plus 主题覆盖（零新依赖） |
| 6 | `<script>` 边界 | 允许替换颜色字面量；数据结构与计算逻辑不动 |
| 7 | 首页内容 | 新增一排信任点（隐私保护 / 随时可用 / 专业内容） |
| 8 | 主色方案 | 接受双阶主色：`#5B9E8F`（大面积）+ `#40776B`（小面积） |
| 9 | 分支策略 | 不建新分支，在当前分支修改 |

---

## 7. 验证方案

### 7.1 可自动化硬指标

**色值字面量的白名单只有两个文件**：`vue/src/styles/tokens.scss`（CSS 令牌唯一源）与 `vue/src/styles/chart-palette.js`（图表色板镜像）。除这两个文件外，`vue/src/**` 中不得出现任何色值字面量。

| 检查项 | 改前（实测） | 改后要求 |
|---|---|---|
| `npm run build --prefix vue` | **通过**（1.42s，仅两条无关第三方警告） | **必须通过**（捕获 SCSS 变量未定义等编译错误） |
| `vue/src/**` 中 `.vue` 文件的色值字面量 | **297 处**（216 十六进制 + 81 `rgb()/rgba()`） | **0 处** |
| `vue/src/style.css` 的色值字面量 | 22 处（12 + 10） | 文件删除，指标消失 |
| `!important` | **6 处** | **0 处**；若某处经证明无法去除，须书面说明原因并单独报告，不得静默保留 |
| 含色值的内联 `style="..."` | **4 处** | 0 处 |

**构建的环境前提（实测得出）**：本沙箱（`workspace-write`）下 `vite build` 必然失败于 `spawn EPERM`——Vite 内部的 `optimizeSafeRealPathSync` 调用 `child_process.exec`，受限沙箱禁止以管道捕获子进程输出。这不是代码问题，且改动命令写法无法绕过（失败发生在 Vite 内部）。执行时必须以 `danger-full-access` 运行构建命令。构建基线已用该方式实测通过。

### 7.2 运行时逐页回归（11 条路由）

`/home`、`/consultation`、`/emotionDiary`、`/knowledge`、`/knowledge/article/:id`、`/auth/login`、`/auth/register`、`/back/dashboard`、`/back/knowledge`、`/back/consultations`、`/back/emotions`

每页确认：布局不错位、文字不溢出、Element 组件外观正常、浏览器控制台无报错。

### 7.3 能力边界（重要）

- **Agent 无法截图**，因此"视觉美观度"的最终判定必须由用户在浏览器中完成。Agent 能保证的是：构建通过、硬编码归零、控制台无报错、对比度达标。
- 每批改造完成后启动 dev server 并告知地址，用户刷新确认，不满意即时调整。
- 若需改前/改后像素级对照，可临时安装 Playwright 自动截图（增加约 5 分钟与一次依赖安装，用完卸载）。**默认不安装**，由用户决定。
- 登录 / 后台 / 数据页面依赖后端接口。若后端无法启动，则以空态页面完成样式核对，并明确标注哪些页面未能完整验证。

### 7.4 对比度核对

所有文字/背景组合按 WCAG 2.1 公式核算，目标：正文文字 ≥ 4.5:1，大字号与 UI 组件边界 ≥ 3:1。第 3.5 节的对比度规则与此处的验收是同一套约束；第 3.1 节的双阶主色方案即由此约束推导得出。

---

## 8. 风险与回滚

| 风险 | 应对 |
|---|---|
| 删除 `style.css` 后基准字号 18px → 16px，部分页面观感变化 | 逐页回归；发现错位则在该页显式声明，而非保留全局副作用 |
| `--el-*` 覆盖不生效（引入顺序错误） | 以实际渲染验证，不依赖假设；顺序错误时会在 7.2 逐页检查中暴露 |
| 图表配色改动影响可读性 | 仅替换颜色字面量，图表配置结构不变；改后逐图目视确认 |
| 用户现有未提交改动被误改 | `vue/src/router/index.js`、`vue/src/views/dashboard.vue`、`springboot/src/main/resources/application.yml` **不触碰**；`dashboard.vue` 只改样式与颜色字面量，不碰其他未提交内容 |
| 改动面大，难以回滚 | 不建分支（用户确认）；依靠 git 对已知提交 `d7f469e` 的回滚能力，每批改造后提交一次，保证回滚粒度可控 |

---

## 9. 明确不做的事

- 不新增任何 npm 依赖
- 不引入外链字体或图标库
- 不做深色模式
- 不改后端、接口、数据库、路由跳转行为
- 不改动 `<script>` 中除颜色字面量以外的任何内容
- 不重构组件拆分或文件组织（除新增 `styles/` 目录外）

---

## 10. 实施顺序（概要，详细计划见后续 implementation plan）

1. 建立 `styles/` 四个文件与令牌体系，调整 `main.js` 引入，删除 `style.css` → 验证构建通过
2. 改造布局层（3 个布局 + Sidebar + Navbar）→ 逐页回归
3. 改造前台 5 个页面 → 逐页回归
4. 改造后台 4 个页面（含 ECharts 配色）→ 逐页回归
5. 全局硬编码归零核查 + 对比度复核 + 完整 11 路由回归

---

## 11. 附：审查中发现但**不在本次范围**的问题

在做样式审计时顺带读到两处非样式问题，**本次不改**，仅记录在案以免遗忘。若要修，应作为独立任务单独分类、单独批准。

1. **文件名大小写不匹配（会在 Linux 环境构建失败）**
   `vue/src/router/index.js:38` 导入 `@/views/FrontendKnowledge.vue`（大写 `F`），但磁盘上的实际文件名是 `frontendKnowledge.vue`（小写 `f`）。Windows 文件系统大小写不敏感，所以本地开发正常；一旦部署到 Linux 容器或 CI，`vite build` 会直接报模块找不到而失败。

2. **`main.js` 中组件注册晚于挂载**
   `vue/src/main.js:23` 先 `app.mount('#app')`，第 25-27 行才注册 Element Plus 图标组件。当前之所以能正常显示，是因为路由视图是懒加载、在挂载之后才渲染的——属于**依赖时序的侥幸**。若某个图标被同步渲染的组件使用（例如 `App.vue` 直接写 `<Expand />`），就会报组件未找到。


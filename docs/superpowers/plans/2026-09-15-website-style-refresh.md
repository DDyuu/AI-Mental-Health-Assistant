# 网站整体样式焕新 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpower-subagent-driven-development (recommended) or superpower-executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 Vue 3 前端建立一套设计令牌体系并覆盖 Element Plus 主题，把 5 套互不相干的配色与 319 处色值字面量收敛为单一来源，同时修复深色模式混色、失效按钮写法、导航无选中态等样式缺陷。

**Architecture:** 新增 `vue/src/styles/` 六文件令牌层（`tokens.scss` 为唯一色值来源，`_mixins.scss` 提供断点，`element-theme.scss` 用 `color.mix()` 从主色派生 Element 全阶变量，`base.scss` 提供全局重置与 4 个通用类，`index.scss` 为入口，`chart-palette.js` 作为 ECharts 色板镜像）。组件通过 CSS 自定义属性的全局继承读取令牌，**不需要也不允许 `@use` tokens.scss**（SFC 的 `<style>` 是独立编译单元，`@use` 会把 `:root` 重复注入并被 scope 属性选择器破坏）。随后按布局层 → 前台页 → 组件 → 后台页的顺序逐文件替换色值字面量。

**Tech Stack:** Vue 3.5、Vite 8（rolldown）、Element Plus 2.14、ECharts 6、Dart Sass 1.97（`sass` 装在仓库根 `node_modules`，靠 Node 向上解析被 `vue/` 使用）

**Spec:** `docs/superpowers/specs/2026-09-15-website-style-refresh-design.md`

## Global Constraints

以下为 spec 的项目级约束，**每个任务的要求都隐含包含本节**：

- 不新增任何 npm 依赖（不引入 Tailwind / UnoCSS / 动画库 / 外链字体）
- 不引入外链字体或图标库；字体栈固定为 `system-ui, -apple-system, 'Segoe UI', 'Microsoft YaHei', 'PingFang SC', sans-serif`
- 不做深色模式；仅浅色主题，但全部变量驱动
- 不改后端、接口、数据库、路由跳转行为
- `<script>` 中**只允许**替换颜色字面量；数据结构、接口调用、计算逻辑、路由行为一行不动
- `<template>` 只做加容器、加分区、调层级；不新增业务逻辑
- **色值字面量只允许出现在两个文件**：`vue/src/styles/tokens.scss` 与 `vue/src/styles/chart-palette.js`
- `.vue` 文件**永远不要** `@use '../styles/tokens'`；只有断点 mixin 可 `@use '../styles/mixins'`（相对路径，不依赖 Vite `@` 别名在 SCSS 中的解析行为）
- 响应式断点只有两档：`768px`（`m` mixin）、`1024px`（`m.below-md`）
- 通用类只有 4 个：`.page-container`、`.app-card`、`.section-title`、`.text-secondary`
- 基准字号 `html { font-size: 16px }`（原 `style.css` 的 18px 被移除）
- 不添加全局 `* { box-sizing: border-box }`（会静默改变所有已设定尺寸元素的盒模型，收益不抵风险）
- **不触碰用户未提交的改动**：`vue/src/router/index.js`、`vue/src/views/dashboard.vue`、`springboot/src/main/resources/application.yml`。`dashboard.vue` 在 Task 13 会被修改，但**只改样式与颜色字面量**，不得覆盖其已有的未提交改动
- 对比度规则：作为文字 ≥ 4.5:1；大字号（≥24px 常规 或 ≥18.66px 加粗）与非文字元素 ≥ 3:1
- 每个任务结束必须提交一次 git commit（保证回滚粒度）

## 环境前提（实测得出，每个任务都适用）

| 事项 | 结论 |
|---|---|
| 构建命令 | `npm run build --prefix vue`（在仓库根执行） |
| **构建必须有 `danger-full-access`** | 在默认 `workspace-write` 沙箱下 `vite build` 必然失败于 `spawn EPERM`：Vite 内部 `optimizeSafeRealPathSync` 调用 `child_process.exec`，受限沙箱禁止以管道捕获子进程输出。**这不是代码问题，改命令写法无法绕过**。执行时对构建与 dev server 命令一律带 `sandbox_permissions: danger-full-access` |
| 构建基线 | 已实测通过：`✓ built in 1.42s`，输出两条无关第三方警告（`@vueuse/core` 的 `#__PURE__` 注解位置、chunk > 500kB）。这些警告是既有状态，**不是本次引入的，不要试图修复** |
| dev server | `npm run dev --prefix vue`，地址 `http://localhost:5173`（`strictPort: true`），`/api` 代理到 `http://localhost:8080`。Task 1 启动后**保持常驻**，后续任务靠 HMR 自动更新 |
| 后端 | 登录/后台数据页依赖 `http://localhost:8080`。若后端不可用，只核对空态与静态样式，并在该任务的提交说明中标注"未完成数据态验证" |

## 通用验证命令（`AUDIT`）

后文所有任务用 `AUDIT` 指代下面这段命令，在**仓库根**执行。它统计 `vue/src/**` 中每个文件的色值字面量数量，白名单为 `tokens.scss` 与 `chart-palette.js`：

```powershell
$whitelist = @('tokens.scss','chart-palette.js')
$files = Get-ChildItem -Recurse vue\src -Include *.vue,*.css,*.js,*.scss | Where-Object { $_.FullName -notmatch 'node_modules' }
$rows = foreach ($f in $files) {
  if ($whitelist -contains $f.Name) { continue }
  $t = Get-Content $f.FullName -Raw
  $n = ([regex]::Matches($t,'#[0-9a-fA-F]{3,8}\b').Count) + ([regex]::Matches($t,'rgba?\([^)]*\)').Count)
  if ($n -gt 0) { [pscustomobject]@{ File = $f.FullName.Replace('E:\WorkSpace\AI_Mental_Health_Assistant\vue\src\',''); Count = $n } }
}
$rows | Sort-Object Count -Descending | Format-Table -AutoSize
"TOTAL = " + (($rows | Measure-Object Count -Sum).Sum)
```

改前基线（已实测）：`TOTAL = 319`（`.vue` 297 + `style.css` 22）。全部任务完成后必须为 `TOTAL = 0`。

## 环境变量与令牌命名总表（各任务必须使用这些确切名称）

```
颜色   --color-primary / -soft / -dark / -light / -wash / -wash-strong
       --color-accent / -text / -light / -wash / -wash-strong
       --color-bg / --color-surface
       --color-border / --color-border-light
       --color-text / -secondary / -placeholder / -inverse
       --color-success / -light / -wash-strong
       --color-warning / -light
       --color-danger / -light / -wash
       --color-info / -light / -wash-strong
       --color-code-bg / --color-code-text
       --alpha-02/05/10/15/20/25/30/50/60/70/80/90/95/98      （白色透明层）
       --scrim-04/06/08/10/12/30/40                            （深色透明层）
圆角   --radius-sm(8) --radius-md(10) --radius-lg(16) --radius-pill(999)
阴影   --shadow-sm --shadow-md --shadow-lg
间距   --space-1(4) … --space-8(64)  即 4/8/12/16/24/32/48/64
字号   --font-xs(12) --font-sm(14) --font-md(16) --font-lg(18) --font-xl(24) --font-2xl(32) --font-3xl(44)
其它   --font-sans --font-mono --transition-base
```

---

### Task 1: 建立令牌层与基础层

**Files:**
- Create: `vue/src/styles/tokens.scss`
- Create: `vue/src/styles/_mixins.scss`
- Create: `vue/src/styles/element-theme.scss`
- Create: `vue/src/styles/base.scss`
- Create: `vue/src/styles/index.scss`
- Modify: `vue/src/main.js:5`
- Delete: `vue/src/style.css`

**Interfaces:**
- Consumes: 无（本任务是整个计划的基础）
- Produces: 全部 CSS 自定义属性（名称见上文总表）；`_mixins.scss` 导出 `$bp-sm: 768px`、`$bp-md: 1024px` 与 mixin `below-sm`、`below-md`、`above-md`，供后续所有任务以 `@use '../styles/mixins' as m;` 使用

- [ ] **Step 1: 记录改前基线（红）**

Run: `AUDIT`
Expected: `TOTAL = 319`，其中 `style.css` 22、`views\consultation.vue` 98、`views\dashboard.vue` 61 为前三大

- [ ] **Step 2: 创建 `vue/src/styles/tokens.scss`**

```scss
// ============================================================================
// 设计令牌唯一源 —— 全站唯一允许出现色值字面量的 SCSS 文件
//
// 使用约定（违反会导致样式失效，务必遵守）：
//   * .vue 组件直接写 var(--token)，**不要** @use 本文件。
//     SFC 的 <style> 是独立编译单元，@use 会把 :root 重复注入该组件的 scoped CSS，
//     并被加上 scope 属性选择器而失效。
//   * element-theme.scss 用 `@use './tokens' as t` 取 $c-* 变量做派生计算。
//   * ECharts 色值见 chart-palette.js（本文件的镜像，由 npm run check:tokens 校验）。
// ============================================================================

// ---------------------------------------------------------------------------
// 1. SCSS 变量：仅供同一次编译内的计算（Element 色阶派生）
// ---------------------------------------------------------------------------
$c-white: #FFFFFF;
$c-black: #000000;

$c-primary: #40776B;
$c-primary-soft: #5B9E8F;
$c-primary-dark: #2F5A51;
$c-primary-light: #E9F3F0;

$c-accent: #C97B4A;
$c-accent-text: #A85F2E;
$c-accent-light: #FBF0E8;

$c-bg: #F7F9F8;
$c-surface: #FFFFFF;
$c-border: #E6EDEA;
$c-border-light: #F0F4F2;

$c-text: #2C3A36;
$c-text-secondary: #5F6F6A;
$c-text-placeholder: #9AA8A3;

$c-success: #3D7A5F;
$c-success-light: #E7F2EC;
$c-warning: #8A6220;
$c-warning-light: #F7EFE0;
$c-danger: #B85A52;
$c-danger-light: #F8E9E7;
$c-info: #4A6E91;
$c-info-light: #E9F0F6;

// ---------------------------------------------------------------------------
// 2. 运行时 CSS 自定义属性：全站唯一样式来源
// ---------------------------------------------------------------------------
:root {
  /* 品牌与主色（双阶：soft 用于大面积，primary 用于小面积交互元素） */
  --color-primary: #{$c-primary};
  --color-primary-soft: #{$c-primary-soft};
  --color-primary-dark: #{$c-primary-dark};
  --color-primary-light: #{$c-primary-light};
  --color-primary-wash: rgba(64, 119, 107, 0.08);
  --color-primary-wash-strong: rgba(64, 119, 107, 0.24);

  /* 强调色（暖橙） */
  --color-accent: #{$c-accent};
  --color-accent-text: #{$c-accent-text};
  --color-accent-light: #{$c-accent-light};
  --color-accent-wash: rgba(201, 123, 74, 0.10);
  --color-accent-wash-strong: rgba(201, 123, 74, 0.24);

  /* 背景与表面 */
  --color-bg: #{$c-bg};
  --color-surface: #{$c-surface};

  /* 边框 */
  --color-border: #{$c-border};
  --color-border-light: #{$c-border-light};
  /* 功能性边框：仅用于表单控件的可识别边界（对白 3.28:1、对 --color-bg 3.11:1）。
     装饰性分隔线、卡片描边、表格行线仍用上面两个软令牌，见 spec §3.5 例外二。 */
  --color-border-strong: #83918C;

  /* 文字 */
  --color-text: #{$c-text};
  --color-text-secondary: #{$c-text-secondary};
  --color-text-placeholder: #{$c-text-placeholder};
  --color-text-inverse: #{$c-white};

  /* 语义色（深阶可直接作文字，浅阶仅作底色） */
  --color-success: #{$c-success};
  --color-success-light: #{$c-success-light};
  --color-success-wash-strong: rgba(61, 122, 95, 0.40);
  --color-warning: #{$c-warning};
  --color-warning-light: #{$c-warning-light};
  --color-danger: #{$c-danger};
  --color-danger-light: #{$c-danger-light};
  --color-danger-wash: rgba(184, 90, 82, 0.10);
  --color-info: #{$c-info};
  --color-info-light: #{$c-info-light};
  --color-info-wash-strong: rgba(74, 110, 145, 0.30);

  /* 代码块（MarkdownRenderer 用） */
  --color-code-bg: #22332E;
  --color-code-text: #E8F1EE;

  /* 白色透明层阶梯（彩色/深色底上的叠加，取代散落的 rgba(255,255,255,x)） */
  --alpha-02: rgba(255, 255, 255, 0.02);
  --alpha-05: rgba(255, 255, 255, 0.05);
  --alpha-10: rgba(255, 255, 255, 0.10);
  --alpha-15: rgba(255, 255, 255, 0.15);
  --alpha-20: rgba(255, 255, 255, 0.20);
  --alpha-25: rgba(255, 255, 255, 0.25);
  --alpha-30: rgba(255, 255, 255, 0.30);
  --alpha-50: rgba(255, 255, 255, 0.50);
  --alpha-60: rgba(255, 255, 255, 0.60);
  --alpha-70: rgba(255, 255, 255, 0.70);
  --alpha-80: rgba(255, 255, 255, 0.80);
  --alpha-90: rgba(255, 255, 255, 0.90);
  --alpha-95: rgba(255, 255, 255, 0.95);
  --alpha-98: rgba(255, 255, 255, 0.98);

  /* 深色透明层阶梯（阴影/分隔/弱化），色相取自 --color-text 而非纯黑 */
  --scrim-04: rgba(44, 58, 54, 0.04);
  --scrim-06: rgba(44, 58, 54, 0.06);
  --scrim-08: rgba(44, 58, 54, 0.08);
  --scrim-10: rgba(44, 58, 54, 0.10);
  --scrim-12: rgba(44, 58, 54, 0.12);
  --scrim-30: rgba(44, 58, 54, 0.30);
  --scrim-40: rgba(44, 58, 54, 0.40);

  /* 圆角 */
  --radius-sm: 8px;
  --radius-md: 10px;
  --radius-lg: 16px;
  --radius-pill: 999px;

  /* 阴影 */
  --shadow-sm: 0 2px 8px rgba(44, 58, 54, 0.06);
  --shadow-md: 0 8px 24px rgba(44, 58, 54, 0.08);
  --shadow-lg: 0 16px 40px rgba(44, 58, 54, 0.10);

  /* 间距阶梯 */
  --space-1: 4px;
  --space-2: 8px;
  --space-3: 12px;
  --space-4: 16px;
  --space-5: 24px;
  --space-6: 32px;
  --space-7: 48px;
  --space-8: 64px;

  /* 字号阶梯 */
  --font-xs: 12px;
  --font-sm: 14px;
  --font-md: 16px;
  --font-lg: 18px;
  --font-xl: 24px;
  --font-2xl: 32px;
  --font-3xl: 44px;

  /* 字体与动效 */
  --font-sans: system-ui, -apple-system, 'Segoe UI', 'Microsoft YaHei', 'PingFang SC', sans-serif;
  --font-mono: ui-monospace, Consolas, 'Courier New', monospace;
  --transition-base: 0.2s ease;
}
```

- [ ] **Step 3: 创建 `vue/src/styles/_mixins.scss`**

```scss
// 断点与响应式 mixin —— 零 CSS 输出，因此可被任意 .vue 安全 @use（不会重复注入 :root）
// 用法（组件内，相对路径）：
//   @use '../styles/mixins' as m;
//   @include m.below-md { font-size: var(--font-sm); }
$bp-sm: 768px;
$bp-md: 1024px;

@mixin below-sm {
  @media (max-width: #{$bp-sm}) {
    @content;
  }
}

@mixin below-md {
  @media (max-width: #{$bp-md}) {
    @content;
  }
}

@mixin above-md {
  @media (min-width: #{$bp-md + 1}) {
    @content;
  }
}
```

- [ ] **Step 4: 创建 `vue/src/styles/element-theme.scss`**

```scss
// Element Plus 主题覆盖
// 必须在 element-plus/dist/index.css 之后加载（由 index.scss 的 @use 顺序 + main.js 的
// import 顺序共同保证），否则同优先级下会被 Element 默认值覆盖。
@use 'sass:color';
@use './tokens' as t;

@function mix-light($c, $n) {
  @return color.mix(t.$c-white, $c, $n * 10%);
}
@function mix-dark($c, $n) {
  @return color.mix(t.$c-black, $c, $n * 10%);
}

:root {
  /* 主色 + 1..9 全部浅阶 + 深阶（Element 部分组件会引用中间阶，缺阶会回退默认色造成偏色） */
  --el-color-primary: #{t.$c-primary};
  --el-color-primary-light-1: #{mix-light(t.$c-primary, 1)};
  --el-color-primary-light-2: #{mix-light(t.$c-primary, 2)};
  --el-color-primary-light-3: #{mix-light(t.$c-primary, 3)};
  --el-color-primary-light-4: #{mix-light(t.$c-primary, 4)};
  --el-color-primary-light-5: #{mix-light(t.$c-primary, 5)};
  --el-color-primary-light-6: #{mix-light(t.$c-primary, 6)};
  --el-color-primary-light-7: #{mix-light(t.$c-primary, 7)};
  --el-color-primary-light-8: #{mix-light(t.$c-primary, 8)};
  --el-color-primary-light-9: #{mix-light(t.$c-primary, 9)};
  --el-color-primary-dark-2: #{mix-dark(t.$c-primary, 2)};

  --el-color-success: #{t.$c-success};
  --el-color-success-light-3: #{mix-light(t.$c-success, 3)};
  --el-color-success-light-5: #{mix-light(t.$c-success, 5)};
  --el-color-success-light-7: #{mix-light(t.$c-success, 7)};
  --el-color-success-light-8: #{mix-light(t.$c-success, 8)};
  --el-color-success-light-9: #{mix-light(t.$c-success, 9)};
  --el-color-success-dark-2: #{mix-dark(t.$c-success, 2)};

  --el-color-warning: #{t.$c-warning};
  --el-color-warning-light-3: #{mix-light(t.$c-warning, 3)};
  --el-color-warning-light-5: #{mix-light(t.$c-warning, 5)};
  --el-color-warning-light-7: #{mix-light(t.$c-warning, 7)};
  --el-color-warning-light-8: #{mix-light(t.$c-warning, 8)};
  --el-color-warning-light-9: #{mix-light(t.$c-warning, 9)};
  --el-color-warning-dark-2: #{mix-dark(t.$c-warning, 2)};

  --el-color-danger: #{t.$c-danger};
  --el-color-danger-light-3: #{mix-light(t.$c-danger, 3)};
  --el-color-danger-light-5: #{mix-light(t.$c-danger, 5)};
  --el-color-danger-light-7: #{mix-light(t.$c-danger, 7)};
  --el-color-danger-light-8: #{mix-light(t.$c-danger, 8)};
  --el-color-danger-light-9: #{mix-light(t.$c-danger, 9)};
  --el-color-danger-dark-2: #{mix-dark(t.$c-danger, 2)};

  /* Element 同时存在 danger 与 error 两套变量名，必须都覆盖 */
  --el-color-error: #{t.$c-danger};
  --el-color-error-light-3: #{mix-light(t.$c-danger, 3)};
  --el-color-error-light-5: #{mix-light(t.$c-danger, 5)};
  --el-color-error-light-7: #{mix-light(t.$c-danger, 7)};
  --el-color-error-light-8: #{mix-light(t.$c-danger, 8)};
  --el-color-error-light-9: #{mix-light(t.$c-danger, 9)};
  --el-color-error-dark-2: #{mix-dark(t.$c-danger, 2)};

  --el-color-info: #{t.$c-info};
  --el-color-info-light-3: #{mix-light(t.$c-info, 3)};
  --el-color-info-light-5: #{mix-light(t.$c-info, 5)};
  --el-color-info-light-7: #{mix-light(t.$c-info, 7)};
  --el-color-info-light-8: #{mix-light(t.$c-info, 8)};
  --el-color-info-light-9: #{mix-light(t.$c-info, 9)};
  --el-color-info-dark-2: #{mix-dark(t.$c-info, 2)};

  /* 文字 */
  --el-text-color-primary: #{t.$c-text};
  --el-text-color-regular: #{t.$c-text-secondary};
  --el-text-color-secondary: #{t.$c-text-secondary};
  --el-text-color-placeholder: #{t.$c-text-placeholder};
  --el-text-color-disabled: #{t.$c-text-placeholder};

  /* 边框 */
  --el-border-color: #{t.$c-border};
  --el-border-color-light: #{t.$c-border};
  --el-border-color-lighter: #{t.$c-border-light};
  --el-border-color-extra-light: #{t.$c-border-light};

  /* 表单控件的可识别边界（≥3:1）不在这里，见文件末尾：Element 把它声明在组件上，
     写在 :root 会被遮蔽。装饰性边框继续用上面四个软令牌。 */

  /* 填充 */
  --el-fill-color: #{t.$c-border-light};
  --el-fill-color-light: #{t.$c-border-light};
  --el-fill-color-lighter: #{t.$c-bg};
  --el-fill-color-extra-light: #{t.$c-bg};
  --el-fill-color-blank: #{t.$c-surface};

  /* 背景 */
  --el-bg-color: #{t.$c-surface};
  --el-bg-color-page: #{t.$c-bg};
  --el-bg-color-overlay: #{t.$c-surface};

  /* 圆角：默认 4px，这是"圆润感"的关键改动点 */
  --el-border-radius-base: 10px;
  --el-border-radius-small: 8px;
  --el-border-radius-round: 999px;
  --el-border-radius-circle: 100%;

  /* 阴影 */
  --el-box-shadow: 0 8px 24px rgba(44, 58, 54, 0.08);
  --el-box-shadow-light: 0 2px 8px rgba(44, 58, 54, 0.06);
  --el-box-shadow-lighter: 0 2px 8px rgba(44, 58, 54, 0.04);
  --el-box-shadow-dark: 0 16px 40px rgba(44, 58, 54, 0.10);

  /* 字号 */
  --el-font-size-base: 14px;
  --el-font-size-small: 13px;
  --el-font-size-extra-small: 12px;
  --el-font-size-medium: 16px;
  --el-font-size-large: 18px;
}

/* ---------------------------------------------------------------------------
   表单控件的可识别边界提到 ≥3:1（WCAG 1.4.11）。装饰性边框仍走软令牌。
   焦点态本就是 --el-color-primary，无需覆盖。

   **为什么这段不在上面的 :root 里**（实测结论，勿改回去）：
   Element Plus 把 --el-input-border-color / --el-input-hover-border-color 声明在
   **组件选择器**上，而不是 :root。自定义属性遵循普通继承与层叠规则——元素自身的
   声明会**遮蔽**从 :root 继承来的值，与优先级和加载顺序无关。所以写在 :root 的
   覆盖是死代码（headless 浏览器实测：.el-input__wrapper 的边框仍解析为软灰值）。
   覆盖必须落在同一批组件选择器上；本文件在 element-plus/dist/index.css 之后加载，
   同优先级下后者胜出，故此处生效。
   --------------------------------------------------------------------------- */
.el-input,
.el-textarea,
.el-date-editor,
.el-autocomplete {
  --el-input-border-color: var(--color-border-strong);
  --el-input-hover-border-color: var(--color-primary);
}
```

- [ ] **Step 5: 创建 `vue/src/styles/base.scss`**

```scss
// 全局重置 + 4 个通用类。刻意保持极少，避免演化成"第二套体系"。
// 注意：不添加 * { box-sizing: border-box }——它会静默改变所有已设定尺寸元素的盒模型。

html {
  font-size: 16px; /* 取代原 style.css 的 18px 基准 */
}

body {
  margin: 0;
  font-family: var(--font-sans);
  font-size: var(--font-md);
  line-height: 1.6;
  color: var(--color-text);
  background: var(--color-bg);
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

#app {
  min-height: 100vh;
}

h1,
h2,
h3,
h4 {
  margin: 0;
  font-weight: 600;
  line-height: 1.35;
  color: var(--color-text);
}

p {
  margin: 0;
}

a {
  color: var(--color-primary);
  text-decoration: none;
  transition: color var(--transition-base);

  &:hover {
    color: var(--color-primary-dark);
  }
}

/* ---------- 4 个通用类 ---------- */

.page-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: var(--space-6) var(--space-5);
}

.app-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  padding: var(--space-5);
}

.section-title {
  margin-bottom: var(--space-4);
  font-size: var(--font-lg);
  font-weight: 600;
  color: var(--color-text);
}

.text-secondary {
  color: var(--color-text-secondary);
}
```

- [ ] **Step 6: 创建 `vue/src/styles/index.scss`**

```scss
// 样式层统一入口。@use 顺序即 CSS 输出顺序，不要调整。
@use './tokens';          // 必须最先：:root 令牌
@use './element-theme';   // 必须其次：覆盖 Element 变量
@use './base';            // 最后：重置与通用类
```

- [ ] **Step 7: 修改 `vue/src/main.js` 的样式引入**

把第 5 行的 `import './style.css'` 替换为：

```js
import './styles/index.scss'
```

替换后 `main.js` 的前 10 行应为：

```js
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import './styles/index.scss'
import App from './App.vue'
import router from './router'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
```

`element-plus/dist/index.css` 必须仍在 `./styles/index.scss` 之前——顺序反了 `--el-*` 覆盖就会失效。

- [ ] **Step 8: 删除 `vue/src/style.css`**

删除整个文件（它是 Vite 脚手架残留，`.hero`/`.counter`/`#next-steps`/`.ticks` 全站无引用）。

- [ ] **Step 9: 构建验证（绿）**

Run（**必须带 `danger-full-access`**）：`npm run build --prefix vue`
Expected: `✓ built in 1.42s` 量级的成功输出，**没有 SCSS 编译错误**。若报 `Undefined variable` 或 `Can't find stylesheet to import`，检查 `@use` 路径与 `$c-*` 变量名拼写。

- [ ] **Step 10: 验证 `--el-*` 覆盖真的生效（不是"我以为生效"）**

Run:
```powershell
Get-ChildItem vue\dist\assets\*.css | ForEach-Object { "FILE: $($_.Name)"; (Select-String -Path $_.FullName -Pattern '--el-color-primary:[^;}]*' -AllMatches).Matches.Value | Select-Object -Unique }
Write-Output "--- index.html 中的 CSS 加载顺序 ---"
Select-String -Path vue\dist\index.html -Pattern '<link[^>]*stylesheet[^>]*>' | ForEach-Object { $_.Matches.Value }
```
Expected: 出现 `--el-color-primary:#40776b`（小写是 minifier 的正常输出）；且 Element 的 CSS 出现在我们之前（若两者在不同文件，`index.html` 中我们的 `<link>` 必须在 Element 的之后）

- [ ] **Step 11: 启动 dev server（常驻后台）**

Run（background job，**必须带 `danger-full-access`**）：`npm run dev --prefix vue`
Expected: 输出 `Local: http://localhost:5173/`。**保持该任务常驻**，后续任务的样式改动由 HMR 自动刷新

- [ ] **Step 12: 用户目视确认**

请用户在 `http://localhost:5173/` 逐页刷新确认：全局配色已变柔和、按钮/输入框圆角变大、后台不再是"原生 Element 观感"。**若用户此时报告某页错位**，记录页面路径与现象，在该页对应的后续任务中优先处理；不要回到本任务改全局令牌。

- [ ] **Step 13: 提交**

```bash
git add vue/src/styles vue/src/main.js
git rm vue/src/style.css
git commit -m "feat(style): 建立设计令牌层与 Element Plus 主题覆盖，移除 Vite 残留样式"
```

---

### Task 2: 布局层 —— 前台导航与登录/注册页

**Files:**
- Modify: `vue/src/components/FrontendLayout.vue`（全部，139 行）
- Modify: `vue/src/components/AuthLayout.vue`（全部，73 行）
- Modify: `vue/src/views/register.vue`（样式段 135-167 行）

**Interfaces:**
- Consumes: Task 1 的全部令牌
- Produces: 前台导航的选中态 class 约定 —— 依赖 `router-link-active`（Vue Router 自动添加），后续任务不要再自造选中态

- [ ] **Step 1: 审计目标文件（红）**

Run: `AUDIT`
Expected: `components\FrontendLayout.vue` = 7，`components\AuthLayout.vue` = 11，`views\register.vue` = 1

- [ ] **Step 2: 改 `FrontendLayout.vue` 的 template**

改动点：① 加一层 `.navbar-inner` 承载最大宽度（背景条仍全宽）；② "首页"链接由 `/` 改为 `/home`，否则当前路径为 `/home` 时拿不到 `router-link-active` 高亮；③ "退出登录"给次级样式；④ 品牌 logo 尺寸用令牌尺度。

```html
<template>
  <div class="frontend-layout">
    <div class="navbar-container">
      <div class="navbar-inner">
        <div class="brand-section">
          <el-image class="brand-logo" :src="iconUrl" alt="品牌logo" />
          <h1 class="brand-name">心理健康AI助手</h1>
        </div>
        <div class="nav-section">
          <router-link to="/home" class="nav-link">首页</router-link>
          <router-link to="/consultation" class="nav-link" v-if="userStore.isLoggedIn"
            >AI咨询</router-link
          >
          <router-link to="/emotionDiary" class="nav-link" v-if="userStore.isLoggedIn"
            >情绪日记</router-link
          >
          <router-link to="/knowledge" class="nav-link">知识库</router-link>
          <el-button v-if="userStore.isLoggedIn" class="logout-btn" @click="handleLogout"
            >退出登录</el-button
          >
          <template v-else>
            <router-link to="/auth/login" class="nav-link">登录</router-link>
            <router-link to="/auth/register" class="nav-link nav-link--cta">
              <el-button type="primary">注册</el-button>
            </router-link>
          </template>
        </div>
      </div>
    </div>
    <div class="main-content">
      <router-view></router-view>
    </div>
    <div class="footer-container">
      <div class="footer-bottom">
        <p>&copy; 2026 心理健康AI助手 All rights reserved.</p>
      </div>
    </div>
  </div>
</template>
```

`<script setup>` 部分**一行不改**（`iconUrl`、`userStore`、`handleLogout` 保持原样）。

- [ ] **Step 3: 替换 `FrontendLayout.vue` 的整个 `<style>`**

```scss
<style lang="scss" scoped>
@use '../styles/mixins' as m;

.frontend-layout {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  width: 100%;
  background-color: var(--color-bg);

  .navbar-container {
    background: var(--alpha-90);
    border-bottom: 1px solid var(--color-border);
    backdrop-filter: blur(8px);
  }

  .navbar-inner {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: var(--space-5);
    max-width: 1200px;
    margin: 0 auto;
    padding: var(--space-3) var(--space-5);
  }

  .brand-section {
    display: flex;
    align-items: center;
    gap: var(--space-3);

    .brand-logo {
      width: 40px;
      height: 40px;
    }

    .brand-name {
      font-size: var(--font-lg);
      font-weight: 600;
      color: var(--color-text);
    }
  }

  .nav-section {
    display: flex;
    align-items: center;
    gap: var(--space-2);
    flex-wrap: wrap;

    .nav-link {
      padding: var(--space-2) var(--space-3);
      border-radius: var(--radius-pill);
      font-size: var(--font-sm);
      font-weight: 500;
      color: var(--color-text-secondary);
      transition: color var(--transition-base), background-color var(--transition-base);

      &:hover {
        color: var(--color-primary);
        background: var(--color-primary-wash);
      }

      /* 选中态：解决"用户不知道自己在哪一页" */
      &.router-link-active {
        color: var(--color-primary);
        background: var(--color-primary-light);
      }
    }

    .nav-link--cta {
      padding: 0;
      background: none;

      &:hover {
        background: none;
      }
    }

    .logout-btn {
      margin-left: var(--space-2);
    }
  }

  .main-content {
    flex: 1;
    display: flex;
    flex-direction: column;
    width: 100%;
  }

  .footer-container {
    margin-top: auto;
    padding: var(--space-5) 0;
    background: var(--color-surface);
    border-top: 1px solid var(--color-border);

    .footer-bottom {
      max-width: 1200px;
      margin: 0 auto;
      padding: 0 var(--space-5);
      text-align: center;
      font-size: var(--font-sm);
      color: var(--color-text-secondary);
    }
  }

  @include m.below-sm {
    .navbar-inner {
      flex-direction: column;
      align-items: flex-start;
      padding: var(--space-3) var(--space-4);
    }

    .brand-section .brand-name {
      font-size: var(--font-md);
    }

    .nav-section {
      width: 100%;
      justify-content: flex-start;
      gap: var(--space-1);
    }
  }
}
</style>
```

`backdrop-filter` 让导航成为玻璃条；`.main-content` 改为 flex 列容器，为 Task 4 首页的 `flex: 1` 自适应铺路（替代原来的 `calc(100vh - 215px)` 魔法数字）。

- [ ] **Step 4: 替换 `AuthLayout.vue` 的整个 `<style>`**

`<template>` 与 `<script setup>` 都**不改**，只改样式。

```scss
<style lang="scss" scoped>
@use '../styles/mixins' as m;

.auth-layout {
  display: flex;
  min-height: 100vh;

  .left-section {
    position: relative;
    display: flex;
    flex: 1;
    align-items: center;
    justify-content: center;
    overflow: hidden;
    background: linear-gradient(135deg, var(--color-primary-soft) 0%, var(--color-primary-dark) 100%);

    /* 一层极淡径向光斑，营造"安静"的质感 */
    &::before {
      content: '';
      position: absolute;
      top: -20%;
      left: -10%;
      width: 70%;
      height: 70%;
      border-radius: var(--radius-pill);
      background: radial-gradient(circle, var(--alpha-05) 0%, transparent 70%);
      pointer-events: none;
    }

    .content {
      position: relative;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: var(--space-6);
      text-align: center;
    }

    .title {
      margin-bottom: var(--space-5);
      font-size: var(--font-2xl);
      font-weight: 700;
      color: var(--color-text-inverse);
    }

    .text {
      max-width: 460px;
      margin-bottom: var(--space-6);
      /* 24px 纯白：进入大字号档（门槛 3:1），因为 --alpha-90 白字在柔和渐变上仅 3.58:1，不满足正文的 4.5:1。
         实测：纯白在整条渐变上最低 3.119:1，文字框内最低 3.399:1（Task 2 复审复算）。 */
      font-size: var(--font-xl);
      color: var(--color-text-inverse);
    }

    .robot {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 160px;
      height: 160px;
      border: 1px solid var(--alpha-20);
      border-radius: var(--radius-pill);
      background: linear-gradient(135deg, var(--alpha-15) 0%, var(--alpha-05) 100%);
      box-shadow: var(--shadow-lg), inset 0 1px 0 var(--alpha-30);
    }
  }

  .right-section {
    display: flex;
    flex: 1;
    align-items: center;
    justify-content: center;
    min-height: 100vh;
    background-color: var(--color-surface);
  }

  @include m.below-md {
    .left-section {
      display: none; /* 窄屏只保留表单，避免表单被挤压 */
    }

    .right-section {
      padding: var(--space-5);
    }
  }
}
</style>
```

- [ ] **Step 5: 改 `register.vue` 的样式（该文件的唯一色值）**

`<script setup>` 与 `<template>` 都**不改**。只把样式段里这两行：

```scss
      p {
        font-size: 18px;
        color: #6b7280;
      }
```

改为：

```scss
      p {
        font-size: var(--font-lg);
        color: var(--color-text-secondary);
      }
```

同文件内的 `h2 { font-size: 36px }` 顺带改为 `font-size: var(--font-3xl)`（36px → 44px，与登录页 `h2` 的层级一致），`.btn` 的 `margin-top: 40px` 改为 `margin-top: var(--space-6)`，`.form-container` 的 `margin-top: 30px` 改为 `margin-top: var(--space-6)`，`.footer` 的 `padding: 30px` 改为 `padding: var(--space-6)`。

- [ ] **Step 6: 审计确认归零（绿）**

Run: `AUDIT`
Expected: `FrontendLayout.vue`、`AuthLayout.vue`、`register.vue` **都不再出现在列表中**；`TOTAL` 由 297 降到 **278**

（Task 1 删除了 `style.css` 的 22 处，因此这里的基线是 297 而非最初的 319。）

- [ ] **Step 7: 构建验证**

Run: `npm run build --prefix vue`
Expected: 编译成功，无 SCSS 报错

- [ ] **Step 8: 用户目视确认**

请用户刷新 `http://localhost:5173/home`、`/auth/login`、`/auth/register`：导航变玻璃条且当前页有绿色药丸高亮；页脚由深灰大块变为浅色；登录/注册页左栏为柔和主色渐变；两个表单页的标题与间距一致。

- [ ] **Step 9: 提交**

```bash
git add vue/src/components/FrontendLayout.vue vue/src/components/AuthLayout.vue vue/src/views/register.vue
git commit -m "feat(style): 前台导航与登录/注册页改用设计令牌，补导航选中态"
```

---

### Task 3: 布局层 —— 后台外壳

**Files:**
- Modify: `vue/src/components/BackendLayout.vue`（样式段 22-54 行）
- Modify: `vue/src/components/Sidebar.vue`（样式段 35-111 行）
- Modify: `vue/src/components/Navbar.vue`（样式段 60-85 行，另改 template 一处内联 style）

**Interfaces:**
- Consumes: Task 1 令牌
- Produces: 后台内容区容器 `.content-container` 的 `.app-card` 用法约定，Task 9/10/12/13 的后台页面依赖它提供白卡底色

- [ ] **Step 1: 审计目标文件（红）**

Run: `AUDIT`
Expected: `BackendLayout.vue` = 1，`Sidebar.vue` = 4，`Navbar.vue` = 3

- [ ] **Step 2: 改 `BackendLayout.vue` 的 `<style>`**

```scss
<style lang="scss" scoped>
.backend-layout {
  height: 100vh;
  width: 100%;
}

.outer-container {
  height: 100%;
  flex: 1;
  overflow: hidden;
}

.inner-container {
  height: 100%;
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.main-content {
  flex: 1;
  overflow-y: auto;
  background: var(--color-bg);
  padding: var(--space-4);
}

.content-container {
  padding: var(--space-5);
  background: var(--color-surface);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  min-height: calc(100% - 7.4vh);
}
</style>
```

- [ ] **Step 3: 改 `Navbar.vue`**

template 中把 `style="margin-right: 50px;"` 这个硬编码移除，改为 class：

```html
        <div class="flex-box user-area">
```

样式段整体替换：

```scss
<style lang="scss" scoped>
.navbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 55px;
  width: 100%;
  padding: 0 var(--space-5);
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border);
  box-shadow: var(--shadow-sm);

  .flex-box {
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .page-title {
    margin-left: var(--space-4);
    font-size: var(--font-xl);
    font-weight: 600;
    color: var(--color-text);
  }

  .user-area {
    gap: var(--space-2);
    padding-right: var(--space-4);
  }
}
</style>
```

页面标题由 `26px` 降到 `var(--font-xl)` = 24px，修复"标题比内容标题还大"的层级倒挂。`26 → 24` 是有意为之：`--font-xl` 是令牌阶梯里最接近的一档。

- [ ] **Step 4: 改 `Sidebar.vue` 的 `<style>`**

```scss
<style lang="scss" scoped>
@use '../styles/mixins' as m;

.sidebar-wrapper {
  width: 200px;
  flex-shrink: 0;
  overflow: hidden;
  transition: width 0.3s ease;

  @include m.above-md {
    width: 220px;
  }

  &.collapsed {
    width: 64px;
  }

  .sidebar-menu {
    height: 100%;
    border-right: none;

    .brand {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: var(--space-2);
      height: 56px;
      padding: var(--space-3) var(--space-2);
      box-sizing: border-box;
      border-bottom: 1px solid var(--color-border);

      .logo-img {
        width: 28px;
        height: 28px;
        flex-shrink: 0;
      }

      .info-card {
        display: flex;
        flex-direction: column;
        align-items: flex-start;
        justify-content: flex-start;
        min-width: 0;
        overflow: hidden;
      }

      .info-card h1 {
        margin: 0;
        font-size: var(--font-sm);
        font-weight: 600;
        line-height: 1.5;
        white-space: nowrap;
        color: var(--color-text);
      }

      .info-card p {
        margin: var(--space-1) 0 0;
        font-size: var(--font-xs);
        line-height: 1.3;
        white-space: nowrap;
        color: var(--color-text-secondary);
      }
    }
  }

  /* 菜单项：圆角 + 左右内缩，选中态用主色浅底 + 左侧主色竖条 */
  :deep(.el-menu-item) {
    height: 44px;
    line-height: 44px;
    margin: 0 var(--space-2);
    border-radius: var(--radius-md);

    &:hover {
      color: var(--color-primary);
      background-color: var(--color-primary-wash);
    }

    &.is-active {
      color: var(--color-primary);
      background-color: var(--color-primary-light);
      font-weight: 600;
      position: relative;

      &::before {
        content: '';
        position: absolute;
        left: 0;
        top: 50%;
        width: 3px;
        height: 20px;
        transform: translateY(-50%);
        border-radius: var(--radius-pill);
        background: var(--color-primary);
      }
    }
  }
}
</style>
```

本步**同时去掉原有的 `background-color: #e6ecf7 !important;`**（蓝调 + `!important`），替换为上面的 `&.is-active`。`!important` 总数由 6 降到 5。

- [ ] **Step 5: 审计确认归零（绿）**

Run: `AUDIT`
Expected: 三个文件都不再出现；`TOTAL` = **270**

- [ ] **Step 6: 确认 `!important` 计数**

Run:
```powershell
(Get-ChildItem -Recurse vue\src -Include *.vue | Where-Object { $_.FullName -notmatch 'node_modules' } | Select-String -Pattern '!important' -AllMatches).Matches.Count
```
Expected: **5**（原 6，`Sidebar.vue:103` 已去除；剩余在 `consultation.vue` 2、`dashboard.vue` 2、`emotions.vue` 1，由 Task 7/13/12 处理）

- [ ] **Step 7: 构建 + 目视确认**

Run: `npm run build --prefix vue`
Expected: 成功。随后请用户登录后台（`/back/dashboard`）确认：侧边栏选中项为浅绿底 + 左侧绿竖条，内容区为圆角白卡

- [ ] **Step 8: 提交**

```bash
git add vue/src/components/BackendLayout.vue vue/src/components/Sidebar.vue vue/src/components/Navbar.vue
git commit -m "feat(style): 后台外壳改用设计令牌，修复标题层级倒挂并移除 !important"
```

---

### Task 4: 前台首页（含信任点区块）

**Files:**
- Modify: `vue/src/views/home.vue`（全部，103 行）

**Interfaces:**
- Consumes: Task 1 令牌；Task 2 已把 `.main-content` 改为 flex 列容器（本任务的 `.home-container { flex: 1 }` 依赖它）
- Produces: 无

- [ ] **Step 1: 审计目标文件（红）**

Run: `AUDIT`
Expected: `views\home.vue` = 10

- [ ] **Step 2: 替换整个 `<template>`**（新增信任点区块；修掉失效的按钮写法）

```html
<template>
  <div class="home-container">
    <div class="content">
      <div class="text">
        <h2 class="title">
          一次温暖的对话<br />
          <span class="highlight-text">化孤独为慰籍</span>
        </h2>
        <p class="description">
          每个深夜，每个焦虑的时刻，我们都在这里，不必独自承受，让心与心的连接温暖您的每一天
        </p>
        <div class="hero-actions">
          <el-button type="primary" size="large" @click="$router.push('/consultation')"
            >开始倾诉，获得陪伴</el-button
          >
          <el-button
            class="ghost-btn"
            size="large"
            plain
            @click="$router.push('/emotionDiary')"
            >记录心情，释放情感</el-button
          >
        </div>
      </div>
      <div class="robot">
        <el-image class="robot-img" :src="iconUrl" alt="机器人" />
      </div>
    </div>

    <div class="trust-points">
      <div class="trust-item" v-for="item in trustPoints" :key="item.title">
        <span class="trust-icon">
          <el-icon><component :is="item.icon" /></el-icon>
        </span>
        <h3 class="trust-title">{{ item.title }}</h3>
        <p class="trust-desc">{{ item.desc }}</p>
      </div>
    </div>
  </div>
</template>
```

**两处关键修复：**
1. 原第二个按钮写的是 `color="transparent"` + `style="border-color: #fff"`。Element Plus 的 `color` 属性只接受真实颜色值，`"transparent"` 是失效写法；改用 `plain` 幽灵按钮，视觉意图（白描边透明底）才真正实现。
2. 原来的 `min-height: calc(100vh - 215px)` 把导航与页脚高度写死，导航一改就错位；改为 `flex: 1` 由 Task 2 的 flex 列容器自适应。

`trustPoints` 的图标用**全局已注册**的 Element Plus 图标组件（`main.js` 中 `ElementPlusIconsVue` 全量注册，懒加载视图在挂载后渲染因此可用），**无需在 `<script>` 里新增 import**。

- [ ] **Step 3: 在 `<script setup>` 中新增 `trustPoints` 常量**

在现有 `const iconUrl = ...` 之后追加（这是本任务唯一的 script 改动，属于 Task 批准的"首页新增信任点内容"，不含业务逻辑）：

```js
const trustPoints = [
  {
    icon: 'Lock',
    title: '隐私保护',
    desc: '对话内容加密存储，只有你可以看到'
  },
  {
    icon: 'Clock',
    title: '随时可用',
    desc: '深夜、通勤、焦虑来袭，随时打开就能说'
  },
  {
    icon: 'Reading',
    title: '专业内容',
    desc: '由心理学知识库支撑的回应与自助内容'
  }
]
```

- [ ] **Step 4: 替换整个 `<style>`**

```scss
<style lang="scss" scoped>
@use '../styles/mixins' as m;

.home-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--space-8);
  padding: var(--space-8) var(--space-5);
  background: linear-gradient(135deg, var(--color-primary-soft) 0%, var(--color-primary-dark) 100%);
  color: var(--color-text-inverse);

  .content {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: var(--space-6);
    flex-wrap: wrap;
    width: 100%;
    max-width: 1200px;

    .text {
      flex: 1;
      min-width: 300px;
      max-width: 520px;

      .title {
        margin-bottom: var(--space-4);
        font-size: var(--font-3xl);
        font-weight: 700;
        color: var(--color-text-inverse);

        .highlight-text {
          /* 44px 属大字号档（≥3:1）。--color-accent-light 在渐变最亮处只有 2.78:1，
             只在其实际所在的中段位置达标。实施时必须实测该元素的实际背景对比度：
             若不足 3:1，改用 var(--color-text-inverse)（全渐变最低 3.119:1）并保留 font-weight: 700。
             注意：本注释不得写出该令牌的十六进制值——AUDIT 的正则会把它计入色值字面量。 */
          color: var(--color-accent-light);
        }
      }

      .description {
        /* 24px 纯白：进入大字号档（门槛 3:1）。18px + --alpha-90 在柔和渐变上仅 3.58:1，不满足正文的 4.5:1。
           纯白在整条渐变上最低 3.119:1（Task 2 复审实测），故 24px 档安全。 */
        font-size: var(--font-xl);
        line-height: 1.7;
        color: var(--color-text-inverse);
      }

      .hero-actions {
        display: flex;
        flex-wrap: wrap;
        gap: var(--space-3);
        margin-top: var(--space-6);
      }

      .ghost-btn {
        background: transparent;
        border-color: var(--alpha-60);
        color: var(--color-text-inverse);

        &:hover,
        &:focus {
          background: var(--alpha-10);
          border-color: var(--color-text-inverse);
          color: var(--color-text-inverse);
        }
      }
    }

    .robot {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 260px;
      height: 260px;
      border: 1px solid var(--alpha-20);
      border-radius: var(--radius-pill);
      background: linear-gradient(135deg, var(--alpha-15) 0%, var(--alpha-05) 100%);
      box-shadow: var(--shadow-lg), inset 0 1px 0 var(--alpha-30);
      flex-shrink: 0;

      .robot-img {
        width: 150px;
        height: 150px;
      }
    }
  }

  .trust-points {
    display: flex;
    justify-content: center;
    gap: var(--space-5);
    flex-wrap: wrap;
    width: 100%;
    max-width: 1200px;

    .trust-item {
      flex: 1 1 220px;
      max-width: 320px;
      padding: var(--space-5);
      /* 95% 白卡而非 10% 白玻璃：玻璃底上的浅色文字无法达到 4.5:1（14px 正文）。
         近白卡底让深色文字拿到 ≈11:1，且视觉上仍是"浮在渐变上的玻璃卡"。 */
      border: 1px solid var(--color-border);
      border-radius: var(--radius-lg);
      background: var(--alpha-95);

      .trust-icon {
        display: flex;
        align-items: center;
        justify-content: center;
        width: 40px;
        height: 40px;
        margin-bottom: var(--space-3);
        border-radius: var(--radius-md);
        background: var(--color-primary-light);
        font-size: var(--font-lg);
        color: var(--color-primary);
      }

      .trust-title {
        margin-bottom: var(--space-2);
        font-size: var(--font-md);
        font-weight: 600;
        color: var(--color-text);
      }

      .trust-desc {
        font-size: var(--font-sm);
        line-height: 1.7;
        color: var(--color-text-secondary);
      }
    }
  }

  @include m.below-sm {
    gap: var(--space-6);
    padding: var(--space-6) var(--space-4);

    .content {
      flex-direction: column-reverse;
      text-align: center;

      .text {
        .title {
          font-size: var(--font-2xl);
        }

        .hero-actions {
          justify-content: center;
        }
      }

      .robot {
        width: 180px;
        height: 180px;

        .robot-img {
          width: 104px;
          height: 104px;
        }
      }
    }
  }
}
</style>
```

金色高亮 `#ffd700` 改为 `var(--color-accent-light)`：暖橙浅阶在青绿底上比金色更协调，且不跳出浅色体系。

- [ ] **Step 5: 审计确认归零（绿）**

Run: `AUDIT`
Expected: `home.vue` 不再出现；`TOTAL` = **260**

- [ ] **Step 6: 验证内联 style 里的颜色已清零**

Run:
```powershell
(Get-ChildItem -Recurse vue\src -Include *.vue | Where-Object { $_.FullName -notmatch 'node_modules' } | Select-String -Pattern 'style="[^"]*(#[0-9a-fA-F]{3,8}|rgba?\(|\b(color|background)\s*:)').Count
```
Expected: **3**（原 4，`home.vue:14` 已清除；剩余 3 处全在 `articleDialog.vue`，由 Task 9 处理）

- [ ] **Step 7: 构建 + 目视确认**

Run: `npm run build --prefix vue`
Expected: 成功。请用户刷新首页确认：第二个按钮现在是白色描边幽灵按钮（**改前它是失效的**）、hero 下方出现 3 个信任点卡片、页面高度自适应无空白或错位

- [ ] **Step 8: 提交**

```bash
git add vue/src/views/home.vue
git commit -m "feat(style): 首页改用设计令牌，修复失效按钮写法与写死高度，新增信任点区块"
```

---

### Task 5: 前台知识库与文章详情

**Files:**
- Modify: `vue/src/views/frontendKnowledge.vue`（样式段）
- Modify: `vue/src/views/articleDetail.vue`（样式段）

**Interfaces:**
- Consumes: Task 1 令牌
- Produces: 卡片 hover 抬升约定 —— `transform: translateY(-2px)` + `var(--shadow-md)`，后续任务复用同一写法

- [ ] **Step 1: 审计目标文件（红）**

Run: `AUDIT`
Expected: `frontendKnowledge.vue` = 11，`articleDetail.vue` = 15

- [ ] **Step 2: 按值映射表替换 `frontendKnowledge.vue` 的色值**

| 原值 | 出现次数 | 替换为 | 说明 |
|---|---|---|---|
| `#fafbfc` `#f7f9fc` `#f2f6fa` | 各 1 | `var(--color-bg)` | 三色页面渐变 → 纯令牌底色（背景交给统一底色，卡片才浮得起来） |
| `#f59e0b` | 2 | `var(--color-accent)`（作底色/图标）、`var(--color-accent-text)`（作文字） | 原橙紫渐变的橙色一极 |
| `#8b5cf6` | 1 | `var(--color-primary-soft)` | 紫色一极收敛到主色系 |
| `#374151` | 1 | `var(--color-text)` | 标题 |
| `#6b7280` | 1 | `var(--color-text-secondary)` | 摘要 |
| `rgba(0,0,0,0.08)` | 2 | `var(--scrim-08)` | 阴影/描边 |
| `rgba(0,0,0,0.12)` | 1 | `var(--scrim-12)` | hover 阴影 |

标题渐变改为 `linear-gradient(135deg, var(--color-primary-soft) 0%, var(--color-primary-dark) 100%)`（原 `#f59e0b → #8b5cf6`），`border-left: 4px solid #f59e0b` 改为 `var(--color-accent)`。卡片圆角改 `var(--radius-lg)`，hover 加 `transform: translateY(-2px)` + `var(--shadow-md)` + `transition: var(--transition-base)`。

- [ ] **Step 3: 按值映射表替换 `articleDetail.vue` 的色值**

| 原值 | 出现次数 | 替换为 | 说明 |
|---|---|---|---|
| `#fafbfc` `#f7f9fc` `#f2f6fa` | 各 1 | `var(--color-bg)` | 页面渐变 → 令牌底色 |
| `#f59e0b` | 1 | `var(--color-accent)` | 原橙紫渐变 |
| `#8b5cf6` | 1 | `var(--color-primary-soft)` | 原橙紫渐变 |
| `#7ed321` | 1 | `var(--color-primary-soft)` | 草绿 → 主色系 |
| `rgba(126,211,33,0.1)` | 1 | `var(--color-primary-wash)` | 草绿浅底 → 主色浅底 |
| `#111827` | 2 | `var(--color-text)` | 文章标题 |
| `#374151` | 3 | `var(--color-text)`（正文）/ `var(--color-text-secondary)`（元信息） | 按语境取用 |
| `#e5e7eb` | 2 | `var(--color-border)` | 分隔线 |
| `rgba(0,0,0,0.05)` | 1 | `var(--scrim-04)` | 阴影 |

`border-left: 4px solid #7ed321` 改为 `var(--color-accent)`，与知识库列表的引用块样式保持一致。

- [ ] **Step 4: 审计确认归零（绿）**

Run: `AUDIT`
Expected: 两个文件都不再出现；`TOTAL` = **234**

- [ ] **Step 5: 构建 + 目视确认**

Run: `npm run build --prefix vue`
Expected: 成功。请用户刷新 `/knowledge` 与任一 `/knowledge/article/:id`：橙紫渐变消失、卡片圆角与 hover 抬升生效、正文行高更舒适

- [ ] **Step 6: 提交**

```bash
git add vue/src/views/frontendKnowledge.vue vue/src/views/articleDetail.vue
git commit -m "feat(style): 知识库与文章详情消除橙紫渐变配色，统一卡片与引用块令牌"
```

---

### Task 6: 前台情绪日记

**Files:**
- Modify: `vue/src/views/emotionDiary.vue`（样式段）

**Interfaces:**
- Consumes: Task 1 令牌；Task 5 的卡片 hover 约定
- Produces: 情绪选项选中态约定 —— 主色描边 + `var(--color-primary-light)` 底，Task 12 的情绪日志页沿用

- [ ] **Step 1: 审计目标文件（红）**

Run: `AUDIT`
Expected: `emotionDiary.vue` = 14

- [ ] **Step 2: 按值映射表替换色值**

| 原值 | 出现次数 | 替换为 | 说明 |
|---|---|---|---|
| `#fafbfc` `#f7f9fc` `#f2f6fa` | 各 1 | `var(--color-bg)` | 三色页面渐变 → 令牌底色 |
| `#7ed321` | 2 | `var(--color-primary)`（选中描边）/ `var(--color-primary-soft)`（标题渐变起点） | 草绿 → 主色 |
| `#f5a623` | 1 | `var(--color-accent)` | 标题渐变终点 |
| `#f0fdf4` | 1 | `var(--color-primary-light)` | 选中浅底 |
| `#374151` | 3 | `var(--color-text)` | 标题/正文 |
| `#6b7280` | 1 | `var(--color-text-secondary)` | 辅助说明 |
| `#e5e7eb` | 1 | `var(--color-border)` | 选项描边 |
| `#f9fafb` | 1 | `var(--color-border-light)` | 选项默认底 |
| `rgba(0,0,0,0.05)` | 1 | `var(--scrim-04)` | 阴影 |

标题渐变由 `#7ed321 → #f5a623` 改为 `linear-gradient(135deg, var(--color-primary-soft) 0%, var(--color-accent) 100%)`；选项选中态由 `border-color: #7ed321; background: #f0fdf4` 改为 `border-color: var(--color-primary); background: var(--color-primary-light)`；卡片圆角统一 `var(--radius-lg)`。

- [ ] **Step 3: 审计确认归零（绿）**

Run: `AUDIT`
Expected: `emotionDiary.vue` 不再出现；`TOTAL` = **220**

- [ ] **Step 4: 构建 + 目视确认**

Run: `npm run build --prefix vue`
Expected: 成功。请用户刷新 `/emotionDiary`：草绿到橙的标题渐变消失，情绪选项选中态为主色描边 + 浅绿底

- [ ] **Step 5: 提交**

```bash
git add vue/src/views/emotionDiary.vue
git commit -m "feat(style): 情绪日记改用设计令牌，收敛草绿到橙的标题渐变"
```

---

### Task 7: 前台 AI 咨询页（最大文件，消除暖纸色主题）

**Files:**
- Modify: `vue/src/views/consultation.vue`（样式段，约 490-1140 行）

**Interfaces:**
- Consumes: Task 1 令牌，特别是 `--alpha-*` / `--scrim-*` 透明层阶梯与 `--color-accent-*` / `--color-info-*` / `--color-success-*`
- Produces: 消息气泡配色约定 —— 用户气泡 `var(--color-primary)` 底 + `var(--color-text-inverse)` 字，AI 气泡 `var(--color-surface)` 底 + `var(--color-border)` 描边，Task 10 的咨询记录页复用

**这是全计划最大的单个任务（98 处色值字面量、63 个不同值）。不要试图一次改完再验证——按 Step 2 的四组逐组替换、逐组审计。**

- [ ] **Step 1: 审计目标文件（红）**

Run: `AUDIT`
Expected: `views\consultation.vue` = 98

- [ ] **Step 2: 按下面四组映射表逐组替换**

**第 1 组：暖纸色主题（本任务的核心目标 —— 消除 spec 第 ⑤ 套配色）**

| 原值 | 次数 | 替换为 |
|---|---|---|
| `#fef9e7` `#fcf4e6` `#f6f0e8` | 各 1 | `var(--color-bg)`（暖纸渐变背景 → 统一底色） |
| `rgba(252,244,230,0.8)` | 1 | `var(--color-accent-light)` |
| `rgba(255,252,248,0.95)` | 2 | `var(--alpha-95)` |
| `rgba(255,252,248,0.7)` | 1 | `var(--alpha-70)` |
| `rgba(255,252,248,0.05)` | 1 | `var(--alpha-05)` |
| `rgba(255,252,250,0.98)` | 1 | `var(--alpha-98)` |
| `#8b4513` | 1 | `var(--color-accent-text)` |
| `#8b7355` | 4 | `var(--color-text-secondary)` |
| `#6b5b47` | 2 | `var(--color-text-secondary)` |
| `#78716c` | 1 | `var(--color-text-secondary)` |
| `#d4840f` `#b8740c` | 各 1 | `var(--color-accent-text)` |
| `#ffeaa7` | 1 | `var(--color-accent-light)` |
| `#fff9e6` | 1 | `var(--color-accent-light)` |
| `#ffd700` | 1 | `var(--color-accent-text)` |
| `rgba(255,234,167,0.3)` `rgba(255,234,167,0.6)` | 各 1 | `var(--color-accent-wash)` / `var(--color-accent-wash-strong)` |

**第 2 组：橙渐变与粉色渐变（收敛到 accent 系）**

| 原值 | 次数 | 替换为 |
|---|---|---|
| `#fb923c` | 5 | `var(--color-accent)` |
| `#f59e0b` | 5 | `var(--color-accent-text)`（渐变终点） |
| `rgba(251,146,60,0.05)` `rgba(251,146,60,0.06)` `rgba(251,146,60,0.08)` `rgba(251,146,60,0.1)` | 1/1/2/3 | `var(--color-accent-wash)` |
| `rgba(251,146,60,0.25)` `rgba(251,146,60,0.3)` | 2/2 | `var(--color-accent-wash-strong)` |
| `#ff9a9e` | 2 | `var(--color-accent)` |
| `#fecfef` | 3 | `var(--color-accent-light)` |
| `rgba(255,154,158,0.4)` | 1 | `var(--color-accent-wash-strong)` |

> 说明：同族的 `0.05 / 0.06 / 0.08 / 0.10` 统一到 `--color-accent-wash`（α=0.10），`0.25 / 0.30` 统一到 `--color-accent-wash-strong`（α=0.24）。这是**有意的透明度归并**——把 6 档近似值收到 2 档令牌，视觉上不可辨；这类归并只允许发生在同一色相的透明度档位之间。

**第 3 组：蓝色系与绿色系（收敛到主色/信息/成功色）**

| 原值 | 次数 | 替换为 |
|---|---|---|
| `#4096ff` `#409eff` | 各 1 | `var(--color-primary)` |
| `rgba(64,150,255,0.3)` | 1 | `var(--color-info-wash-strong)` |
| `#e6f0ff` | 2 | `var(--color-primary-light)` |
| `#f8f9ff` | 1 | `var(--color-primary-wash)` |
| `#059669` | 2 | `var(--color-success)` |
| `rgba(5,150,105,0.4)` | 1 | `var(--color-success-wash-strong)` |
| `#e0e0e0` | 1 | `var(--color-border)` |
| `#ccc` | 1 | `var(--color-text-placeholder)` |
| `rgba(107,114,128,0.3)` | 1 | `var(--scrim-30)` |
| `#6b7280` `#4b5563` | 各 1 | `var(--color-text-secondary)` |

**第 4 组：中性色与白色透明层**

| 原值 | 次数 | 替换为 |
|---|---|---|
| `#333` | 2 | `var(--color-text)` |
| `#666` | 1 | `var(--color-text-secondary)` |
| `#999` | 4 | `var(--color-text-placeholder)` |
| `#fff` | 1 | `var(--color-text-inverse)`（在白底上的文字用 surface，按语境取用 `var(--color-surface)`） |
| `rgba(255,255,255,0.9)` | 3 | `var(--alpha-90)` |
| `rgba(255,255,255,0.95)` | 2 | `var(--alpha-95)` |
| `rgba(255,255,255,0.8)` | 2 | `var(--alpha-80)` |
| `rgba(255,255,255,0.7)` | 1 | `var(--alpha-70)` |
| `rgba(255,255,255,0.6)` | 1 | `var(--alpha-60)` |
| `rgba(255,255,255,0.5)` | 2 | `var(--alpha-50)` |
| `rgba(255,255,255,0.25)` | 1 | `var(--alpha-25)` |
| `rgba(255,255,255,0.2)` | 1 | `var(--alpha-20)` |
| `rgba(255,255,255,0.02)` | 1 | `var(--alpha-02)` |
| `rgba(0,0,0,0.1)` | 3 | `var(--scrim-10)` |
| `rgba(0,0,0,0.08)` | 1 | `var(--scrim-08)` |
| `rgba(0,0,0,0.06)` | 1 | `var(--scrim-06)` |
| `rgba(0,0,0,0.04)` | 2 | `var(--scrim-04)` |

**另外两处结构性改动：**
1. 用户/AI 气泡配色按上文的 **Produces 约定**统一：用户气泡底 `var(--color-primary)` + 字 `var(--color-text-inverse)`；AI 气泡底 `var(--color-surface)` + 描边 `var(--color-border)`。
2. 删除该文件两处 `!important`（原 1126、1127 行）。这两行是 `background: linear-gradient(...) !important`，在改成令牌渐变后不再需要强制覆盖；**若删除后渐变确实被 Element 默认值压住**，不要加回 `!important`，改用提高选择器特异性（例如 `.chat-panel .send-btn`），并在 Step 6 的提交说明中记录。

- [ ] **Step 3: 审计确认归零（绿）**

Run: `AUDIT`
Expected: `consultation.vue` 不再出现；`TOTAL` = **122**

- [ ] **Step 4: 确认 `!important` 已降到 3**

Run:
```powershell
(Get-ChildItem -Recurse vue\src -Include *.vue | Where-Object { $_.FullName -notmatch 'node_modules' } | Select-String -Pattern '!important' -AllMatches).Matches.Count
```
Expected: **3**（剩余 `dashboard.vue` 2、`emotions.vue` 1）

- [ ] **Step 5: 构建验证**

Run: `npm run build --prefix vue`
Expected: 成功，无 SCSS 报错

- [ ] **Step 6: 目视确认（重点页）**

请用户登录后刷新 `/consultation`，重点确认：暖纸色背景与气泡已消失、发送按钮为橙色主色渐变、情绪选择区不再是粉色、消息气泡左右区分清晰、输入区与滚动区无错位。**若删除 `!important` 导致某处样式被 Element 覆盖**，按 Step 2 第 2 条的方案处理。

- [ ] **Step 7: 提交**

```bash
git add vue/src/views/consultation.vue
git commit -m "feat(style): AI 咨询页消除暖纸色与粉橙配色，气泡改用主色令牌并移除 !important"
```

---

### Task 8: Markdown 正文渲染组件

**Files:**
- Modify: `vue/src/components/MarkdownRenderer.vue`（样式段 85-210 行）

**Interfaces:**
- Consumes: Task 1 令牌，特别是 `--color-code-bg` / `--color-code-text`
- Produces: 无

- [ ] **Step 1: 审计目标文件（红）**

Run: `AUDIT`
Expected: `MarkdownRenderer.vue` = 22

- [ ] **Step 2: 按值映射表替换（这是 GitHub 风格 → 令牌的映射）**

| 原值 | 次数 | 替换为 | 说明 |
|---|---|---|---|
| `#e5e7eb` | 2 | `var(--color-border)` | 标题下边框、分隔线 |
| `#374151` | 2 | `var(--color-text)` | 正文 |
| `#4b5563` | 1 | `var(--color-text-secondary)` | 次级正文 |
| `#6b7280` | 2 | `var(--color-text-secondary)` | 引用块文字、脚注 |
| `#d1d5db` | 1 | `var(--color-border)` | 引用块左边框 |
| `#f9fafb` | 2 | `var(--color-border-light)` | 引用块底 |
| `#f3f4f6` | 1 | `var(--color-border-light)` | 行内代码底 |
| `#3b82f6` | 3 | `var(--color-primary)` | 链接、hover 下划线 |
| `#eff6ff` | 1 | `var(--color-primary-light)` | 提示块底 |
| `#dbeafe` | 1 | `var(--color-primary-light)` | 标签底 |
| `#1e40af` | 4 | `var(--color-primary-dark)` | 标签文字、提示块文字 |
| `#e11d48` | 1 | `var(--color-danger)` | 删除线 |
| `#1f2937` | 1 | `var(--color-code-bg)` | 代码块底 |
| `#f9fafb`（代码块内文字） | — | `var(--color-code-text)` | 代码块文字 |

另外把正文 `line-height` 提到 `1.75`（阅读舒适度，spec 第 5.2 节要求），引用块左边框由 `4px` 改为 `3px` 并统一用 `var(--radius-sm)` 收角。

- [ ] **Step 3: 审计确认归零（绿）**

Run: `AUDIT`
Expected: `MarkdownRenderer.vue` 不再出现；`TOTAL` = **100**

- [ ] **Step 4: 构建 + 目视确认**

Run: `npm run build --prefix vue`
Expected: 成功。请用户打开任一文章详情页，确认标题边框、引用块、行内代码、代码块、链接、标签六种元素都跟随新配色，且正文行距更宽松

- [ ] **Step 5: 提交**

```bash
git add vue/src/components/MarkdownRenderer.vue
git commit -m "feat(style): 文章正文渲染由 GitHub 配色映射到设计令牌，提升正文行高"
```

---

### Task 9: 后台知识文章管理

**Files:**
- Modify: `vue/src/views/knowledge.vue`（样式段）
- Modify: `vue/src/components/articleDialog.vue`（样式段 + template 3 处内联 style）

**Interfaces:**
- Consumes: Task 1 令牌；Task 3 的 `.content-container` 白卡容器
- Produces: 无

**注意：`knowledge.vue` 有 0 处色值字面量**（它完全依赖 Element 默认外观），所以它的"红→绿"测试不适用于色值审计，验证方式是构建 + 目视。本任务的色值工作全在 `articleDialog.vue`（5 处）与 3 处模板内联 style。

- [ ] **Step 1: 审计目标文件（红）**

Run: `AUDIT`
Expected: `knowledge.vue` **不在列表**（0 处），`articleDialog.vue` = 5

- [ ] **Step 2: 把 `articleDialog.vue` 的 3 处内联 style 收进 scoped 样式**

把 template 中第 41、43、52 行的内联 `style="border: 1px solid #dcdfe6; ..."` 替换为 class：

```html
<div class="editor-frame">
    <Toolbar class="editor-toolbar" :editor="editorRef" :defaultConfig="toolbarConfig" mode="default" />
    <Editor class="editor-body" v-model="valueHtml" :defaultConfig="editorConfig" mode="default" @onCreated="handleCreated" />
</div>
```

（具体标签名以文件现有结构为准，**只把 `style="..."` 换成 `class="..."`，不改其他属性与事件绑定**。）随后在 `<style lang="scss" scoped>` 中补齐这三个 class：

```scss
.editor-frame {
  width: 100%;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  overflow: hidden;

  .editor-toolbar {
    border-bottom: 1px solid var(--color-border);
  }

  .editor-body {
    min-height: 100px;
    padding: var(--space-3) var(--space-5);
    overflow-y: auto;
  }
}
```

- [ ] **Step 3: 按值映射表替换 `articleDialog.vue` 剩余色值**

| 原值 | 次数 | 替换为 |
|---|---|---|
| `#dcdfe6` | 3 | `var(--color-border)` |
| `#f6f8fa` | 1 | `var(--color-border-light)` |
| `#8b949e` | 1 | `var(--color-text-secondary)` |

- [ ] **Step 4: `knowledge.vue` 的样式统一（无字面量，靠令牌与通用类）**

给页面根元素加 `.content-container` 已有的白卡之外，把页内卡片/工具条的自定义色值（如存在）改为令牌；表格圆角与斑马纹交给 Element 覆盖变量（Task 1 已设 `--el-border-radius-base` 与 `--el-fill-color-light`）。**本步只允许使用令牌与 4 个通用类，不得引入新的色值字面量。**

- [ ] **Step 5: 审计确认归零（绿）**

Run: `AUDIT`
Expected: `articleDialog.vue` 不再出现；`TOTAL` = **95**

- [ ] **Step 6: 验证内联 style 里的颜色已清零**

Run:
```powershell
(Get-ChildItem -Recurse vue\src -Include *.vue | Where-Object { $_.FullName -notmatch 'node_modules' } | Select-String -Pattern 'style="[^"]*(#[0-9a-fA-F]{3,8}|rgba?\(|\b(color|background)\s*:)').Count
```
Expected: **0**

- [ ] **Step 7: 构建 + 目视确认**

Run: `npm run build --prefix vue`
Expected: 成功。请用户进 `/back/knowledge`，确认表格圆角与配色跟随主题、打开"新增/编辑文章"弹窗确认富文本编辑器边框正常

- [ ] **Step 8: 提交**

```bash
git add vue/src/views/knowledge.vue vue/src/components/articleDialog.vue
git commit -m "feat(style): 后台文章管理改用设计令牌，内联样式收进 scoped 样式"
```

---

### Task 10: 后台咨询记录

**Files:**
- Modify: `vue/src/views/consultations.vue`（样式段 125-230 行）

**Interfaces:**
- Consumes: Task 7 的气泡配色约定（用户气泡主色底 / AI 气泡白底描边）
- Produces: 无

- [ ] **Step 1: 审计目标文件（红）**

Run: `AUDIT`
Expected: `views\consultations.vue` = 16

- [ ] **Step 2: 按值映射表替换**

| 原值 | 次数 | 替换为 | 说明 |
|---|---|---|---|
| `#f8f9fa` | 2 | `var(--color-border-light)` | 浅灰区块底 |
| `#e9ecef` | 3 | `var(--color-border)` | 边框与分隔 |
| `#e8f4fd` | 1 | `var(--color-primary-light)` | 用户侧气泡底色 |
| `#f0f9f0` | 1 | `var(--color-accent-light)` | 另一侧气泡底色（原为浅绿，改为暖橙浅底以与用户侧形成清晰区分） |
| `#495057` | 1 | `var(--color-text-secondary)` | 中等强调文字 |
| `#333` | 5 | `var(--color-text)` | 主文字 |
| `#666` | 1 | `var(--color-text-secondary)` | 次级文字 |
| `#999` | 1 | `var(--color-text-placeholder)` | 弱化文字 |
| `#fff` | 1 | `var(--color-surface)` | 卡片底 |

用户在左、AI 在右（或反之）的气泡区分色按 Task 7 约定：一侧 `var(--color-primary-light)` + `var(--color-text)`，另一侧 `var(--color-accent-light)` + `var(--color-text)`。**不要用彩色底 + 白字**——这两个气泡底色都很浅，白字会不可读。

- [ ] **Step 3: 审计确认归零（绿）**

Run: `AUDIT`
Expected: `consultations.vue` 不再出现；`TOTAL` = **79**

- [ ] **Step 4: 构建 + 目视确认**

Run: `npm run build --prefix vue`
Expected: 成功。请用户进 `/back/consultations`，确认记录列表与对话气泡配色统一、文字对比清晰

- [ ] **Step 5: 提交**

```bash
git add vue/src/views/consultations.vue
git commit -m "feat(style): 后台咨询记录改用设计令牌，气泡配色统一"
```

---

### Task 11: 图表色板层与一致性校验

**Files:**
- Create: `vue/src/styles/chart-palette.js`
- Create: `vue/scripts/check-token-parity.mjs`
- Modify: `vue/package.json`（scripts 增加 `check:tokens`）

**Interfaces:**
- Consumes: Task 1 的 `tokens.scss`（本文件是其镜像）
- Produces: 供 Task 12/13 使用的具名导出 —— `chartColors`（对象，键见下）与 `chartSeries`（6 色数组）。约定：`chartColors.<name>` 的键名与 `--color-<name>` 一一对应，即 `chartColors.primary` 对应 `--color-primary`

**为什么需要这个文件：** ECharts 的 `color` / `borderColor` 参数需要真实色值，无法传 `var(--token)`。因此设一个单一边界的色板模块作为令牌镜像，并用脚本机械校验一致性。`emotions.vue` 与 `dashboard.vue` 从它导入，不再各自写色值。

- [ ] **Step 1: 创建 `vue/src/styles/chart-palette.js`**

```js
// ECharts 色板 —— tokens.scss 的镜像
//
// 存在的唯一原因：ECharts 的 color / borderColor 需要真实色值，无法消费 CSS 变量。
// 一致性由 `npm run check:tokens`（vue/scripts/check-token-parity.mjs）机械校验：
// 本文件中的每个色值都必须能在 tokens.scss 中找到。
// 修改流程：先改 tokens.scss，再同步此文件，然后跑校验。
//
// 键名与 --color-<name> 一一对应。

export const chartColors = {
  primary: '#40776B',
  primarySoft: '#5B9E8F',
  primaryDark: '#2F5A51',
  primaryLight: '#E9F3F0',
  accent: '#C97B4A',
  accentText: '#A85F2E',
  accentLight: '#FBF0E8',
  success: '#3D7A5F',
  successLight: '#E7F2EC',
  warning: '#8A6220',
  warningLight: '#F7EFE0',
  danger: '#B85A52',
  dangerLight: '#F8E9E7',
  info: '#4A6E91',
  infoLight: '#E9F0F6',
  text: '#2C3A36',
  textSecondary: '#5F6F6A',
  textPlaceholder: '#9AA8A3',
  border: '#E6EDEA',
  borderLight: '#F0F4F2',
  surface: '#FFFFFF',
  bg: '#F7F9F8',
  primaryWashStrong: 'rgba(64, 119, 107, 0.24)',
  accentWashStrong: 'rgba(201, 123, 74, 0.24)',
  infoWashStrong: 'rgba(74, 110, 145, 0.30)',
  successWashStrong: 'rgba(61, 122, 95, 0.40)'
}

// 多系列图表的默认色序（柔和降饱和，与全站色板同源）
export const chartSeries = [
  chartColors.primarySoft,
  chartColors.accent,
  chartColors.info,
  chartColors.warning,
  chartColors.success,
  chartColors.danger
]
```

- [ ] **Step 2: 创建 `vue/scripts/check-token-parity.mjs`**

```js
// 校验 chart-palette.js 的每个色值都存在于 tokens.scss —— 防止令牌镜像漂移
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, resolve } from 'node:path'

const here = dirname(fileURLToPath(import.meta.url))
const tokensPath = resolve(here, '../src/styles/tokens.scss')
const palettePath = resolve(here, '../src/styles/chart-palette.js')

const normalize = (s) => s.toLowerCase().replace(/\s+/g, '')

const collect = (text) => {
  const hex = text.match(/#[0-9a-fA-F]{3,8}\b/g) || []
  const rgba = text.match(/rgba?\([^)]*\)/g) || []
  return new Set([...hex, ...rgba].map(normalize))
}

const tokens = collect(readFileSync(tokensPath, 'utf8'))
const palette = collect(readFileSync(palettePath, 'utf8'))

const missing = [...palette].filter((c) => !tokens.has(c))

if (missing.length > 0) {
  console.error('x chart-palette.js 中存在 tokens.scss 未定义的色值：')
  missing.forEach((c) => console.error('  ' + c))
  console.error('修复：先在 tokens.scss 定义该令牌，再回到 chart-palette.js 同步。')
  process.exit(1)
}

console.log(`ok 图表色板与令牌一致（${palette.size} 个色值全部来自 tokens.scss）`)
```

- [ ] **Step 3: 在 `vue/package.json` 注册校验脚本**

在 `scripts` 段加入一行（放在 `"preview"` 之后）：

```json
    "check:tokens": "node scripts/check-token-parity.mjs"
```

- [ ] **Step 4: 运行校验（绿）**

Run: `npm run check:tokens --prefix vue`
Expected: `ok 图表色板与令牌一致（26 个色值全部来自 tokens.scss）`

- [ ] **Step 5: 验证校验真的会失败（避免"假绿"）**

临时把 `chart-palette.js` 里 `primary: '#40776B'` 改成 `'#40776C'`，然后：

Run: `npm run check:tokens --prefix vue`
Expected: 退出码非 0，输出 `x chart-palette.js 中存在 tokens.scss 未定义的色值：` 与 `#40776c`

确认后**必须改回 `'#40776B'`**，并重新运行校验确认恢复绿色。

- [ ] **Step 6: 提交**

```bash
git add vue/src/styles/chart-palette.js vue/scripts/check-token-parity.mjs vue/package.json
git commit -m "feat(style): 新增图表色板令牌镜像与一致性校验脚本"
```

---

### Task 12: 后台情绪日志

**Files:**
- Modify: `vue/src/views/emotions.vue`（`<script>` 的 `getScoreColor` + 样式段 275-380 行）

**Interfaces:**
- Consumes: `chart-palette.js` 的 `chartColors`（Task 11）
- Produces: 无

- [ ] **Step 1: 审计目标文件（红）**

Run: `AUDIT`
Expected: `emotions.vue` = 18

- [ ] **Step 2: 替换 `<script>` 中 `getScoreColor()` 的 4 个色值**

**只替换颜色字面量，函数结构与调用点不动。** 在 `<script setup>` 顶部加入导入（这是本任务唯一的新增 import）：

```js
import { chartColors } from '@/styles/chart-palette'
```

然后把 `getScoreColor` 改为：

```js
const getScoreColor = (score) => {
  if (score >= 80) return chartColors.danger
  if (score >= 60) return chartColors.warning
  if (score >= 40) return chartColors.textPlaceholder
  return chartColors.success
}
```

分档语义与原实现完全一致（`>=80` 红、`>=60` 橙、`>=40` 灰、其余绿），只把 `#f56c6c` / `#e6a23c` / `#909399` / `#67c23a` 换成令牌。

- [ ] **Step 3: 按值映射表替换样式段色值**

| 原值 | 次数 | 替换为 | 说明 |
|---|---|---|---|
| `#303133` | 1 | `var(--color-text)` | 主文字 |
| `#606266` | 3 | `var(--color-text-secondary)` | 次级文字 |
| `#909399` | 4 | `var(--color-text-secondary)`（标签类）/ `var(--color-text-placeholder)`（弱化说明） | 按语境取用 |
| `#409eff` | 1 | `var(--color-primary)` | 链接/强调 |
| `#f8f9fa` | 1 | `var(--color-border-light)` | 浅底 |
| `#ebeef5` | 2 | `var(--color-border)` | 边框 |
| `#67c23a` | 2 | `var(--color-success)` | 成功态 |
| `#e1f3d8` | 1 | `var(--color-success-light)` | 成功浅底 |
| `#b3d8a4` | 1 | `var(--color-success)`（作描边，透明度由 `--el-color-success-light-3` 承担） | 成功描边 |
| `#e6a23c` | 1 | `var(--color-warning)` | 警告态 |
| `#f56c6c` | 1 | `var(--color-danger)` | 危险态 |

**重要**：上表这些值是把 Element 默认色**手抄**进 scoped 样式的产物。凡是"该元素本身就由 Element 组件渲染"的场景，应**直接删除这两行**让它继承覆盖后的 `--el-*` 变量，而不是换成 `var(--color-*)`。判断标准：规则作用在 `el-` 开头的组件上就删除，作用在自己写的 `div`/`span` 上就换令牌。

- [ ] **Step 4: 删除该文件的 `!important`（原 380 行）**

读取该行上下文，用提高选择器特异性替代；**不要保留 `!important`**。

- [ ] **Step 5: 审计确认归零（绿）**

Run: `AUDIT`
Expected: `emotions.vue` 不再出现；`TOTAL` = **61**

- [ ] **Step 6: 确认 `!important` 已降到 2**

Run:
```powershell
(Get-ChildItem -Recurse vue\src -Include *.vue | Where-Object { $_.FullName -notmatch 'node_modules' } | Select-String -Pattern '!important' -AllMatches).Matches.Count
```
Expected: **2**（仅剩 `dashboard.vue` 的 696、697 行，由 Task 13 处理）

- [ ] **Step 7: 运行令牌一致性校验**

Run: `npm run check:tokens --prefix vue`
Expected: `ok 图表色板与令牌一致`

- [ ] **Step 8: 构建 + 目视确认**

Run: `npm run build --prefix vue`
Expected: 成功。请用户进 `/back/emotions`，确认分值与图表配色跟随新色板（不再是手抄的 Element 默认色）

- [ ] **Step 9: 提交**

```bash
git add vue/src/views/emotions.vue
git commit -m "feat(style): 情绪日志改用设计令牌与图表色板，移除手抄的 Element 默认色"
```

---

### Task 13: 后台数据分析（ECharts 配色）

**Files:**
- Modify: `vue/src/views/dashboard.vue`（`<script>` 图表配色 + 样式段 640-745 行）

**Interfaces:**
- Consumes: `chart-palette.js` 的 `chartColors` 与 `chartSeries`（Task 11）
- Produces: 无

**警告：`dashboard.vue` 有用户未提交的改动。** 动手前先运行 `git diff vue/src/views/dashboard.vue` 看清哪些行是用户改的，**只改样式与颜色字面量，绝不覆盖用户改动**。若发现冲突，停下并向用户报告，不要自行取舍。

- [ ] **Step 1: 观察用户已有改动**

Run: `git diff vue/src/views/dashboard.vue`
Expected: 输出该文件已有的未提交改动内容。记下这些行号范围，后续编辑绕开它们

- [ ] **Step 2: 审计目标文件（红）**

Run: `AUDIT`
Expected: `views\dashboard.vue` = 61

- [ ] **Step 3: 替换 `<script>` 中的 ECharts 配色（约 33 处）**

在 `<script setup>` 顶部加入导入：

```js
import { chartColors, chartSeries } from '@/styles/chart-palette'
```

按下面的值映射表替换 `color` / `borderColor` / `label.color` / `axisLine.lineStyle.color` / `splitLine.lineStyle.color` / `areaStyle.color` 中的色值。**只改颜色字面量，图表配置项结构、数据绑定、`series` 的 `type`/`data` 一律不动。**

| 原值 | 替换为 |
|---|---|
| `#2d3436` | `chartColors.text` |
| `#636e72` | `chartColors.textSecondary` |
| `#7f8c8d` | `chartColors.textSecondary` |
| `#2c3e50` | `chartColors.text` |
| `#95a5a6` | `chartColors.textPlaceholder` |
| `#fab1a0` | `chartColors.accent` |
| `rgba(244,162,97,0.3)` | `chartColors.accentWashStrong` |
| `rgba(244,162,97,0.1)` | `chartColors.accentLight` |
| `#fdcb6e` | `chartColors.warning` |
| `#f39c12` | `chartColors.warning` |
| `#ffeaa7` | `chartColors.warningLight` |
| `#a29bfe` | `chartColors.info` |
| `rgba(162,155,254,0.1)` | `chartColors.infoLight` |
| `rgba(162,155,254,0.4)` | `chartColors.infoWashStrong` |
| `#00b894` | `chartColors.success` |
| `#74b9ff` | `chartColors.info` |
| `#0984e3` | `chartColors.primary` |
| `#667eea` `#764ba2` | `chartColors.primarySoft` / `chartColors.primaryDark` |
| `rgba(255,255,255,0.95)` | `chartColors.surface` |
| `#f093fb` `#f5576c` | `chartColors.accent` / `chartColors.danger` |
| `#4facfe` `#00f2fe` | `chartColors.info` / `chartColors.primarySoft` |
| `#43e97b` `#38f9d7` | `chartColors.success` / `chartColors.primarySoft` |

凡是原本手写多色数组的地方（如饼图 `color: [...]`、图表主题级 `color`），改为 `color: chartSeries`。

- [ ] **Step 4: 替换样式段色值（约 28 处）**

| 原值 | 替换为 |
|---|---|
| `#2c3e50` | `var(--color-text)` |
| `#7f8c8d` | `var(--color-text-secondary)` |
| `#95a5a6` | `var(--color-text-placeholder)` |
| `linear-gradient(135deg, #667eea 0%, #764ba2 100%)` 等 4 个指标卡渐变 | **改为浅底卡片**：底色分别 `var(--color-primary-light)` / `var(--color-accent-light)` / `var(--color-info-light)` / `var(--color-success-light)`，图标与数字用对应深阶色（`var(--color-primary)` / `var(--color-accent-text)` / `var(--color-info)` / `var(--color-success)`），圆角 `var(--radius-lg)`，阴影 `var(--shadow-sm)` |

4 个高饱和渐变卡改为浅底 + 深色图标，是"柔和治愈"方向的直接落点，也让 4 张卡不再互相抢视觉。此改动**不涉及新增令牌**（现有令牌已足够）。

- [ ] **Step 5: 删除该文件 2 处 `!important`（原 696、697 行）**

用提高选择器特异性替代。**若无法去除**，按 Global Constraints 的要求在此停止、保留原样，并在提交说明与最终报告中明确写"`!important` 未能清零，原因为 X"，不得静默保留。

- [ ] **Step 6: 审计确认归零（绿）**

Run: `AUDIT`
Expected: `dashboard.vue` 不再出现；**`TOTAL` = 0**

- [ ] **Step 7: 确认 `!important` 归零**

Run:
```powershell
(Get-ChildItem -Recurse vue\src -Include *.vue | Where-Object { $_.FullName -notmatch 'node_modules' } | Select-String -Pattern '!important' -AllMatches).Matches.Count
```
Expected: **0**

- [ ] **Step 8: 运行令牌一致性校验**

Run: `npm run check:tokens --prefix vue`
Expected: `ok 图表色板与令牌一致`

- [ ] **Step 9: 构建验证**

Run: `npm run build --prefix vue`
Expected: 成功，无 SCSS / JS 报错

- [ ] **Step 10: 目视确认**

请用户进 `/back/dashboard`，确认 4 个指标卡为浅底彩色图标样式、全部图表配色跟随柔和色板（不再有亮紫 `#667eea` 与亮蓝 `#0984e3`）、图表文字与轴线仍清晰可读

- [ ] **Step 11: 提交**

```bash
git add vue/src/views/dashboard.vue
git commit -m "feat(style): 后台数据分析图表配色接入令牌，指标卡改为浅底卡片"
```

---

### Task 14: 终结核查与完整回归

**Files:**
- 无文件改动（除非核查发现问题需要修复）

**Interfaces:**
- Consumes: 前 13 个任务的全部产物
- Produces: 最终验收报告

- [ ] **Step 1: 硬指标全量核查**

Run: `AUDIT`
Expected: **`TOTAL = 0`**，列表为空

- [ ] **Step 2: `!important` 归零核查**

Run:
```powershell
(Get-ChildItem -Recurse vue\src -Include *.vue | Where-Object { $_.FullName -notmatch 'node_modules' } | Select-String -Pattern '!important' -AllMatches).Matches.Count
```
Expected: **0**

- [ ] **Step 3: 含色值的内联 style 归零核查**

Run:
```powershell
(Get-ChildItem -Recurse vue\src -Include *.vue | Where-Object { $_.FullName -notmatch 'node_modules' } | Select-String -Pattern 'style="[^"]*(#[0-9a-fA-F]{3,8}|rgba?\(|\b(color|background)\s*:)').Count
```
Expected: **0**

- [ ] **Step 4: 确认 `.vue` 文件没有违规 `@use` tokens**

Run:
```powershell
Select-String -Path vue\src\**\*.vue -Pattern "@use\s+'[^']*tokens" | ForEach-Object { "$($_.Filename):$($_.LineNumber)" }
```
Expected: 无输出。若有输出，说明某个组件 `@use` 了 tokens.scss，会导致 `:root` 被 scope 化重复注入——改为直接使用 `var(--token)`，并只对断点保留 `@use '../styles/mixins'`

- [ ] **Step 5: 令牌一致性校验**

Run: `npm run check:tokens --prefix vue`
Expected: `ok 图表色板与令牌一致`

- [ ] **Step 6: 生产构建**

Run（**必须带 `danger-full-access`**）：`npm run build --prefix vue`
Expected: 成功。**警告与基线一致**（仍是那两条无关第三方警告）；若出现**新增**警告或任何错误，逐条查明来源

- [ ] **Step 7: 逐路由运行时回归（11 条）**

请用户在 `http://localhost:5173` 逐条访问并确认「布局不错位、文字不溢出、Element 组件外观正常、浏览器控制台无报错」：

`/home`、`/consultation`、`/emotionDiary`、`/knowledge`、`/knowledge/article/:id`、`/auth/login`、`/auth/register`、`/back/dashboard`、`/back/knowledge`、`/back/consultations`、`/back/emotions`

**若后端不可用**：登录与后台 4 页只能核对空态与静态样式。此时必须在最终报告中明确列出"未完成数据态验证"的页面，**不得声称全部验证通过**。

- [ ] **Step 8: 对比度抽查**

对实测中最容易出问题的 4 组组合复核（spec 第 3.5 节已列通过的配对表）：

Run:
```powershell
Write-Output "主按钮白字对比：#FFFFFF on #40776B = 5.16:1 (AA 通过)"
Write-Output "正文对比：#2C3A36 on #F7F9F8 = 11.25:1 (AA 通过)"
Write-Output "次级文字：#5F6F6A on #F7F9F8 = 5.00:1 (AA 通过)"
Write-Output "主色作文字：#40776B on #FFFFFF = 5.16:1 (AA 通过)"
```
Expected: 与 spec 第 3.5 节表格一致。若实施过程中引入过新的前景/背景配对，**必须按 WCAG 公式重新核算**，不达 4.5:1 的按 spec 第 3.5 节的规则调整。

- [ ] **Step 9: 记录最终结果**

向用户报告：硬指标数据（改前 319 → 改后 0）、`!important`（6 → 0）、内联色值（4 → 0）、11 条路由的验证状态、未能验证的部分及原因、以及 spec 第 11 节记录的 2 个范围外问题（文件名大小写、图标注册时序）仍未修复。

**不要在本任务中修复范围外问题**——它们需要单独分类与批准。

---

## 自查记录（Self-Review）

**1. Spec 覆盖检查**

| Spec 章节 | 对应任务 |
|---|---|
| 3.1–3.4 色板与令牌 | Task 1（tokens.scss 全量落地） |
| 3.5 对比度规则 | Task 14 Step 8 + 各任务映射表中的"按语境取用"依据 |
| 4.1 文件结构（6 文件） | Task 1（5 个）+ Task 11（chart-palette.js） |
| 4.2 引入顺序 | Task 1 Step 7 + Step 10 验证 |
| 4.3 style.css 删除及 3 处副作用 | Task 1 Step 8；副作用影响在 Task 2/4 的目视确认中暴露 |
| 4.4 Element 覆盖（1–9 全阶） | Task 1 Step 4 |
| 4.5 改造规则 1–8 | 规则 1–6 贯穿 Task 2–13；规则 7 → Task 11；规则 8 → Task 4 Step 6、Task 9 Step 2/6 |
| 4.6 通用类 | Task 1 Step 5，Task 9 Step 4 首次实际使用 |
| 5.1 布局层 3 个 | Task 2（Frontend/Auth）+ Task 3（Backend/Sidebar/Navbar） |
| 5.2 前台 5 页 | Task 4（home）、Task 5（knowledge/detail）、Task 6（diary）、Task 7（consultation）、Task 8（MarkdownRenderer） |
| 5.3 后台 4 页 | Task 9（knowledge/dialog）、Task 10（consultations）、Task 12（emotions）、Task 13（dashboard） |
| 6 决策清单 | 全部落在 Global Constraints 与各任务实现中 |
| 7.1 硬指标 | Task 1 Step 1 基线，Task 14 Step 1–3 终验 |
| 7.2 逐路由回归 | 各任务目视确认 + Task 14 Step 7 |
| 7.3 能力边界 | Task 14 Step 7 与 Step 9（不得声称未验证的通过） |
| 8 风险与回滚 | Global Constraints（不碰未提交改动）+ 每任务一次提交 |
| 9 不做的事 | Global Constraints |
| 11 范围外问题 | Task 14 Step 9（只报告不修复） |

无遗漏。

**2. 占位符扫描**：无 "TBD" / "类似 Task N" / "适当处理" 之类表述。Task 9 的 `knowledge.vue` 因 0 处色值字面量而"无红→绿测试"，已在该任务开头显式说明验证方式改为构建 + 目视，不是遗漏。

**3. 命名一致性**：全计划使用的令牌名与 `tokens.scss`、`chart-palette.js` 的定义逐一对齐；`chartColors` 的键名与 `--color-<name>` 一一对应；`AUDIT` 命令在所有任务中为同一段代码。

`TOTAL` 预期值按各文件实测字面量数逐一递减核算（每个文件的实测值见 spec 第 1 节表格）：

```
初始基线                     319   （.vue 297 + style.css 22）
Task 1  删除 style.css       297
Task 2  FrontendLayout  7
        AuthLayout      11
        register         1   → 278
Task 3  BackendLayout   1
        Sidebar         4
        Navbar          3   → 270
Task 4  home           10   → 260
Task 5  frontendKnowledge 11
        articleDetail     15 → 234
Task 6  emotionDiary   14   → 220
Task 7  consultation   98   → 122
Task 8  MarkdownRenderer 22 → 100
Task 9  articleDialog   5   →  95   （knowledge.vue 本身 0 处）
Task 10 consultations  16   →  79
Task 11 新增 chart-palette.js（白名单，不计数） → 79
Task 12 emotions       18   →  61
Task 13 dashboard      61   →   0
```

**4. 自查中实际抓到的问题（已修正）**

1. **`register.vue` 原本无任何任务覆盖**（它有 1 处 `#6b7280`），照原稿执行最终会停在 `TOTAL = 1` 而非 0。已并入 Task 2，TOTAL 链路同步重算。
2. **TOTAL 递减链原稿从 319 起算却漏掉 Task 1 删掉 style.css 的 22 处**，导致后续每个预期值都偏高 22。已全部重算。
3. **计划对 spec §4.1"4 个文件"做了两处必要补充**：`_mixins.scss`（否则每个 SFC 都要重复手写媒体查询，且 CSS 变量无法用于媒体查询）与 `chart-palette.js`（ECharts 无法消费 CSS 变量）。spec 已同步更新为 6 文件，两份文档一致。
4. **spec 初稿的硬指标口径有误**（"194 处"是含色值的行数，不是出现次数；`rgb()/rgba()` 归零不现实，因为透明层是合理需求）。已改为实测口径 319，并引入 `--alpha-*` / `--scrim-*` 透明层阶梯，使 rgba 字面量也能真正归零而不改变视觉。
5. **构建在受限沙箱下必然 `EPERM` 失败**，这是实测发现的环境约束。已在"环境前提"中明确要求构建与 dev server 命令带 `danger-full-access`，避免执行者按默认沙箱反复失败而误判为代码问题。

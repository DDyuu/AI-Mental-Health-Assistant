# AI 心理健康助手

一个基于 **Vue 3 + Spring Boot 3** 的全栈智能心理健康咨询平台，集成 DeepSeek AI 大模型，提供 AI 心理咨询、情绪分析、情绪日记、知识库等功能，帮助用户进行心理健康管理和情感疏导。

## 项目简介

本项目是一个现代化的心理健康助手应用，通过 AI 技术为用户提供专业的心理咨询服务。系统分为前台用户端和后台管理端，前台支持实时流式 AI 对话、情绪花园可视化、情绪日记记录、知识库文章浏览等功能；后台提供数据分析看板、知识文章管理、咨询记录查看、情绪日志管理等能力。

## AI 模型配置

- **API密钥**：在 `application.yml` 中配置 DeepSeek API 密钥，用于与 DeepSeek AI 模型进行交互。


## 技术栈

### 前端

- **核心框架**：Vue 3 (^3.5.34)
- **构建工具**：Vite (^8.0.12)
- **路由管理**：Vue Router (^4.6.4)
- **状态管理**：Pinia (^3.0.4)
- **UI 组件库**：Element Plus (^2.14.1)
- **图标库**：@element-plus/icons-vue (^2.3.2)
- **HTTP 客户端**：Axios (^1.13.4)
- **图表库**：ECharts (^6.1.0)
- **富文本编辑器**：wangEditor (^5.1.23)
- **流式请求**：@microsoft/fetch-event-source (^2.0.1)
- **CSS 预处理器**：Sass（scoped scss）

### 后端

- **框架**：Spring Boot 3.4.5
- **JDK**：Java 17
- **数据库**：MySQL
- **ORM**：MyBatis-Plus 3.5.9
- **鉴权**：JWT (jjwt 0.12.6)
- **AI 模型**：DeepSeek Chat API
- **构建工具**：Maven

## 功能特性

### 前台用户端

#### 首页
- 温暖的心理健康主题欢迎界面
- 机器人助手 IP 形象展示
- 咨询、情绪日记、知识库快速导航入口

#### AI 心理咨询
- **实时流式对话**：基于 SSE 的流式响应，逐字输出 AI 回复
- **智能情绪分析**：每次对话后自动分析情绪，识别主要情绪、强度评分、风险等级
- **情绪花园**：以可视化方式展示当前情绪状态
- **会话管理**：新建会话、历史会话检索与删除
- **治愈建议**：根据情绪状态推送改善建议和治愈小行动
- **风险预警**：高危情绪自动提示

#### 情绪日记
- 每日心情记录与评分（0-100）
- 生活指标追踪（睡眠质量、压力水平等）
- AI 自动分析日记内容，生成情绪评估与改善建议
- 历史日记时间线浏览

#### 知识库
- 心理健康文章分类浏览
- 文章详情阅读，Markdown 渲染
- 阅读量统计

### 后台管理端

#### 数据分析仪表盘
- **系统概览**：用户总数、会话总数、日记总数
- **情绪趋势图**：ECharts 折线图展示情绪变化趋势
- **咨询统计图**：ECharts 柱状图展示咨询数据
- **用户活跃度**：ECharts 图表展示用户活跃情况

#### 知识文章管理
- 文章发布与编辑（富文本编辑器 wangEditor）
- 文章分类管理
- 文章封面图片上传

#### 咨询记录管理
- 用户会话记录查看
- 对话消息详情浏览
- 数据检索与分页

#### 情感日志管理
- 用户情绪记录列表
- AI 分析结果查看
- 情绪趋势追踪
- 风险预警管理

### 用户认证

- 用户注册与登录
- JWT Token 鉴权
- 双角色权限控制（前台用户 `userType=1` / 后台管理员 `userType=2`）
- 路由守卫自动跳转

## 项目结构

```
ai-mental-health-assistant/
├── vue/                              # 前端项目
│   ├── public/
│   ├── src/
│   │   ├── api/                      # API 接口层
│   │   │   ├── admin.js              # 后台管理接口
│   │   │   └── frontend.js           # 前台用户接口
│   │   ├── assets/images/            # 图片资源
│   │   ├── components/               # 公共组件
│   │   │   ├── BackendLayout.vue     # 后台布局
│   │   │   ├── FrontendLayout.vue    # 前台布局
│   │   │   ├── AuthLayout.vue        # 认证布局
│   │   │   ├── Navbar.vue            # 导航栏
│   │   │   ├── Sidebar.vue           # 侧边栏
│   │   │   ├── PageHead.vue          # 页面头部
│   │   │   ├── MarkdownRenderer.vue  # Markdown 渲染器
│   │   │   ├── articleDialog.vue     # 文章编辑弹窗
│   │   │   ├── RichTextEditor.vue    # 富文本编辑器
│   │   │   └── TableSearch.vue       # 表格搜索组件
│   │   ├── config/index.js           # 配置文件
│   │   ├── router/index.js           # 路由配置
│   │   ├── stores/                   # Pinia 状态管理
│   │   │   ├── user.js               # 用户状态（个人信息、登录态）
│   │   │   └── admin.js              # 后台布局状态（侧边栏折叠）
│   │   ├── utils/request.js          # Axios 封装（拦截器、Token）
│   │   ├── views/                    # 页面组件
│   │   │   ├── home.vue              # 首页
│   │   │   ├── consultation.vue      # AI 咨询
│   │   │   ├── emotionDiary.vue      # 情绪日记
│   │   │   ├── frontendKnowledge.vue # 前台知识库
│   │   │   ├── articleDetail.vue     # 文章详情
│   │   │   ├── dashboard.vue         # 数据分析仪表盘
│   │   │   ├── knowledge.vue         # 知识文章管理
│   │   │   ├── consultations.vue     # 咨询记录管理
│   │   │   ├── emotions.vue          # 情绪日志管理
│   │   │   ├── login.vue             # 登录
│   │   │   └── register.vue          # 注册
│   │   ├── App.vue                   # 根组件
│   │   ├── main.js                   # 入口文件
│   │   └── style.css                 # 全局样式
│   ├── index.html
│   ├── vite.config.js                # Vite 配置
│   └── package.json
├── springboot/                       # 后端项目
│   ├── src/main/java/com/mentalhealth/assistant/
│   │   ├── common/                   # 通用响应（Result、ResultCode）
│   │   ├── config/                   # 配置（CORS、MyBatis-Plus、WebMvc）
│   │   ├── controller/               # 控制器
│   │   │   ├── UserController.java
│   │   │   ├── ConsultationController.java
│   │   │   ├── ArticleController.java
│   │   │   ├── CategoryController.java
│   │   │   ├── EmotionDiaryController.java
│   │   │   ├── EmotionGardenController.java
│   │   │   ├── DataAnalysisController.java
│   │   │   ├── FileUploadController.java
│   │   │   └── HealthController.java
│   │   ├── entity/                   # 数据实体
│   │   ├── mapper/                   # MyBatis-Plus Mapper
│   │   ├── service/                  # 业务逻辑层
│   │   │   ├── ai/                   # AI 服务
│   │   │   │   ├── DeepSeekService.java
│   │   │   │   ├── EmotionAnalysisService.java
│   │   │   │   └── PromptConfig.java
│   │   │   └── impl/                 # 服务实现
│   │   ├── util/JwtUtil.java         # JWT 工具
│   │   ├── vo/                       # 视图对象（分页 VO）
│   │   └── AssistantApplication.java # 启动类
│   ├── src/main/resources/
│   │   └── application.yml           # 配置文件
│   ├── uploads/                      # 文件上传目录
│   ├── pom.xml                       # Maven 依赖
│   └── mvnw
└── README.md
```

## 快速开始

### 环境要求

- **Node.js** >= 16.0.0
- **npm** >= 8.0.0
- **JDK** >= 17
- **Maven** >= 3.6
- **MySQL** >= 8.0

### 数据库初始化

创建数据库并执行初始化脚本：

```sql
CREATE DATABASE ai_mental_health_assistant
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;
```

数据库表结构由 MyBatis-Plus 自动维护，首次启动时将自动建表。

### 后端启动

1. 修改 `springboot/src/main/resources/application.yml` 中的数据库连接信息（用户名、密码）和 DeepSeek API Key。
2. 启动后端：

```bash
cd springboot
.\mvnw spring-boot:run
```

后端将在 `http://localhost:8080` 启动。

### 前端启动

1. 安装依赖：

```bash
cd vue
npm install
```

2. 启动开发服务器：

```bash
npm run dev
```

前端将在 `http://localhost:5173` 启动。

3. 生产构建：

```bash
npm run build
```

## 配置说明

### 前端代理配置

在 `vue/vite.config.js` 中配置了 API 代理，开发环境下将 `/api` 请求转发到后端：

```javascript
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true
    }
  }
}
```

### 后端配置

`springboot/src/main/resources/application.yml` 主要配置项：

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ai_mental_health_assistant?useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: root

deepseek:
  api-key: your-api-key
  api-url: https://api.deepseek.com/chat/completions
  model: deepseek-chat

mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```

### 路径别名

前端配置了 `@` 别名指向 `src` 目录：

```javascript
resolve: {
  alias: { '@': resolve(__dirname, 'src') }
}
```

## 核心功能说明

### AI 流式对话

前端使用原生 `fetch` + `ReadableStream` 实现 SSE 流式对话，逐字渲染 AI 回复内容：

```
客户端 POST /api/psychological-chat/session/chat
  -> 服务端返回 text/event-stream
  -> 每行以 data: 开头
  -> data:CONTENT 逐字推送
  -> data:[DONE] 结束标记
  -> data:SESSION:xxx 新会话 ID 标记
```

### 情绪分析

每次 AI 对话完成后，后端自动调用 DeepSeek API 分析会话情绪，分析内容包括：

- **主要情绪**：识别用户当前主导情绪（如焦虑、悲伤、平静、开心等）
- **情绪评分**：0-100 的量化评分
- **风险等级**：0（正常）~ 3（危机）
- **情绪性质**：正面 / 负面判断
- **改善建议**：个性化治愈小行动推荐
- **风险描述**：高危情绪的温馨提示

### 权限控制

基于用户类型的路由守卫控制：

- **前台用户**（userType=1）：可访问 `/`, `/home`, `/consultation`, `/emotionDiary`, `/knowledge`
- **后台管理员**（userType=2）：可访问 `/back/dashboard`, `/back/knowledge`, `/back/consultations`, `/back/emotions`

JWT Token 存储在 `localStorage`，每次 API 请求通过 Axios 拦截器自动携带。

### 状态管理

- **user store** (`stores/user.js`)：管理用户登录信息（userInfo）、登录态判断（isLoggedIn）、管理员判断（isAdmin），支持 localStorage 持久化恢复
- **admin store** (`stores/admin.js`)：管理后台侧边栏折叠状态

## 浏览器支持

- Chrome >= 90
- Firefox >= 88
- Safari >= 14
- Edge >= 90

## 开发建议

1. **前端开发**：遵循 Vue 3 Composition API + `<script setup>` 语法，使用 scoped SCSS 管理组件样式
2. **后端开发**：遵循三层架构（Controller -> Service -> Mapper），统一使用 `Result` 封装响应
3. **API 风格**：统一 `/api/` 前缀，使用统一的 Axios 实例（`utils/request.js`）发送请求
4. **状态管理**：用户全局状态使用 Pinia 管理，避免在组件间通过 props/emit 传递用户信息
5. **权限校验**：前端通过路由守卫控制页面访问，后端通过拦截器校验 JWT Token 和权限
6. **AI 集成**：DeepSeek API 相关配置集中在 `application.yml`，AI 服务封装在 `service/ai/` 包中

## 许可证

MIT License

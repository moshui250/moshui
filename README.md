# CodeAgent 智码刷题平台

> AI 原生的编程练习平台，面向编程新手。把“不会做”拆成下一步：判题告诉你对错，AI 调试告诉你先看哪里、下一步验证什么。

CodeAgent 不是“帮你写代码”的题库。它把一次失败拆成可执行的练习闭环：

```text
选题 → 写代码 → 运行样例 → 提交判题 → 失败
     → AI 定位与追问 → 学生修改 → 再提交 → 通过
     → 错题本 → 同类题推荐 → 下一次独立尝试
```

项目采用前后端分离的单仓库结构，后端为 Spring Boot 单体，判题由独立部署的 go-judge 执行，AI 能力基于 Spring AI + DeepSeek + Zilliz。

---

## 目录

- [项目简介](#项目简介)
- [功能亮点](#功能亮点)
- [技术栈](#技术栈)
- [系统架构](#系统架构)
- [目录结构](#目录结构)
- [快速开始](#快速开始)
- [环境变量](#环境变量)
- [判题服务](#判题服务)
- [AI 与 RAG](#ai-与-rag)
- [图片存储](#图片存储)
- [构建与测试](#构建与测试)
- [API 概览](#api-概览)
- [安全约定](#安全约定)
- [团队信息](#团队信息)

---

## 项目简介

新手学编程时，卡住的地方往往不是“完全不会写”，而是：

- 编译错误、运行时错误、WA 混在一起，不知道先看哪一行；
- 看懂了别人的完整题解，换一道题又回到原点；
- 提示太直接，练习变成复制代码，过程没有留下来。

CodeAgent 的回应是：**先定位，再追问，最后把练习还给学生。**

核心设计：

| 原则 | 做法 |
|---|---|
| 判题要确定 | 对错由 go-judge 沙箱执行，不由大模型判断 |
| 解释要克制 | AI 只给观察、追问和最小验证，不给完整可提交代码 |
| 提示要相关 | 绑定题目、代码、判题日志、知识点和错题记录 |
| 学习要留下 | 错题本、薄弱知识点、学习看板、同类题推荐 |
| 工程要可交付 | 单体优先，不引入 Redis / MQ / 微服务 |

当前演示库包含 35 道公共题、16 个知识点、60 条判题用例、3 场竞赛；后端 37 个 REST 接口，前端 34 个 Vue 组件。

---

## 功能亮点

- **判题与 AI 分离**：AC / WA / CE / RE / TLE / MLE 由沙箱确定，AI 只做错误定位与教学引导。
- **受控 Agent**：系统预执行 `locate_error` 与 `retrieve_knowledge`，模型负责校验、纠错和提问，不自由调用任意工具。
- **CREST-CoT 教学结构**：回答固定为“先对焦 → 拆开看 → 你来判断 → 最小验证”，一次只推进一步。
- **新手友好的 AI 帮助**：不展示框架术语，只告诉学生说清“卡在哪、你怎么想、试过什么、想让我怎么帮”。
- **RAG 知识库**：题解、知识点、错题手册写入 Zilliz；检索失败时降级关键词检索，不把失败包装成成功。
- **完整学习闭环**：错题本记录最近代码与错误类型，学习看板展示周趋势、薄弱知识点，推荐 3 道同类题。
- **图片录题**：上传题目截图，视觉模型识别为可编辑草稿，识别失败可手动补。
- **竞赛模式**：独立题单、报名、排名、倒计时；竞赛题访问受控，AI 调试禁用，提交不污染普通错题本。
- **安全边界**：JWT 认证、题目可见性、对象级权限、隐藏用例不下发、上传目录可写探针。
- **真数据首页**：首页统计、周趋势、知识点地图来自真实接口，不使用静态假数据。

---

## 技术栈

| 层次 | 技术 |
|---|---|
| 前端 | Vue 3.5、Vite 6、Vue Router 4、Pinia、Element Plus、Axios、Monaco Editor、ECharts、marked + DOMPurify |
| 后端 | Java 17、Spring Boot 3.5、Spring MVC、Validation、MyBatis、JWT（jjwt）、Caffeine |
| AI | Spring AI 1.0、DeepSeek Chat、Tool Calling、RAG、DashScope 视觉模型 |
| 数据 | MySQL 8、Zilliz / Milvus（向量）、本地文件存储 |
| 判题 | go-judge v1.14（Java 17 / g++，Docker 沙箱） |
| 构建 | Maven、npm、Docker Compose |

### 主要依赖

**后端**（`backend/codeagent/pom.xml`）

- `spring-boot-starter-web`、`spring-boot-starter-validation`
- `spring-ai-starter-model-openai`、`spring-ai-milvus-store`
- `mybatis-spring-boot-starter`、`mysql-connector-j`
- `jjwt-api` / `jjwt-impl` / `jjwt-jackson`
- `spring-security-crypto`（密码哈希）
- `caffeine`（短时缓存）、`thumbnailator`（图片处理）
- `lombok`、`spring-boot-starter-test`

**前端**（`frontend/package.json`）

- `vue`、`vue-router`、`pinia`、`axios`
- `element-plus`、`@element-plus/icons-vue`
- `monaco-editor`、`echarts`
- `marked`、`dompurify`、`highlight.js`

---

## 系统架构

```text
浏览器（Vue 3 + Monaco + ECharts）
        │  REST / SSE
        ▼
Spring Boot 单体
  ├─ JWT 拦截器 / 统一异常处理
  ├─ 题目、判题、错题、竞赛、看板服务
  ├─ 有界线程池 → go-judge（编译 / 运行隔离）
  ├─ Spring AI → DeepSeek（调试 / 讲解）
  └─ RAG → Zilliz（题目 / 题解 / 知识点）
        │
        ▼
MySQL 8（业务数据 + 判题状态）
```

### 判题状态机

```text
PENDING → JUDGING → AC
                  ├→ WA
                  ├→ CE
                  ├→ RE
                  ├→ TLE
                  └→ MLE
```

提交先落库为 `PENDING`，异步执行后写 `JUDGING`，最终写结果与用例明细；前端轮询结果，不阻塞提交请求。

---

## 目录结构

```text
.
├── backend/
│   └── codeagent/
│       ├── src/main/java/com/codeagent/
│       │   ├── controller/        # REST 接口
│       │   ├── service/           # 业务接口与实现
│       │   ├── mapper/            # MyBatis Mapper
│       │   ├── entity/            # 数据库实体
│       │   ├── dto/               # 请求对象
│       │   ├── vo/                # 响应对象
│       │   ├── config/            # Web / AI / 文件配置
│       │   ├── ai/                # Agent、Prompt、RAG
│       │   └── common/            # R<T>、异常、常量
│       ├── src/main/resources/
│       │   ├── application.yml
│       │   ├── mapper/            # MyBatis XML
│       │   └── db/                # V1 ~ V9 数据库脚本
│       └── deploy/go-judge/       # 判题镜像与 Compose
├── frontend/
│   ├── src/
│   │   ├── api/                   # Axios 接口封装
│   │   ├── components/            # 编辑器、AI 时间线、图表等
│   │   ├── layouts/               # 公共布局
│   │   ├── router/                # 路由与守卫
│   │   ├── store/                 # Pinia
│   │   └── views/                 # 页面
│   └── vite.config.js
├── .env.example                   # 环境变量模板（不含真实密钥）
└── README.md
```

---

## 快速开始

### 0. 环境要求

| 组件 | 版本 / 说明 |
|---|---|
| JDK | 17+ |
| Maven | 3.9+ |
| Node.js | 18+，推荐 20 LTS |
| MySQL | 8.0+ |
| Docker | 用于启动 go-judge，需要 Linux 容器 |
| 端口 | 后端 8080、前端 5173、go-judge 5050 |

### 1. 克隆仓库

```bash
git clone https://github.com/moshui250/moshui.git
cd moshui
```

> 仓库默认分支为 `main`。

### 2. 初始化数据库

创建数据库：

```sql
CREATE DATABASE codeagent
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

按文件名顺序执行 `backend/codeagent/src/main/resources/db/` 下的脚本（V1 → V9）：

```bash
cd backend/codeagent/src/main/resources/db
for f in $(ls V*.sql | sort -V); do
  mysql -u root -p codeagent < "$f"
done
```

Windows PowerShell 可以逐个执行：

```powershell
Get-ChildItem V*.sql | Sort-Object Name | ForEach-Object {
  mysql -u root -p codeagent < $_.FullName
}
```

脚本说明：

| 脚本 | 内容 |
|---|---|
| `V1__init.sql` | 核心 11 张表 |
| `V2__seed.sql` | 公共题、用例、题解、知识点 |
| `V3__demo.sql` | 演示用户与演示提交 |
| `V4__contest.sql` | 竞赛表与竞赛提交字段 |
| `V5__question_visibility.sql` | 题目可见性 |
| `V6__user_avatar.sql` | 用户头像 |
| `V7__multi_knowledge_point.sql` | 题目-知识点多对多 |
| `V8__more_samples.sql` | 更多样例 |
| `V9__basic_topics_and_questions.sql` | 基础知识点与补充题 |

> V3 提供演示账号 `demo / Demo@123456`，仅用于本地演示，请勿用于生产。

### 3. 配置环境变量

复制模板：

```bash
cp .env.example .env
```

**注意：Spring Boot 不会自动读取 `.env` 文件。** `.env` 只作为本地记录，实际请在 IDE 运行配置、PowerShell、bash 或 Docker Compose 中注入环境变量。

最少需要配置：

```bash
export DB_HOST=localhost
export DB_PORT=3306
export DB_NAME=codeagent
export DB_USERNAME=root
export DB_PASSWORD=your-password
export JWT_SECRET="$(openssl rand -base64 48)"
```

PowerShell：

```powershell
$env:DB_HOST="localhost"
$env:DB_PORT="3306"
$env:DB_NAME="codeagent"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="your-password"
$env:JWT_SECRET="use-a-random-secret-at-least-32-bytes"
```

完整的变量说明见 [环境变量](#环境变量) 与 `.env.example`。

### 4. 启动 go-judge

```bash
cd backend/codeagent/deploy/go-judge
docker compose up -d --build
docker compose ps
curl http://127.0.0.1:5050/version
```

`/version` 应返回 go-judge 版本、`os: linux` 和 `platform`。更多细节见 `backend/codeagent/deploy/go-judge/README.md`。

> 5050 不要直接暴露到公网；它执行用户提交的任意代码。默认 Compose 只绑定 `127.0.0.1`。

### 5. 启动后端

```bash
cd backend/codeagent
mvn -B -DskipTests package
java -jar target/codeagent-0.0.1-SNAPSHOT.jar
```

开发时也可以直接：

```bash
mvn -DskipTests spring-boot:run
```

健康检查：

```bash
curl http://127.0.0.1:8080/api/health
```

### 6. 启动前端

```bash
cd frontend
npm install
npm run dev
```

打开 `http://127.0.0.1:5173`。

前端开发服务器会把 `/api` 与 `/uploads` 代理到 `http://localhost:8080`；生产环境由 Nginx 同域代理，见 [图片存储](#图片存储)。

### 7. 体验路径

1. 使用演示账号登录（或注册新账号）；
2. 进入题库，选择“数组元素求和”；
3. 运行样例；
4. 提交不完整代码，得到 WA；
5. 打开 AI 调试，输入卡点；
6. 查看“先对焦 / 拆开看 / 你来判断 / 最小验证”；
7. 修改后再提交；
8. 打开错题本、学习看板，查看记录与推荐。

---

## 环境变量

完整模板见 [`.env.example`](.env.example)。所有密钥只从环境变量读取，仓库不保存真实值。

| 变量 | 必填 | 说明 |
|---|---|---|
| `DB_HOST` / `DB_PORT` / `DB_NAME` | 是 | MySQL 地址、端口、库名 |
| `DB_USERNAME` / `DB_PASSWORD` | 是 | MySQL 账号 |
| `JWT_SECRET` | 是 | HS256 密钥，至少 32 字节；未配置时启动失败 |
| `SILICONFLOW_API_KEY` | RAG | SiliconFlow Embedding 密钥 |
| `SILICONFLOW_BASE_URL` | 否 | 默认 `https://api.siliconflow.cn` |
| `SILICONFLOW_EMBEDDING_MODEL` | 否 | 默认 `BAAI/bge-m3`，输出 1024 维 |
| `ZILLIZ_URI` / `ZILLIZ_TOKEN` | RAG | Zilliz 连接信息；不配时 RAG 降级 |
| `ZILLIZ_DATABASE` | 否 | 默认 `default` |
| `DEEPSEEK_API_KEY` | AI | DeepSeek 对话密钥；不配时 AI 接口返回可读错误 |
| `DEEPSEEK_BASE_URL` | 否 | 默认 `https://api.deepseek.com` |
| `DEEPSEEK_MODEL` | 否 | 默认 `deepseek-chat`；不建议用流式只返回 reasoning 的模型 |
| `DASHSCOPE_API_KEY` | 图片录题 | DashScope 视觉模型密钥 |
| `DASHSCOPE_BASE_URL` | 否 | 默认兼容模式地址 |
| `DASHSCOPE_VISION_MODEL` | 否 | 默认 `qwen-vl-max` |
| `DASHSCOPE_FALLBACK_VISION_MODEL` | 否 | 默认 `qwen3-vl-plus` |
| `FILE_UPLOAD_DIR` | 是 | 图片持久目录，默认 `/data/upload` |
| `FILE_PUBLIC_BASE_URL` | 否 | 图片对外前缀；留空返回 `/uploads/...` |
| `GOJUDGE_BASE_URL` | 是 | 默认 `http://127.0.0.1:5050` |
| `GOJUDGE_JAVA_HOME` / `GOJUDGE_PATH` | 是 | 判题镜像内 JDK 与工具链路径 |
| `GOJUDGE_LD_LIBRARY_PATH` | 否 | 精简 JRE 需要包含 `lib` 与 `lib/server` |
| `GOJUDGE_JAVA_XMX` | 否 | 默认 `512m` |
| `RAG_ZILLIZ_INITIALIZE_SCHEMA` | 否 | 首次建集合时设 `true`，生产保持 `false` |

### 密钥安全

- 不要提交 `.env`、数据库密码、JWT 密钥、第三方 API Key。
- `JWT_SECRET` 没有默认值，缺失时后端会启动失败，这是有意的。
- 如果密钥曾出现在提交历史、截图或聊天记录中，请立即轮换。
- `.env` 与 `.env.*` 已在 `.gitignore` 中忽略，`.env.example` 作为模板保留。

---

## 判题服务

CodeAgent 使用 [go-judge](https://github.com/criyle/go-judge) 执行 Java / C++ 提交，不再依赖公共 Piston API。

- 判题镜像：`backend/codeagent/deploy/go-judge/`
- 默认地址：`http://127.0.0.1:5050`
- 状态：AC、WA、CE、RE、TLE、MLE
- 隐藏用例不下发前端；失败时只返回最小对比
- 提交异步执行，前端轮询结果
- 连续失败触发熔断，避免判题服务异常拖垮业务

本地启动：

```bash
cd backend/codeagent/deploy/go-judge
docker compose up -d --build
```

生产环境应把 go-judge 放在独立、无敏感数据的节点，并限制网络访问。

---

## AI 与 RAG

### AI 调试

- 触发时机：判题失败后解锁，由学生主动发起；
- 上下文：题目、当前代码、判题日志、最近对话；
- 工具：系统预执行 `locate_error`、`retrieve_knowledge`；
- 输出：先对焦 / 拆开看 / 你来判断 / 最小验证；
- 约束：不输出完整可提交代码，不展示工具调用与推理日志，不暴露检索命中数；
- 传输：SSE，前端用 `fetch + ReadableStream` 消费。

### RAG 知识库

- 资料源：题目题面、题解、知识点、错题手册；
- 向量库：Zilliz（Milvus 协议），集合 `codeagent_docs`；
- Embedding：SiliconFlow `BAAI/bge-m3`，1024 维；
- 切片：约 500 字，重叠 80；
- 降级：向量检索不可用时退回关键词检索；
- 健康检查：`GET /api/health/rag`；
- 重建：开发环境使用 `init` profile，调用 `RagIndexServiceImpl.rebuild()`。

首次建集合：

```bash
RAG_ZILLIZ_INITIALIZE_SCHEMA=true mvn -DskipTests spring-boot:run
```

建完后把 `RAG_ZILLIZ_INITIALIZE_SCHEMA` 改回 `false`。

---

## 图片存储

题目图片保存在后端本地目录，数据库只保存访问 URL。

```bash
mkdir -p /srv/codeagent/uploads
chown -R 1000:1000 /srv/codeagent/uploads
export FILE_UPLOAD_DIR=/srv/codeagent/uploads
```

Docker 部署必须挂载持久卷，否则容器重建后图片丢失：

```yaml
volumes:
  - codeagent_uploads:/data/upload
```

Nginx 同域部署时，除 `/api/` 外还要代理 `/uploads/`：

```nginx
location /api/     { proxy_pass http://127.0.0.1:8080; }
location /uploads/ { proxy_pass http://127.0.0.1:8080; }
```

如果图片由独立域名或 CDN 提供：

```bash
FILE_PUBLIC_BASE_URL=https://your-domain
```

上传接口会返回完整 URL。

---

## 构建与测试

### 后端

```bash
cd backend/codeagent
mvn -B -DskipTests compile
mvn test
mvn -B -DskipTests package
```

说明：测试会启动 Spring 上下文；未配置 Zilliz、go-judge 时，日志中的连接告警属于预期环境提示，不代表业务测试失败。

### 前端

```bash
cd frontend
npm install
npm run build
npm run preview
```

### 手工验收

- 登录 / 注册 / 退出；
- 题库筛选与题目详情；
- 运行样例（不落库）；
- 提交 AC / WA / CE / RE / TLE / MLE；
- 失败后 AI 调试返回四块结构；
- 错题本记录与掌握切换；
- 学习看板与知识图谱切换；
- 竞赛报名、题单、排名与 AI 禁用；
- 图片录题上传、解析、草稿确认。

---

## API 概览

后端统一返回 `R<T> { code, message, data }`，业务异常由 `GlobalExceptionHandler` 转成统一响应。

| 模块 | 主要接口 | 数量 |
|---|---|---|
| 认证 | `/api/auth/register`、`login`、`logout`、`me`、`profile` | 5 |
| 题目 | `/api/question/list`、`recommend`、`{id}`、创建/编辑/删除、`parse-image`、`import-parsed`、`/api/file/image` | 9 |
| 判题 | `/api/judge/submit`、`run-sample`、`result/{id}`、`history` | 4 |
| AI | `/api/ai/explain`、`/api/ai/agent`、`/api/agent/session`、`stream`、`history` | 5 |
| 学习 | `/api/wrong/*`、`/api/profile/stat`、`/api/home/summary`、`/api/rank*`、`/api/knowledge-point/list` | 7 |
| 竞赛 | `/api/contest*`、`/api/admin/contest` | 5 |
| 健康 | `/api/health`、`/api/health/rag` | 2 |

完整接口可通过控制器源码查看：`backend/codeagent/src/main/java/com/codeagent/controller/`。

---

## 安全约定

- 不提交 `.env`、`node_modules`、`target`、`dist`、本地 Maven 仓库。
- 不在代码、配置、提交信息、截图里写真实密钥、数据库密码或个人信息。
- `JWT_SECRET` 没有默认值，未配置时后端启动失败。
- 题目可见性：PUBLIC / PRIVATE / SHARED；个人题目只有作者可改，PUBLIC 只有管理员可建。
- 隐藏用例不下发；判题失败只返回最小对比。
- 上传文件做类型、大小、目录可写校验；生产环境使用持久卷。
- go-judge 不直接暴露公网，放在独立判题节点。

---

## 团队信息

| 项目 | 内容 |
|---|---|
| 团队名称 | 向量 |
| 项目名称 | CodeAgent 智码刷题平台 |
| 参赛赛事 | 2026 传智杯 |
| 成员分工 | 后端与判题 / AI 与 RAG / 前端与交互 / 测试与答辩材料 |
| 代码仓库 | `https://github.com/moshui250/moshui` |
| 默认分支 | `main` |

---

## 说明

- 当前为单节点单体部署，未做生产级压测与水平扩展，不宣称高并发能力。
- 没有引入 Redis / MQ / ES；判题队列基于有界线程池，后续可替换为 MQ。
- 竞赛反作弊、教师端、班级体系尚未实现，属于后续规划。
- 自动化测试以 Spring Boot 测试与手工 E2E 为主，覆盖范围有限，边界已在文档中明确。

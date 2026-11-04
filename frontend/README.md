# CodeAgent Web

CodeAgent 智码刷题平台前端骨架，基于 Vue 3 + Vite + Element Plus + Pinia。

## 环境要求

- Node.js >= 18（推荐 20 / 22 LTS）
- 后端服务默认运行在 `http://localhost:8080`

## 启动

```bash
npm install
npm run dev
```

打开 `http://localhost:5173`。开发服务器会把 `/api` 原样代理到 `http://localhost:8080`，不会去掉 `/api` 前缀。

构建与本地预览：

```bash
npm run build
npm run preview
```

## 环境变量

前端默认通过 Vite 代理访问 `/api`，登录守卫已强制启用，无需额外开关。当前 `.env.example` 仅作说明，不包含可绕过认证的变量。

## 页面与路由

| 路由 | 页面 | 说明 |
| --- | --- | --- |
| `/login` | 登录 | 调用 `POST /api/auth/login` |
| `/register` | 注册 | 调用 `POST /api/auth/register` |
| `/question` | 题库 | 知识点筛选、难度筛选、搜索、分页 |
| `/question/:id` | 题目详情 | 题面、样例、知识点；编码区在 S6 接入 |
| `/wrong-book` | 错题本 | 调用 `/api/wrong/list`，未接入时展示空状态 |
| `/dashboard` | 数据看板 | ECharts 知识点错误率；统计接口在 S14 接入 |
| `/profile` | 个人中心 | 账号信息、本地编辑器偏好 |

## 接口约定

- 统一响应：`{ code, message, data }`，`code === 0` 表示成功。
- axios 在 `src/utils/request.js` 中统一解包 `data`，业务代码直接拿到数据。
- token 存在 `localStorage` 的 `codeagent_token`，请求自动携带 `Authorization: Bearer <token>`。
- `40100` 会清理本地 token，并只提示、跳转一次登录页。
- 路由守卫：无 token 访问业务页会跳转 `/login?redirect=...`；刷新时若只有 token，会先调用 `/api/auth/me` 恢复用户信息。
- 后端未就绪时，题库和题目详情会回退到 `src/data/demo.js` 的演示数据，并在页面顶部明确标注“前端骨架演示数据”；不会伪造用户学习记录。

## 目录

```text
src/
├─ api/          # 按领域拆分的接口封装
├─ components/   # 可复用组件（登录/注册品牌面板）
├─ data/         # 仅用于骨架期的演示数据
├─ layouts/      # 主布局（侧边栏 + 顶栏 + 健康检查）
├─ router/       # 路由与登录守卫
├─ store/        # Pinia userStore
├─ styles/       # 全局设计变量与页面基础样式
├─ utils/        # axios 实例与拦截器
└─ views/        # 页面
```

## 后续接入

- 当前已完成：认证接口、JWT 拦截器、登录/注册页、路由守卫与 token 管理。
- S5/S6：题库真实接口与 Monaco 答题页。
- S7/S8：提交判题与轮询结果。
- S11/S12：SSE + AI 调试对话。
- S14：学习统计看板真实数据。

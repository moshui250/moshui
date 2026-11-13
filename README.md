# CodeAgent 智码刷题平台

AI 原生的编程练习平台，前后端单仓库。

## 目录

- `backend/codeagent`：Spring Boot 3.5 + MyBatis + MySQL 后端
- `frontend`：Vue 3 + Vite + Element Plus + Monaco 前端

## 环境变量

仓库不保存任何密钥。运行前通过环境变量提供：

| 变量 | 说明 |
|---|---|
| `DB_HOST` / `DB_PORT` / `DB_NAME` | MySQL 连接信息 |
| `DB_USERNAME` / `DB_PASSWORD` | MySQL 账号 |
| `JWT_SECRET` | 必填，至少 32 字节的随机字符串 |
| `SILICONFLOW_API_KEY` | SiliconFlow Embedding |
| `ZILLIZ_URI` / `ZILLIZ_TOKEN` / `ZILLIZ_DATABASE` | Zilliz 连接信息 |
| `DEEPSEEK_API_KEY` | DeepSeek 对话模型 |
| `FILE_UPLOAD_DIR` | 题目图片落盘目录，默认 `/data/upload` |
| `FILE_PUBLIC_BASE_URL` | 图片对外前缀；留空返回 `/uploads/...`，跨域部署时填后端域名 |
| `GOJUDGE_BASE_URL` | go-judge 地址，默认 `http://127.0.0.1:5050` |
| `GOJUDGE_JAVA_HOME` | 判题镜像内 JDK 路径，官方镜像为 `/usr/lib/jvm/java-17-openjdk-amd64` |
| `GOJUDGE_PATH` | 判题镜像内工具链 PATH |
| `GOJUDGE_LD_LIBRARY_PATH` | 精简 JRE 的动态库路径，默认 `/usr/local/java15/lib:/usr/local/java15/lib/server` |

## 启动

后端：

`powershell
cd backend/codeagent
mvn -B clean package
java -jar target/codeagent-0.0.1-SNAPSHOT.jar
`

前端：

`powershell
cd frontend
npm install
npm run dev
`

数据库脚本位于 `backend/codeagent/src/main/resources/db/`，按 `V1__init.sql`、`V2__seed.sql` 顺序执行。

判题服务部署见 `backend/codeagent/deploy/go-judge/README.md`。

## 图片存储

题目图片保存在后端本地目录，数据库只保存访问 URL。生产环境必须使用持久目录，并保证运行用户可写：

```bash
mkdir -p /srv/codeagent/uploads
chown -R 1000:1000 /srv/codeagent/uploads
export FILE_UPLOAD_DIR=/srv/codeagent/uploads
```

Docker 部署时应挂载卷，否则容器重建后图片会丢失：

```bash
-v codeagent_uploads:/data/upload
```

前端和后端由 Nginx 同域提供服务时，除 `/api/` 外还要代理 `/uploads/`：

```nginx
location /api/     { proxy_pass http://127.0.0.1:8080; }
location /uploads/ { proxy_pass http://127.0.0.1:8080; }
```

如果图片由独立域名或 CDN 提供，设置 `FILE_PUBLIC_BASE_URL=https://your-domain`，上传接口会返回完整 URL。

## 安全约定

- `.env`、`node_modules`、`target`、`dist`、本地 Maven 仓库均不提交。
- 不要在代码、配置、提交信息中写入真实密钥、数据库密码或个人信息。
- `JWT_SECRET` 没有默认值，未配置时后端会启动失败。
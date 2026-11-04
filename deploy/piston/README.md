# 自托管 Piston 判题服务

公共 Piston API 自 2026-02-15 起只对白名单开放；本项目默认配置仍指向
`https://emkc.org`，因此在未获白名单时提交会返回 `SE`。生产/演示环境应使用本目录的
Piston 容器，或任何兼容 Piston `/api/v2` 协议的私有实例。

## 启动

需要 Docker Desktop（Windows 需启用 WSL2）：

```powershell
docker compose -f deploy/piston/docker-compose.yml up -d
```

首次启动后安装本项目使用的两个运行时。版本必须与
`backend/codeagent/src/main/resources/application.yml` 中 `piston.languages` 一致：

```powershell
curl.exe -X POST http://127.0.0.1:2000/api/v2/packages `
  -H "Content-Type: application/json" `
  -d '{\"language\":\"java\",\"version\":\"15.0.2\"}'

curl.exe -X POST http://127.0.0.1:2000/api/v2/packages `
  -H "Content-Type: application/json" `
  -d '{\"language\":\"gcc\",\"version\":\"10.2.0\"}'
```

检查运行时：

```powershell
curl.exe http://127.0.0.1:2000/api/v2/runtimes
```

## 接入后端

启动后端时把地址指到本地 Piston：

```powershell
$env:PISTON_BASE_URL = "http://127.0.0.1:2000"
$env:PISTON_API_PATH = "/api/v2"
java -jar backend/codeagent/target/codeagent-0.0.1-SNAPSHOT.jar
```

注意：公共 API 的路径前缀是 `/api/v2/piston`，自托管 Piston 是 `/api/v2`；必须同时设置
`PISTON_API_PATH`，否则会调用到不存在的路径并得到 HTTP 404。

`PistonRuntimeInitializer` 会打印实际版本；如果版本与配置不一致，会把内存中的版本
更新为远端真实值并打印警告。判题和 Agent 的 `judge_code` 都复用这个地址。

## 说明

- 容器需要 `privileged: true`，只应部署在受控的内网/演示机。
- 首次安装运行时需要下载编译环境，耗时取决于网络。
- 若无法运行 Docker，至少把 `PISTON_BASE_URL` 指向已获白名单的 Piston 实例；
  否则 `/api/judge/submit` 会按设计返回 `SE`，并在 `errorMessage` 中提示配置可用实例。

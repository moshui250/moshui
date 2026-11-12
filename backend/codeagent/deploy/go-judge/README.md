# go-judge 判题服务

CodeAgent 的 Java / C++ 判题由 [go-judge](https://github.com/criyle/go-judge) 执行，不再依赖 Piston。
本目录提供一个可直接构建的镜像，包含 `g++`、OpenJDK 17 和 go-judge v1.14.0。

## 构建与启动

要求：Docker Engine（Linux 容器），建议至少 2 CPU / 2 GB 内存。

```bash
cd backend/codeagent/deploy/go-judge
docker compose up -d --build
docker compose ps
curl http://127.0.0.1:5050/version
```

`/version` 应返回 `buildVersion`（v1.14.0）、`os: linux` 和 `platform: amd64`。

也可以直接运行：

```bash
docker build -t codeagent/go-judge:1.14.0 .
docker run -d --name codeagent-gojudge \
  --privileged --shm-size=256m \
  -p 127.0.0.1:5050:5050 \
  codeagent/go-judge:1.14.0
```

## 后端配置

后端默认连接 `http://127.0.0.1:5050`。后端进程不在容器内时：

```bash
GOJUDGE_BASE_URL=http://127.0.0.1:5050
GOJUDGE_JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
GOJUDGE_PATH=/usr/lib/jvm/java-17-openjdk-amd64/bin:/usr/local/bin:/usr/bin:/bin
```

后端也在 Docker 网络内时，把 `GOJUDGE_BASE_URL` 改成 `http://go-judge:5050`，并把 `GOJUDGE_JAVA_HOME` / `GOJUDGE_PATH` 保持为镜像内路径。
其余超时、内存、进程数、输出上限见 `application.yml` 的 `gojudge` 段。

## 手工验证

```bash
curl -s http://127.0.0.1:5050/run \
  -H 'Content-Type: application/json' \
  -d '{
    "cmd": [{
      "args": ["/bin/sh", "-c", "echo go-judge-ok"],
      "files": [
        {"content": ""},
        {"name": "stdout", "max": 1024},
        {"name": "stderr", "max": 1024}
      ],
      "cpuLimit": 1000000000,
      "clockLimit": 2000000000,
      "memoryLimit": 268435456,
      "procLimit": 16,
      "copyOut": ["stdout", "stderr"]
    }]
  }'
```

返回的 `results[0].status` 为 `Accepted`，`files.stdout` 为 `go-judge-ok\n`。

## 注意

- go-judge 会执行用户提交的任意代码；`5050` 不要直接暴露到公网。默认 compose 只绑定 `127.0.0.1`。
- `privileged: true` 是 go-judge 创建沙箱所必需的；生产环境应放在独立、无敏感数据的判题节点。
- 编译和运行共用一次沙箱调用，编译失败通过 `compile.err` 回传，运行超时/内存超限由 go-judge 状态返回。
- GitHub Release 下载受限时，可在构建阶段使用代理，或先把对应 `.deb` 放到构建上下文并修改 Dockerfile 的下载步骤。
- 当前镜像包含 Java 17；如果改用其它 JDK，需同步设置 `GOJUDGE_JAVA_HOME` 和 `GOJUDGE_PATH`。

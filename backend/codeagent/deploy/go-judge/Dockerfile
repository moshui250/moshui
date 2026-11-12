FROM ubuntu:24.04

ARG GOJUDGE_VERSION=1.14.0
ARG TARGETARCH=amd64

ENV DEBIAN_FRONTEND=noninteractive \
    LANG=C.UTF-8 \
    JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 \
    GOJUDGE_JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 \
    GOJUDGE_PATH=/usr/lib/jvm/java-17-openjdk-amd64/bin:/usr/local/bin:/usr/bin:/bin

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        ca-certificates \
        curl \
        g++ \
        openjdk-17-jdk-headless \
        tzdata \
    && rm -rf /var/lib/apt/lists/*

# go-judge 官方发行包自带二进制；架构与 Docker TARGETARCH 对齐（amd64/arm64）。
RUN set -eux; \
    arch="${TARGETARCH:-amd64}"; \
    curl -fL --retry 3 --retry-delay 2 \
        -o /tmp/go-judge.deb \
        "https://github.com/criyle/go-judge/releases/download/v${GOJUDGE_VERSION}/go-judge_${GOJUDGE_VERSION}_linux_${arch}.deb"; \
    dpkg -i /tmp/go-judge.deb; \
    rm -f /tmp/go-judge.deb; \
    go-judge -version

EXPOSE 5050

HEALTHCHECK --interval=10s --timeout=3s --start-period=5s --retries=5 \
    CMD curl -fsS http://127.0.0.1:5050/version >/dev/null || exit 1

ENTRYPOINT ["go-judge"]
CMD ["-http-addr=0.0.0.0:5050", "-parallelism=4"]

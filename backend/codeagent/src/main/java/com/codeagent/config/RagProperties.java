package com.codeagent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * RAG 管道配置：向量库连接信息与 Markdown 切块参数。
 */
@Data
@ConfigurationProperties(prefix = "rag")
public class RagProperties {

    private Zilliz zilliz = new Zilliz();

    private Chunk chunk = new Chunk();

    @Data
    public static class Zilliz {

        /** Zilliz Cloud 连接地址，例如 https://xxx.zilliz.com:443。 */
        private String uri;

        /** Zilliz Cloud API Token，只从环境变量读取。 */
        private String token;

        private String databaseName = "default";

        private String collectionName = "codeagent_docs";

        private int dimension = 1024;

        /** 是否由 Spring AI 自动建集合；已有集合时可关闭，避免受建表权限限制。 */
        private boolean initializeSchema = true;
    }

    @Data
    public static class Chunk {

        /** 目标切块长度（字符数）。 */
        private int size = 500;

        /** 尽量达到的最小长度，短文档允许不足。 */
        private int minSize = 400;

        /** 相邻窗口重叠字符数。 */
        private int overlap = 80;
    }
}

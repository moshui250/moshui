package com.codeagent.config;

import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.milvus.MilvusVectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 手写 Zilliz/Milvus VectorStore Bean。
 *
 * <p>不依赖 spring.ai.vectorstore.milvus.* 自动配置，避免 Cloud Token 在
 * 自动配置层被错误处理。未配置 ZILLIZ_URI 时不创建该 Bean，便于本地无凭据启动。</p>
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(RagProperties.class)
public class RagVectorStoreConfig {

    /**
     * 创建 Milvus 向量库客户端与 VectorStore。
     *
     * <p>Token 错误或网络不可达时返回 null，让应用仍能启动；
     * AI 讲解会通过 ObjectProvider 发现没有向量库并降级为关键词检索。</p>
     *
     * @param embeddingModel Spring AI 根据 spring.ai.openai.* 自动装配的 embedding 模型
     * @param properties     Zilliz 地址、Token、库名、集合名与向量维度
     * @return 使用 codeagent_docs 集合的 VectorStore；初始化失败时为 null
     */
    @Bean
    @ConditionalOnExpression("'${rag.zilliz.uri:}' != ''")
    public VectorStore ragVectorStore(EmbeddingModel embeddingModel, RagProperties properties) {
        RagProperties.Zilliz zilliz = properties.getZilliz();
        try {
            ConnectParam connectParam = ConnectParam.newBuilder()
                    .withUri(zilliz.getUri())
                    .withToken(zilliz.getToken())
                    .withDatabaseName(zilliz.getDatabaseName())
                    .build();
            MilvusServiceClient client = new MilvusServiceClient(connectParam);
            return MilvusVectorStore.builder(client, embeddingModel)
                    .collectionName(zilliz.getCollectionName())
                    .databaseName(zilliz.getDatabaseName())
                    .embeddingDimension(zilliz.getDimension())
                    .initializeSchema(zilliz.isInitializeSchema())
                    .build();
        } catch (Exception e) {
            log.warn("Zilliz 初始化失败，AI 讲解将降级为关键词检索: {}", e.getMessage());
            return null;
        }
    }
}

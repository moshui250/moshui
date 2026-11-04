package com.codeagent.ai.agent;

import com.codeagent.ai.rag.ErrorManual;
import com.codeagent.ai.rag.RagChunk;
import com.codeagent.ai.rag.RagChunker;
import com.codeagent.ai.rag.RagSourceDocument;
import com.codeagent.entity.KnowledgePoint;
import com.codeagent.entity.Question;
import com.codeagent.mapper.KnowledgePointMapper;
import com.codeagent.mapper.QuestionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Agent 的知识检索器。
 * 优先走向量库；向量库不可用或没有命中时，用 MyBatis LIKE 查询知识点、题目和内置手册。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeRetriever {

    private static final double MIN_SCORE = 0.35;

    private final ObjectProvider<VectorStore> vectorStoreProvider;
    private final KnowledgePointMapper knowledgePointMapper;
    private final QuestionMapper questionMapper;
    private final RagChunker ragChunker;

    /**
     * 检索与 query 相关的知识片段。
     *
     * @param query             检索问题
     * @param knowledgePointCode 知识点编码，可为空
     * @param topK              期望条数，服务端限制为 1~8
     * @return 检索结果和是否发生降级
     */
    public RetrievalResult retrieve(String query, String knowledgePointCode, int topK) {
        int limit = Math.max(1, Math.min(8, topK));
        VectorStore vectorStore = vectorStoreProvider.getIfAvailable();
        if (vectorStore == null) {
            return new RetrievalResult(keywordSearch(query, limit), true);
        }

        try {
            SearchRequest.Builder builder = SearchRequest.builder()
                    .query(query)
                    .topK(limit)
                    .similarityThreshold(0.0);
            if (knowledgePointCode != null && !knowledgePointCode.isBlank()) {
                builder.filterExpression(new FilterExpressionBuilder()
                        .eq("knowledge_point_code", knowledgePointCode)
                        .build());
            }

            List<Document> documents = vectorStore.similaritySearch(builder.build());
            List<RetrievedChunk> chunks = new ArrayList<>();
            if (documents != null) {
                for (Document document : documents) {
                    double score = document.getScore() == null ? 0.0 : document.getScore();
                    if (score < MIN_SCORE) {
                        continue;
                    }
                    Map<String, Object> metadata = document.getMetadata();
                    String title = stringValue(metadata.get("title"), "知识库片段");
                    String sourceType = stringValue(metadata.get("source_type"), "UNKNOWN");
                    String sourceId = stringValue(metadata.get("source_id"), "0");
                    String chunkIndex = stringValue(metadata.get("chunk_index"), "0");
                    chunks.add(new RetrievedChunk(
                            sourceType + ":" + sourceId + ":" + chunkIndex,
                            title,
                            truncate(document.getText(), 600),
                            sourceType,
                            score));
                    if (chunks.size() >= limit) {
                        break;
                    }
                }
            }
            if (chunks.isEmpty()) {
                return new RetrievalResult(keywordSearch(query, limit), true);
            }
            return new RetrievalResult(chunks, false);
        } catch (Exception e) {
            log.warn("Agent 向量检索失败，降级为关键词检索: {}", e.getMessage());
            return new RetrievalResult(keywordSearch(query, limit), true);
        }
    }

    /**
     * 向量库不可用时的关键词检索。
     *
     * @param query 检索问题
     * @param limit 最大条数
     * @return 命中的知识片段
     */
    private List<RetrievedChunk> keywordSearch(String query, int limit) {
        String keyword = normalizeKeyword(query);
        if (keyword.isBlank()) {
            return List.of();
        }
        String safeKeyword = keyword.replace("%", "").replace("_", "");
        if (safeKeyword.isBlank()) {
            return List.of();
        }

        List<RetrievedChunk> chunks = new ArrayList<>();
        for (KnowledgePoint point : knowledgePointMapper.selectByNameLike(safeKeyword)) {
            chunks.add(new RetrievedChunk(
                    "KNOWLEDGE:" + point.getId() + ":0",
                    point.getName(),
                    "知识点：" + point.getName() + "\n" + safe(point.getDescription()),
                    "KNOWLEDGE",
                    0.0));
        }
        for (Question question : questionMapper.selectByTitleLike(safeKeyword)) {
            chunks.add(new RetrievedChunk(
                    "QUESTION:" + question.getId() + ":0",
                    question.getTitle(),
                    "题目：" + question.getTitle() + "\n" + safe(question.getDescription()),
                    "QUESTION",
                    0.0));
        }

        RagSourceDocument manual = RagSourceDocument.builder()
                .sourceType("ERROR_MANUAL")
                .sourceId(0L)
                .title("刷题常见错误手册")
                .knowledgePointCode("")
                .content(ErrorManual.TEXT)
                .build();
        for (RagChunk chunk : ragChunker.split(manual)) {
            if (chunk.title().contains(safeKeyword) || chunk.text().contains(safeKeyword)) {
                chunks.add(new RetrievedChunk(
                        "ERROR_MANUAL:0:" + chunk.chunkIndex(),
                        chunk.title(),
                        truncate(chunk.text(), 600),
                        "ERROR_MANUAL",
                        0.0));
            }
            if (chunks.size() >= limit) {
                break;
            }
        }
        return chunks.stream().limit(limit).toList();
    }

    /**
     * 去掉常见问句前后缀，保留核心检索词。
     *
     * @param query 原始问题
     * @return 清洗后的关键词
     */
    private String normalizeKeyword(String query) {
        if (query == null) {
            return "";
        }
        String keyword = query.trim()
                .replaceAll("^(请问|请|帮我|我想问|什么是|怎么|如何|为什么|讲讲|解释一下)", "")
                .replaceAll("(怎么办|怎么写|怎么做|是什么|为什么)$", "")
                .replaceAll("[\\p{Punct}\\s，。！？；：、]+", "");
        return keyword.isBlank() ? query.trim() : keyword;
    }

    /**
     * 安全读取字符串。
     *
     * @param value        原值
     * @param defaultValue 默认值
     * @return 非空字符串
     */
    private String stringValue(Object value, String defaultValue) {
        return value == null ? defaultValue : String.valueOf(value);
    }

    /**
     * 空值转空串。
     *
     * @param value 原值
     * @return 原值或空串
     */
    private String safe(String value) {
        return value == null ? "" : value;
    }

    /**
     * 截断过长文本。
     *
     * @param value 原文本
     * @param limit 最大长度
     * @return 截断结果
     */
    private String truncate(String value, int limit) {
        if (value == null) {
            return "";
        }
        return value.length() <= limit ? value : value.substring(0, limit) + "…";
    }

    /**
     * 检索结果集。
     *
     * @param chunks   命中片段
     * @param degraded 是否使用关键词降级
     */
    public record RetrievalResult(List<RetrievedChunk> chunks, boolean degraded) {
    }

    /**
     * 单个命中片段。
     *
     * @param docId      文档标识
     * @param title      来源标题
     * @param snippet    片段正文
     * @param sourceType 来源类型
     * @param score      相似度；关键词降级为 0
     */
    public record RetrievedChunk(String docId, String title, String snippet,
                                 String sourceType, double score) {
    }
}

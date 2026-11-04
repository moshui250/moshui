package com.codeagent.service.impl;

import com.codeagent.ai.rag.RagChunk;
import com.codeagent.ai.rag.RagChunker;
import com.codeagent.ai.rag.RagDocLoader;
import com.codeagent.ai.rag.RagRebuildResult;
import com.codeagent.ai.rag.RagSourceDocument;
import com.codeagent.common.ErrorCode;
import com.codeagent.entity.RagDocument;
import com.codeagent.exception.BizException;
import com.codeagent.mapper.RagDocumentMapper;
import com.codeagent.service.RagIndexService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * RAG 全量索引实现：按内容哈希做幂等，变更时先删旧向量再写新向量。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagIndexServiceImpl implements RagIndexService {

    private final RagDocLoader ragDocLoader;
    private final RagChunker ragChunker;
    private final RagDocumentMapper ragDocumentMapper;
    private final ObjectProvider<VectorStore> vectorStoreProvider;
    private final ObjectMapper objectMapper;

    /**
     * 重建全部 RAG 索引。
     *
     * @return 重建统计
     */
    @Override
    public synchronized RagRebuildResult rebuild() {
        VectorStore vectorStore = vectorStoreProvider.getIfAvailable();
        if (vectorStore == null) {
            throw new BizException(ErrorCode.AI_UNAVAILABLE, "未配置 ZILLIZ_URI，无法重建 RAG");
        }
        List<RagSourceDocument> documents = ragDocLoader.loadAll();
        int indexed = 0;
        int skipped = 0;
        int failed = 0;
        int chunks = 0;
        for (RagSourceDocument document : documents) {
            String hash = sha256(document.getContent());
            try {
                IndexOutcome outcome = indexOne(vectorStore, document, hash);
                if (outcome.indexed()) {
                    indexed++;
                } else {
                    skipped++;
                }
                chunks += outcome.chunks();
            } catch (Exception e) {
                failed++;
                log.error("RAG 索引失败: sourceType={}, sourceId={}",
                        document.getSourceType(), document.getSourceId(), e);
                markFailed(document, hash);
            }
        }
        return new RagRebuildResult(indexed, skipped, failed, chunks);
    }

    /**
     * 处理一篇文档：哈希未变则跳过，变更则删旧写新。
     *
     * @param vectorStore 向量库
     * @param source      来源文档
     * @param hash        当前内容 SHA-256
     * @return 是否实际写入及块数
     */
    private IndexOutcome indexOne(VectorStore vectorStore, RagSourceDocument source, String hash) {
        RagDocument existing = ragDocumentMapper.selectBySource(source.getSourceType(), source.getSourceId());
        if (existing != null && hash.equals(existing.getContentHash())
                && "READY".equals(existing.getStatus())) {
            int count = existing.getChunkCount() == null ? 0 : existing.getChunkCount();
            return new IndexOutcome(false, count);
        }

        List<RagChunk> chunks = ragChunker.split(source);
        deleteOldVectors(vectorStore, existing, source);
        List<String> vectorIds = new ArrayList<>();
        if (!chunks.isEmpty()) {
            List<Document> vectors = new ArrayList<>(chunks.size());
            for (RagChunk chunk : chunks) {
                String vectorId = UUID.randomUUID().toString();
                vectorIds.add(vectorId);
                vectors.add(Document.builder()
                        .id(vectorId)
                        .text(chunk.text())
                        .metadata(metadata(source, chunk))
                        .build());
            }
            vectorStore.add(vectors);
        }

        RagDocument record = existing == null ? new RagDocument() : existing;
        record.setSourceType(source.getSourceType());
        record.setSourceId(source.getSourceId());
        record.setTitle(source.getTitle());
        record.setKnowledgePointId(source.getKnowledgePointId());
        record.setChunkCount(chunks.size());
        record.setVectorIds(toJson(vectorIds));
        record.setContentHash(hash);
        record.setStatus("READY");
        if (existing == null) {
            ragDocumentMapper.insert(record);
        } else {
            ragDocumentMapper.update(record);
        }
        return new IndexOutcome(true, chunks.size());
    }

    /**
     * 按来源元数据删除旧向量，避免同一文档重复堆积。
     *
     * @param vectorStore 向量库
     * @param existing    已有登记记录，null 表示首次写入
     * @param source      当前来源
     */
    private void deleteOldVectors(VectorStore vectorStore, RagDocument existing, RagSourceDocument source) {
        if (existing == null || existing.getVectorIds() == null
                || "[]".equals(existing.getVectorIds().trim())) {
            // 首次写入或上一轮失败时没有已落库向量，跳过删除，避免无意义的权限调用。
            return;
        }
        FilterExpressionBuilder builder = new FilterExpressionBuilder();
        vectorStore.delete(builder.and(
                builder.eq("source_type", source.getSourceType()),
                builder.eq("source_id", source.getSourceId())
        ).build());
    }

    /**
     * 构造向量 metadata。
     *
     * @param source 来源文档
     * @param chunk  当前切块
     * @return metadata 映射
     */
    private Map<String, Object> metadata(RagSourceDocument source, RagChunk chunk) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("source_type", source.getSourceType());
        metadata.put("source_id", source.getSourceId());
        metadata.put("title", chunk.title());
        metadata.put("knowledge_point_code",
                source.getKnowledgePointCode() == null ? "" : source.getKnowledgePointCode());
        metadata.put("chunk_index", chunk.chunkIndex());
        if (source.getKnowledgePointId() != null) {
            metadata.put("knowledge_point_id", source.getKnowledgePointId());
        }
        return metadata;
    }

    /**
     * 记录失败状态，保证下次重建不会因旧哈希而跳过。
     *
     * @param source 来源文档
     * @param hash   当前内容哈希
     */
    private void markFailed(RagSourceDocument source, String hash) {
        try {
            RagDocument existing = ragDocumentMapper.selectBySource(source.getSourceType(), source.getSourceId());
            RagDocument record = existing == null ? new RagDocument() : existing;
            record.setSourceType(source.getSourceType());
            record.setSourceId(source.getSourceId());
            record.setTitle(source.getTitle());
            record.setKnowledgePointId(source.getKnowledgePointId());
            record.setChunkCount(0);
            record.setVectorIds("[]");
            record.setContentHash(hash);
            record.setStatus("FAILED");
            if (existing == null) {
                ragDocumentMapper.insert(record);
            } else {
                ragDocumentMapper.update(record);
            }
        } catch (Exception e) {
            log.error("记录 RAG 失败状态时出错: sourceType={}, sourceId={}",
                    source.getSourceType(), source.getSourceId(), e);
        }
    }

    /**
     * 计算 UTF-8 内容的 SHA-256。
     *
     * @param content 文本内容
     * @return 64 位十六进制哈希
     */
    private String sha256(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(
                    (content == null ? "" : content).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("JDK 不支持 SHA-256", e);
        }
    }

    /**
     * 将向量 ID 列表序列化为 JSON 字符串。
     *
     * @param ids 向量 ID
     * @return JSON 数组字符串
     */
    private String toJson(List<String> ids) {
        try {
            return objectMapper.writeValueAsString(ids);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("序列化 vector_ids 失败", e);
        }
    }

    /**
     * 单篇文档的处理结果。
     *
     * @param indexed 是否实际写入
     * @param chunks  块数
     */
    private record IndexOutcome(boolean indexed, int chunks) {
    }
}

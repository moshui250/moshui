package com.codeagent.controller;

import com.codeagent.common.ErrorCode;
import com.codeagent.common.R;
import com.codeagent.exception.BizException;
import com.codeagent.vo.RagHitVO;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 临时 RAG 检索健康接口，用于验证入库结果与命中分数。
 */
@RestController
@RequestMapping("/api/health/rag")
@RequiredArgsConstructor
public class RagHealthController {

    private final ObjectProvider<VectorStore> vectorStoreProvider;

    /**
     * 检索与 query 最相近的 3 个块。
     *
     * @param query 检索问题
     * @return 标题、分数、来源类型与来源 ID
     */
    @GetMapping
    public R<List<RagHitVO>> search(@RequestParam("query") String query) {
        if (query == null || query.isBlank()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "query 不能为空");
        }
        VectorStore vectorStore = vectorStoreProvider.getIfAvailable();
        if (vectorStore == null) {
            throw new BizException(ErrorCode.AI_UNAVAILABLE, "未配置 ZILLIZ_URI，RAG 检索不可用");
        }
        try {
            List<Document> documents = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(query)
                            .topK(3)
                            .similarityThreshold(0.0)
                            .build());
            List<RagHitVO> hits = documents.stream().map(this::toHit).toList();
            return R.ok(hits);
        } catch (Exception e) {
            throw new BizException(ErrorCode.AI_UNAVAILABLE, "RAG 检索失败: " + e.getMessage());
        }
    }

    /**
     * 将 Spring AI Document 转成接口返回项。
     *
     * @param document 命中文档
     * @return 命中项
     */
    private RagHitVO toHit(Document document) {
        Object title = document.getMetadata().get("title");
        Object sourceType = document.getMetadata().get("source_type");
        Object sourceId = document.getMetadata().get("source_id");
        long id = sourceId instanceof Number number ? number.longValue() : 0L;
        return new RagHitVO(
                title == null ? "" : String.valueOf(title),
                document.getScore(),
                sourceType == null ? "" : String.valueOf(sourceType),
                id);
    }
}

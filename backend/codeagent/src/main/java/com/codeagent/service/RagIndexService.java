package com.codeagent.service;

import com.codeagent.ai.rag.RagRebuildResult;

/**
 * RAG 全量索引服务。
 */
public interface RagIndexService {

    /**
     * 重建全部 RAG 向量与登记表。
     *
     * @return 本次重建的文档数、跳过数、失败数与块数
     */
    RagRebuildResult rebuild();
}

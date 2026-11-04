package com.codeagent.service;

import com.codeagent.dto.AiExplainRequest;
import com.codeagent.vo.AiExplainVO;

/**
 * AI 讲解服务。
 */
public interface AiExplainService {

    /**
     * 基于 RAG 检索结果回答用户问题，并保存对话记录。
     *
     * @param userId  当前登录用户 ID
     * @param request 题目 ID 与用户问题
     * @return 回答、引用来源及降级标记
     */
    AiExplainVO explain(Long userId, AiExplainRequest request);
}

package com.codeagent.service;

import com.codeagent.vo.AgentSessionVO;

/**
 * AI 调试会话服务。
 */
public interface AgentSessionService {

    /**
     * 为指定题目创建或复用当前用户最近一次会话。
     *
     * @param userId     当前登录用户 ID
     * @param questionId 题目 ID
     * @return 会话 ID 和标题
     */
    AgentSessionVO createOrReuse(Long userId, Long questionId);
}

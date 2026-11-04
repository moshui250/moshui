package com.codeagent.service;

import com.codeagent.dto.AgentRunRequest;
import com.codeagent.vo.AgentRunVO;

/**
 * AI 调试 Agent 编排服务。
 */
public interface AgentService {

    /**
     * 执行一轮带工具调用能力的 AI 调试对话。
     *
     * @param userId  当前登录用户 ID
     * @param request 题目、问题、可选语言和代码
     * @return 回答、工具调用时间线、降级标记与会话 ID
     */
    AgentRunVO run(Long userId, AgentRunRequest request);
}

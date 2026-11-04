package com.codeagent.service;

import com.codeagent.dto.AgentStreamRequest;
import com.codeagent.vo.AgentHistoryVO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 流式 Agent 调试服务。
 */
public interface AgentStreamService {

    /**
     * 创建流式调试连接。meta 和会话消息在返回前完成初始化。
     *
     * @param userId  当前用户 ID
     * @param request 调试请求
     * @return SSE 连接
     */
    SseEmitter stream(Long userId, AgentStreamRequest request);

    /**
     * 查询某道题下的历史会话，供前端重建时间线。
     *
     * @param userId     当前用户 ID
     * @param questionId 题目 ID
     * @return 按会话分组的历史
     */
    AgentHistoryVO history(Long userId, Long questionId);
}

package com.codeagent.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AI 调试 Agent 的响应。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentRunVO {

    /** 最终回答。 */
    private String answer;

    /** 本轮工具调用时间线。 */
    private List<AgentToolCallVO> toolCalls;

    /** 是否走了关键词降级检索。 */
    private Boolean degraded;

    /** 本轮复用的对话 ID。 */
    private Long sessionId;
}

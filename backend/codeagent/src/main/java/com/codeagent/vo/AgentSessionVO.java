package com.codeagent.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * AI 调试会话标识。
 */
@Data
@AllArgsConstructor
public class AgentSessionVO {

    /** 会话 ID。 */
    private Long sessionId;

    /** 会话标题。 */
    private String title;
}

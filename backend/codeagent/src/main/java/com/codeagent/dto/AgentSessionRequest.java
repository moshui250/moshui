package com.codeagent.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建或复用 AI 调试会话的请求。
 */
@Data
public class AgentSessionRequest {

    /** 题目 ID。 */
    @NotNull(message = "题目 ID 不能为空")
    private Long questionId;
}

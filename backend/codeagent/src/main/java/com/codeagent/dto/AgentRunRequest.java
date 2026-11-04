package com.codeagent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * AI 调试 Agent 请求。
 */
@Data
public class AgentRunRequest {

    /** 题目 ID。 */
    @NotNull(message = "题目 ID 不能为空")
    private Long questionId;

    /** 学生问题。 */
    @NotBlank(message = "问题不能为空")
    @Size(max = 1000, message = "问题不能超过 1000 字")
    private String message;

    /** 当前语言，可选 CPP / JAVA。 */
    private String language;

    /** 当前代码，可选；用于定位错误。 */
    @Size(max = 100000, message = "代码过长")
    private String code;
}

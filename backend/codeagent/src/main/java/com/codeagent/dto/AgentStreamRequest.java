package com.codeagent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 流式 Agent 调试请求。
 */
@Data
public class AgentStreamRequest {

    /**
     * 当前题目 ID。
     */
    @NotNull(message = "题目ID不能为空")
    private Long questionId;

    /**
     * 用户本次问题。
     */
    @NotBlank(message = "问题不能为空")
    @Size(max = 1000, message = "问题不能超过 1000 字")
    private String message;

    /**
     * 提示模式：HINT（默认）或 SOLUTION。
     */
    @Pattern(regexp = "(?i)HINT|SOLUTION", message = "mode 只能是 HINT 或 SOLUTION")
    private String mode;

    /**
     * 当前语言 CPP / JAVA，可不传。
     */
    @Pattern(regexp = "(?i)CPP|JAVA", message = "语言只能是 CPP 或 JAVA")
    private String language;

    /**
     * 当前编辑器代码，可不传。
     */
    @Size(max = 100000, message = "代码过长")
    private String code;

    /**
     * 用户追加的自定义调试要求，可不传。
     */
    @Size(max = 4000, message = "自定义调试要求不能超过 4000 字")
    private String customInstruction;

    /**
     * 可选提交记录 ID，用于复用最近一次判题结果。
     */
    @Positive(message = "提交记录 ID 必须为正数")
    private Long submitId;

    /**
     * 可选会话 ID；不传则复用最近会话或新建。
     */
    @Positive(message = "会话 ID 必须为正数")
    private Long sessionId;
}

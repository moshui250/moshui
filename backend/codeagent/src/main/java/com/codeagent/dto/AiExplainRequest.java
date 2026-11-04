package com.codeagent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * AI 讲解请求。
 */
@Data
public class AiExplainRequest {

    /** 当前题目 ID。 */
    @NotNull(message = "questionId 不能为空")
    private Long questionId;

    /** 用户问题，最多 1000 字。 */
    @NotBlank(message = "message 不能为空")
    @Size(max = 1000, message = "message 最多 1000 字")
    private String message;
}

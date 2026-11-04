package com.codeagent.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 可选的参考代码。language 缺省按 CPP 处理。
 */
@Data
public class ReferenceSolutionRequest {

    private String language;

    @NotBlank(message = "参考代码内容不能为空")
    private String content;

    private String explanation;
}

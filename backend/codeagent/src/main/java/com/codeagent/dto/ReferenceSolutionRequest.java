package com.codeagent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 可选的参考代码。language 缺省按 CPP 处理。
 */
@Data
public class ReferenceSolutionRequest {

    @Size(max = 16, message = "语言标识不能超过 16 个字符")
    @Pattern(regexp = "(?i)CPP|JAVA", message = "语言只能是 CPP 或 JAVA")
    private String language;

    @NotBlank(message = "参考代码内容不能为空")
    @Size(max = 100000, message = "参考代码不能超过 100000 个字符")
    private String content;

    @Size(max = 2000, message = "参考代码说明不能超过 2000 个字符")
    private String explanation;
}

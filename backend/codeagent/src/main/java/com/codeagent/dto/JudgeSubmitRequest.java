package com.codeagent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 判题提交请求。
 */
@Data
public class JudgeSubmitRequest {

    /** 题目 ID。 */
    @NotNull(message = "题目 ID 不能为空")
    private Long questionId;

    /** 语言代码，支持 CPP / C++ / JAVA。 */
    @NotBlank(message = "语言不能为空")
    private String language;

    /** 完整源代码。 */
    @NotBlank(message = "代码不能为空")
    @Size(max = 100000, message = "代码长度不能超过 100000 个字符")
    private String code;

    /** 竞赛 ID；普通练习提交时为空。 */
    private Long contestId;
}

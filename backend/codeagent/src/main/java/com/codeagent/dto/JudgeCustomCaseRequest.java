package com.codeagent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户在答题页临时添加的样例，不落库，也不参与正式判题。
 */
@Data
public class JudgeCustomCaseRequest {

    /** 自定义输入。 */
    @NotBlank(message = "自定义样例输入不能为空")
    @Size(max = 5000, message = "自定义样例输入不能超过 5000 个字符")
    private String input;

    /** 用户期望的输出。 */
    @NotBlank(message = "自定义样例期望输出不能为空")
    @Size(max = 5000, message = "自定义样例期望输出不能超过 5000 个字符")
    private String expectedOutput;
}

package com.codeagent.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建/编辑个人题时提交的单条用例。
 * isSample 用 Object 接收，兼容 JSON 的 true/false 和 1/0 两种写法。
 */
@Data
public class TestCaseRequest {

    @NotBlank(message = "用例输入不能为空")
    @JsonAlias("input")
    private String inputData;

    @NotNull(message = "用例期望输出不能为空")
    @JsonAlias("output")
    private String expectedOutput;

    @JsonAlias("sample")
    private Object isSample;

    private Integer score;

    private Integer sortNo;

    /**
     * 判断这条用例是否为样例，兼容 true/false、1/0 和 "1"/"true"。
     *
     * @return true 表示样例用例；无法识别时按非样例处理
     */
    public boolean sampleFlag() {
        if (isSample instanceof Boolean value) {
            return value;
        }
        if (isSample instanceof Number value) {
            return value.intValue() == 1;
        }
        if (isSample instanceof String value) {
            return "1".equals(value) || "true".equalsIgnoreCase(value);
        }
        return false;
    }
}

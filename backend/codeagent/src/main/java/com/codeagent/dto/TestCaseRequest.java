package com.codeagent.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建/编辑个人题时提交的单条用例。
 * isSample 用 Object 接收，兼容 JSON 的 true/false 和 1/0 两种写法。
 */
@Data
public class TestCaseRequest {

    @NotBlank(message = "用例输入不能为空")
    @Size(max = 10000, message = "单条用例输入不能超过 10000 个字符")
    @JsonAlias("input")
    private String inputData;

    @NotNull(message = "用例期望输出不能为空")
    @Size(max = 10000, message = "单条用例输出不能超过 10000 个字符")
    @JsonAlias("output")
    private String expectedOutput;

    @JsonAlias("sample")
    private Object isSample;

    @Min(value = 0, message = "用例分值不能小于 0")
    @Max(value = 100, message = "用例分值不能大于 100")
    private Integer score;

    @Min(value = 1, message = "用例排序必须大于 0")
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

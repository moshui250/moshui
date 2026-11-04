package com.codeagent.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 用户确认/修改图片识别草稿后的入库请求。
 */
@Data
public class ImportParsedRequest {

    /**
     * 识别缓存 ID，必须属于当前用户且未过期。
     */
    @NotBlank(message = "识别结果不能为空")
    private String parseId;

    /**
     * 题目标题。
     */
    @NotBlank(message = "题目标题不能为空")
    @Size(max = 128, message = "题目标题不能超过 128 个字符")
    private String title;

    /**
     * Markdown 题面。
     */
    @NotBlank(message = "题目描述不能为空")
    private String description;

    /**
     * 输入格式。
     */
    private String inputDesc;

    /**
     * 输出格式。
     */
    private String outputDesc;

    /**
     * 难度 1/2/3。
     */
    @NotNull(message = "难度不能为空")
    @Min(value = 1, message = "难度只能是 1/2/3")
    @Max(value = 3, message = "难度只能是 1/2/3")
    private Integer difficulty;

    /**
     * 知识点编码。
     */
    @NotBlank(message = "知识点不能为空")
    private String knowledgePointCode;

    /**
     * 用户确认后的样例用例，至少一条。
     */
    @Valid
    @NotEmpty(message = "至少提交 1 条样例用例")
    private List<TestCaseRequest> samples;

    /**
     * 用户填写的隐藏用例；为空时由样例复制两条并提示人工确认。
     */
    @Valid
    @JsonAlias("hiddenCases")
    private List<TestCaseRequest> testCases;

    /**
     * 图片地址；为空时使用识别缓存中的地址。
     */
    private String sourceImageUrl;
}

package com.codeagent.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
    @Size(max = 64, message = "识别结果 ID 不合法")
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
    @Size(max = 20000, message = "题目描述不能超过 20000 个字符")
    private String description;

    /**
     * 输入格式。
     */
    @Size(max = 10000, message = "输入格式不能超过 10000 个字符")
    private String inputDesc;

    /**
     * 输出格式。
     */
    @Size(max = 10000, message = "输出格式不能超过 10000 个字符")
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
    @Size(max = 64, message = "知识点编码不能超过 64 个字符")
    private String knowledgePointCode;

    /**
     * 用户确认后的样例用例，至少一条。
     */
    @Valid
    @NotEmpty(message = "至少提交 1 条样例用例")
    @Size(max = 20, message = "样例数量不能超过 20 条")
    private List<TestCaseRequest> samples;

    /**
     * 用户填写的隐藏用例；为空时由样例复制两条并提示人工确认。
     */
    @Valid
    @Size(max = 20, message = "隐藏用例数量不能超过 20 条")
    @JsonAlias("hiddenCases")
    private List<TestCaseRequest> testCases;

    /**
     * 图片地址；为空时使用识别缓存中的地址。
     */
    @Size(max = 512, message = "图片地址不能超过 512 个字符")
    private String sourceImageUrl;

    /**
     * 可见性：PRIVATE 仅自己可见，SHARED 共享给其他用户；默认 PRIVATE。
     */
    @Pattern(regexp = "(?i)PRIVATE|SHARED", message = "可见性只能是 PRIVATE 或 SHARED")
    private String visibility;
}

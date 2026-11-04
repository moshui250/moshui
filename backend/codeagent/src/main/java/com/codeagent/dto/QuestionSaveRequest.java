package com.codeagent.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 手动创建/编辑个人题的请求。
 * cases 与 testCases 互为别名；samples 是只含样例的简化写法，两者会合并。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class QuestionSaveRequest {

    @NotBlank(message = "题目标题不能为空")
    @Size(max = 128, message = "题目标题不能超过 128 个字符")
    private String title;

    @NotBlank(message = "题目描述不能为空")
    private String description;

    private String inputDesc;

    private String outputDesc;

    @NotNull(message = "难度不能为空")
    @Min(value = 1, message = "难度只能是 1/2/3")
    @Max(value = 3, message = "难度只能是 1/2/3")
    private Integer difficulty;

    @NotNull(message = "知识点不能为空")
    private Long knowledgePointId;

    private JsonNode templateJson;

    @Valid
    @JsonAlias("cases")
    private List<TestCaseRequest> testCases;

    @Valid
    private List<TestCaseRequest> samples;

    /**
     * 兼容两种写法：对象 {language,content,explanation} 或直接传代码字符串。
     */
    private JsonNode referenceCode;

    @Valid
    private List<ReferenceSolutionRequest> solutions;

    private String sourceImageUrl;
}

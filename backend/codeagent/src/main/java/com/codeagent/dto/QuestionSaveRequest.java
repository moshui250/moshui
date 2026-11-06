package com.codeagent.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
    @Size(max = 20000, message = "题目描述不能超过 20000 个字符")
    private String description;

    @Size(max = 10000, message = "输入格式不能超过 10000 个字符")
    private String inputDesc;

    @Size(max = 10000, message = "输出格式不能超过 10000 个字符")
    private String outputDesc;

    @NotNull(message = "难度不能为空")
    @Min(value = 1, message = "难度只能是 1/2/3")
    @Max(value = 3, message = "难度只能是 1/2/3")
    private Integer difficulty;

    @NotNull(message = "知识点不能为空")
    private Long knowledgePointId;

    /**
     * 多知识点；存在时以第一个作为题目主知识点，其余写入关联表。
     */
    @Size(max = 8, message = "一题最多选择 8 个知识点")
    private List<Long> knowledgePointIds;

    private JsonNode templateJson;

    @Valid
    @Size(max = 20, message = "用例数量不能超过 20 条")
    @JsonAlias("cases")
    private List<TestCaseRequest> testCases;

    @Valid
    @Size(max = 20, message = "样例数量不能超过 20 条")
    private List<TestCaseRequest> samples;

    /**
     * 兼容两种写法：对象 {language,content,explanation} 或直接传代码字符串。
     */
    private JsonNode referenceCode;

    @Valid
    @Size(max = 10, message = "参考题解数量不能超过 10 条")
    private List<ReferenceSolutionRequest> solutions;

    @Size(max = 512, message = "图片地址不能超过 512 个字符")
    private String sourceImageUrl;

    /**
     * 可见性：PRIVATE 仅自己，SHARED 共享给其他用户，PUBLIC 公共题（仅管理员）。
     * 默认 PRIVATE。
     */
    @Pattern(regexp = "(?i)PRIVATE|SHARED|PUBLIC", message = "可见性只能是 PRIVATE、SHARED 或 PUBLIC")
    private String visibility;
}

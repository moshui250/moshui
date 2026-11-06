package com.codeagent.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理员创建竞赛的请求。
 * 既可以从题库挑选已有题目，也可以直接提交竞赛专属题目。
 */
@Data
public class AdminContestRequest {

    @NotBlank(message = "竞赛标题不能为空")
    @Size(max = 120, message = "竞赛标题不能超过 120 个字符")
    private String title;

    @Size(max = 2000, message = "竞赛说明不能超过 2000 个字符")
    private String description;

    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;

    /**
     * 从题库选择的已有题目 ID。
     */
    @Size(max = 26, message = "单场竞赛最多 26 道题")
    private List<Long> questionIds;

    /**
     * 竞赛专属题目，不会出现在公共题库。
     */
    @Valid
    @Size(max = 26, message = "单场竞赛最多 26 道题")
    private List<QuestionSaveRequest> newQuestions;
}

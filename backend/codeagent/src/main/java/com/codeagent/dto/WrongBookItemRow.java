package com.codeagent.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 错题列表的扁平查询结果，Service 再组装成嵌套的 WrongBookItemVO。
 */
@Data
public class WrongBookItemRow {

    /** 错题记录 ID，对应 user_wrong_question.id。 */
    private Long id;

    /** 题目 ID。 */
    private Long questionId;

    /** 题目标题。 */
    private String title;

    /** 难度 1/2/3。 */
    private Integer difficulty;

    /** 知识点 ID。 */
    private Long knowledgePointId;

    /** 知识点编码。 */
    private String knowledgePointCode;

    /** 知识点名称。 */
    private String knowledgePointName;

    /** 累计答错次数。 */
    private Integer wrongCount;

    /** 是否已掌握：1 已掌握，0 未掌握。 */
    private Integer mastered;

    /** 当前用户是否已经 AC 过该题：1 是，0 否。 */
    private Integer passed;

    /** 最近一次答错时间。 */
    private LocalDateTime lastWrongTime;

    /** 最近一次错误提交 ID。 */
    private Long lastSubmitId;

    /** 最近一次错误提交状态。 */
    private String lastSubmitStatus;

    /** 最近一次错误提交的代码片段，SQL 中截取前 200 个字符。 */
    private String lastSubmitCode;

    /** 最近一次错误提交的完整代码，用于错题展开后的代码块展示。 */
    private String lastSubmitCodeFull;

    /** 最近一次错误提交的语言：CPP / JAVA。 */
    private String lastSubmitLanguage;
}

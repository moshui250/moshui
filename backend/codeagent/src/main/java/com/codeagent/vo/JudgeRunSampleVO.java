package com.codeagent.vo;

import lombok.Data;

import java.util.List;

/**
 * 仅运行公开样例的判题结果，不产生提交记录。
 */
@Data
public class JudgeRunSampleVO {

    /** 本次样例运行的整体状态。 */
    private String status;

    /** 通过的样例数。 */
    private int passedCount;

    /** 运行的样例总数。 */
    private int totalCount;

    /** 所有样例中的最大运行耗时，单位毫秒。 */
    private long runTimeMs;

    /** 编译输出，仅编译失败时非空。 */
    private String compileOutput;

    /** 面向用户的错误摘要。 */
    private String errorMessage;

    /** 逐样例结果；仅包含公开样例。 */
    private List<JudgeCaseResultVO> caseResults;
}

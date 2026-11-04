package com.codeagent.vo;

import lombok.Data;

import java.util.List;

/**
 * 判题结果详情，对应前端轮询接口的 data。
 */
@Data
public class JudgeResultVO {

    /** 提交记录 ID。 */
    private Long submitId;

    /** 判题状态 PENDING/JUDGING/AC/WA/TLE/MLE/RE/CE/SE。 */
    private String status;

    /** 通过的用例数。 */
    private int passedCount;

    /** 用例总数。 */
    private int totalCount;

    /** 最大单用例耗时，单位毫秒。 */
    private long runTimeMs;

    /** 编译错误原文，非编译错误时为 null。 */
    private String compileOutput;

    /** 面向用户的错误摘要。 */
    private String errorMessage;

    /** 逐用例结果；隐藏用例通过时不含输入/期望/实际内容。 */
    private List<JudgeCaseResultVO> caseResults;

    /** 是否建议进入 AI 调试；非 AC 时为 true。 */
    private boolean agentSuggest;
}

package com.codeagent.dto;

import lombok.Data;

/**
 * 首页总览的聚合结果。
 */
@Data
public class HomeTotalsRow {

    /** 全站提交总数。 */
    private Long totalSubmits;

    /** 全站通过总数。 */
    private Long acSubmits;

    /** 公共题目数。 */
    private Long publicQuestions;

    /** 知识点数。 */
    private Long knowledgePoints;
}

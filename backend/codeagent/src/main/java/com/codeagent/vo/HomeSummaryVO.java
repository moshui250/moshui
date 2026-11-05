package com.codeagent.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 首页总览响应。
 */
@Data
public class HomeSummaryVO {

    /** 全站提交总数。 */
    private long totalSubmits;

    /** 全站通过总数。 */
    private long acSubmits;

    /** 全站通过率，0-1。 */
    private double passRate;

    /** 公共题目数。 */
    private long publicQuestions;

    /** 知识点数。 */
    private long knowledgePoints;

    /** 是否有提交数据。 */
    private boolean hasData;

    /** 本周七天的提交和通过数，周一至周日。 */
    private List<HomeDailyVO> weekly = new ArrayList<>();
}

package com.codeagent.dto;

import lombok.Data;

/**
 * 竞赛题单查询行。
 */
@Data
public class ContestProblemRow {

    private String label;

    private Long questionId;

    private String title;

    private Integer difficulty;

    private String knowledgePointName;

    private Integer score;

    private Integer sortNo;
}

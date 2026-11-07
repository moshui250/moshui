package com.codeagent.vo;

import lombok.Data;

/**
 * 竞赛题单项。
 */
@Data
public class ContestProblemVO {

    private String label;

    private Long questionId;

    private String title;

    private Integer difficulty;

    private String knowledgePoint;

    private Integer score;

    private Long submitCount;

    private Long acCount;
}

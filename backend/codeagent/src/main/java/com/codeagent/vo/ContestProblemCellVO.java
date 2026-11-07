package com.codeagent.vo;

import lombok.Data;

/**
 * 竞赛榜单中的单题格子。
 */
@Data
public class ContestProblemCellVO {

    private Long questionId;

    /**
     * AC / WA / UNSOLVED
     */
    private String status;

    private Integer attempts;

    /**
     * 从竞赛开始到首次通过的秒数；未通过为 null。
     */
    private Long acTimeSeconds;
}

package com.codeagent.entity;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 竞赛题单中的一道题。
 */
@Data
public class ContestProblem implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long contestId;

    private Long questionId;

    private String label;

    private Integer sortNo;

    private Integer score;

    private LocalDateTime createTime;
}

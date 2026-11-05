package com.codeagent.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 竞赛提交记录行，用于排名计算。
 */
@Data
public class ContestSubmitRow {

    private Long userId;

    private Long questionId;

    private String status;

    private LocalDateTime createTime;
}

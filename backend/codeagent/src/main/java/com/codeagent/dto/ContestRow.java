package com.codeagent.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 竞赛列表/详情查询行。
 */
@Data
public class ContestRow {

    private Long id;

    private String title;

    private String description;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String status;

    private Integer problemCount;

    private Integer participantCount;

    private Boolean registered;
}

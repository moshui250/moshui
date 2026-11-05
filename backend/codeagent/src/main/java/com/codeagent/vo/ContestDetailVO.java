package com.codeagent.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 竞赛详情。
 */
@Data
public class ContestDetailVO {

    private Long id;

    private String title;

    private String description;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String status;

    private LocalDateTime serverTime;

    private Integer problemCount;

    private Integer participantCount;

    private Boolean registered;

    private List<ContestProblemVO> problems;
}

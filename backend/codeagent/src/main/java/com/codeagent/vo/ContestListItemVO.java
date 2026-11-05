package com.codeagent.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 竞赛列表项。
 */
@Data
public class ContestListItemVO {

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

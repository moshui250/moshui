package com.codeagent.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 竞赛排名项。
 */
@Data
public class ContestRankVO {

    private Long rank;

    private Long userId;

    private String username;

    private String nickname;

    private Integer solvedCount;

    private Integer score;

    private Long penaltyMinutes;

    private LocalDateTime lastAcTime;

    private Boolean me;
}

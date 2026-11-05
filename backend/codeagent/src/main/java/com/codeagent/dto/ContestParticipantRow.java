package com.codeagent.dto;

import lombok.Data;

/**
 * 竞赛报名用户行，用于排名初始化。
 */
@Data
public class ContestParticipantRow {

    private Long userId;

    private String username;

    private String nickname;
}

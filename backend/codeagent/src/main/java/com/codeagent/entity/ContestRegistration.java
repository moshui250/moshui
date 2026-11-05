package com.codeagent.entity;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 竞赛报名记录。
 */
@Data
public class ContestRegistration implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long contestId;

    private Long userId;

    private LocalDateTime createTime;
}

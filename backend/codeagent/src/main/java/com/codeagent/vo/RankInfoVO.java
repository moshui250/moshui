package com.codeagent.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 当前用户的排名与段位信息。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RankInfoVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 全站名次，从 1 开始。 */
    private long rank;

    /** 参与排名的用户总数。 */
    private long totalUsers;

    /** 是否进入前 100 名。 */
    private boolean onBoard;

    /** 唯一 AC 题数。 */
    private long acCount;

    /** 当前段位。 */
    private TierVO tier;

    /** 当前段位进度。 */
    private TierProgressVO progress;

    /** 达到当前 AC 题数的时间。 */
    private LocalDateTime reachedAt;
}

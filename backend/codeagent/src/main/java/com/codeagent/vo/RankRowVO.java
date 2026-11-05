package com.codeagent.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 排行榜中的一行。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RankRowVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 名次。 */
    private long rank;

    /** 用户 ID。 */
    private Long userId;

    /** 用户名。 */
    private String username;

    /** 昵称。 */
    private String nickname;

    /** 唯一 AC 题数。 */
    private long acCount;

    /** 段位。 */
    private TierVO tier;

    /** 达到当前 AC 题数的时间。 */
    private LocalDateTime reachedAt;

    /** 是否为当前登录用户。 */
    private boolean me;
}

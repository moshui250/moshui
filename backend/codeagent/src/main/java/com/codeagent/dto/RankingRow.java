package com.codeagent.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 排行榜 SQL 的扁平行。
 */
@Data
public class RankingRow {

    /** 名次。 */
    private Long rankNo;

    /** 用户 ID。 */
    private Long userId;

    /** 用户名。 */
    private String username;

    /** 昵称。 */
    private String nickname;

    /** 唯一 AC 题数。 */
    private Long acCount;

    /** 达到当前 AC 题数的时间。 */
    private LocalDateTime reachedAt;

    /** 参与排名的用户总数。 */
    private Long totalUsers;
}

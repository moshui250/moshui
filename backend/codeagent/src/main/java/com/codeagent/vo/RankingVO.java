package com.codeagent.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 总刷题排行榜响应。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RankingVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 前 100 名。 */
    private List<RankRowVO> top;

    /** 当前用户排名；未登录时为 null。 */
    private RankInfoVO me;

    /** 参与排名的用户总数。 */
    private long totalUsers;

    /** 榜单生成时间。 */
    private LocalDateTime refreshedAt;

    /** 全部段位规则，供问号弹窗使用。 */
    private List<TierVO> tiers;
}

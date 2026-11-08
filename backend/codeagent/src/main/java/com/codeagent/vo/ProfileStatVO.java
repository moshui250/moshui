package com.codeagent.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 学情看板统计响应，口径见手册 §2.5。
 */
@Data
public class ProfileStatVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 全部提交数。 */
    private long totalSubmit;

    /** AC 提交数。 */
    private long acSubmit;

    /** 答案错误提交数。 */
    private long waSubmit;

    /** AC、WA 以外的提交数。 */
    private long otherSubmit;

    /** 正确率 = acSubmit / totalSubmit，保留 3 位小数；无提交时为 0。 */
    private double accuracy;

    /** 有过 AC 的不同题目数，统计全部题目。 */
    private long solvedCount;

    /** 累计 AC（公共题 + 图片录题），仅用于学习概览，不参与段位。 */
    private long totalAc;

    /** 未掌握的错题数。 */
    private long wrongCount;

    /** 5 个知识点的提交与错误率。 */
    private List<KnowledgeStatVO> knowledgeStats = new ArrayList<>();

    /** 最近 5 次提交。 */
    private List<RecentSubmitVO> recentSubmits = new ArrayList<>();

    /** 当前用户最近 7 天的提交与通过趋势。 */
    private List<HomeDailyVO> weekly = new ArrayList<>();

    /** 当前用户的全站排名与段位。 */
    private RankInfoVO rankInfo;

    /** 全部段位规则，供问号弹窗使用。 */
    private List<TierVO> tiers = new ArrayList<>();
}

package com.codeagent.service;

import com.codeagent.vo.RankInfoVO;
import com.codeagent.vo.RankingVO;
import com.codeagent.vo.TierVO;

import java.util.List;

/**
 * 总刷题排行榜与段位服务。
 */
public interface RankingService {

    /**
     * 获取前 100 名及当前用户排名。
     *
     * @param userId 当前用户 ID
     * @return 排行榜响应
     */
    RankingVO getRanking(Long userId);

    /**
     * 获取指定用户的排名与段位，用于个人中心。
     *
     * @param userId 用户 ID
     * @return 排名信息；用户不存在时返回 null
     */
    RankInfoVO getRankInfo(Long userId);

    /**
     * 获取全部段位规则。
     *
     * @return 10 个段位
     */
    List<TierVO> getTiers();
}

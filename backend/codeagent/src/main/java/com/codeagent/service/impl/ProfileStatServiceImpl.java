package com.codeagent.service.impl;

import com.codeagent.common.ErrorCode;
import com.codeagent.dto.ProfileStatRow;
import com.codeagent.exception.BizException;
import com.codeagent.mapper.ProfileStatMapper;
import com.codeagent.service.ProfileStatService;
import com.codeagent.service.RankingService;
import com.codeagent.vo.KnowledgeStatVO;
import com.codeagent.vo.ProfileStatVO;
import com.codeagent.vo.RecentSubmitVO;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 学情看板统计实现。
 *
 * <p>统计主体用一条 GROUP BY SQL 取回 5 个知识点，总提交/AC/已通过题数在内存中求和；
 * 错题数来自同一条 SQL 的标量子查询。最近 5 次提交是独立的一次轻量查询。</p>
 */
@Service
@RequiredArgsConstructor
public class ProfileStatServiceImpl implements ProfileStatService {

    private static final int RECENT_LIMIT = 5;

    private final ProfileStatMapper profileStatMapper;
    private final RankingService rankingService;

    /** 缓存键为用户 ID，30 秒过期；主动失效由提交/掌握状态变更触发。 */
    private final Cache<Long, ProfileStatVO> cache = Caffeine.newBuilder()
            .expireAfterWrite(30, TimeUnit.SECONDS)
            .maximumSize(10_000)
            .build();

    /**
     * 读取看板统计，优先使用 30 秒缓存。
     *
     * @param userId 当前用户 ID
     * @return 看板统计响应
     */
    @Override
    public ProfileStatVO get(Long userId) {
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return cache.get(userId, this::load);
    }

    /**
     * 清除指定用户的缓存，保证提交或标记掌握后数字立即正确。
     *
     * @param userId 当前用户 ID
     */
    @Override
    public void invalidate(Long userId) {
        if (userId != null) {
            cache.invalidate(userId);
        }
    }

    /**
     * 实际查询并组装看板统计。
     *
     * @param userId 当前用户 ID
     * @return 看板统计响应
     */
    private ProfileStatVO load(Long userId) {
        List<ProfileStatRow> rows = profileStatMapper.selectStats(userId);

        ProfileStatVO vo = new ProfileStatVO();
        long totalSubmit = 0L;
        long acSubmit = 0L;
        long waSubmit = 0L;
        long otherSubmit = 0L;
        long solvedCount = 0L;
        long wrongCount = 0L;
        List<KnowledgeStatVO> knowledgeStats = new ArrayList<>();

        if (rows != null && !rows.isEmpty()) {
            for (ProfileStatRow row : rows) {
                long submitCount = row.getSubmitCount() == null ? 0L : row.getSubmitCount();
                long acCount = row.getAcCount() == null ? 0L : row.getAcCount();
                long waCount = row.getWaCount() == null ? 0L : row.getWaCount();
                long otherCount = row.getOtherCount() == null ? 0L : row.getOtherCount();
                long solved = row.getSolvedCount() == null ? 0L : row.getSolvedCount();

                totalSubmit += submitCount;
                acSubmit += acCount;
                waSubmit += waCount;
                otherSubmit += otherCount;
                solvedCount += solved;
                wrongCount = row.getWrongCount() == null ? 0L : row.getWrongCount();

                KnowledgeStatVO item = new KnowledgeStatVO();
                item.setKnowledgePointId(row.getKnowledgePointId());
                item.setName(row.getKnowledgePointName());
                item.setSubmitCount(submitCount);
                item.setAcCount(acCount);
                item.setHasData(submitCount > 0);
                item.setErrorRate(submitCount == 0 ? 0.0 : round(1.0 - (double) acCount / submitCount, 4));
                knowledgeStats.add(item);
            }
        } else {
            wrongCount = profileStatMapper.countWrong(userId);
        }

        vo.setTotalSubmit(totalSubmit);
        vo.setAcSubmit(acSubmit);
        vo.setWaSubmit(waSubmit);
        vo.setOtherSubmit(otherSubmit);
        vo.setSolvedCount(solvedCount);
        vo.setTotalAc(solvedCount);
        vo.setWrongCount(wrongCount);
        vo.setAccuracy(totalSubmit == 0 ? 0.0 : round((double) acSubmit / totalSubmit, 3));
        vo.setKnowledgeStats(knowledgeStats);
        vo.setRankInfo(rankingService.getRankInfo(userId));
        vo.setTiers(rankingService.getTiers());

        List<RecentSubmitVO> recentSubmits = profileStatMapper.selectRecentSubmits(userId, RECENT_LIMIT);
        vo.setRecentSubmits(recentSubmits == null ? new ArrayList<>() : recentSubmits);
        return vo;
    }

    /**
     * 四舍五入到指定小数位。
     *
     * @param value  原始值
     * @param digits 小数位
     * @return 四舍五入后的值
     */
    private double round(double value, int digits) {
        double factor = Math.pow(10, digits);
        return Math.round(value * factor) / factor;
    }
}

package com.codeagent.service.impl;

import com.codeagent.common.ErrorCode;
import com.codeagent.common.RankTier;
import com.codeagent.dto.RankingRow;
import com.codeagent.exception.BizException;
import com.codeagent.mapper.RankingMapper;
import com.codeagent.service.RankingService;
import com.codeagent.vo.RankInfoVO;
import com.codeagent.vo.RankRowVO;
import com.codeagent.vo.RankingVO;
import com.codeagent.vo.TierProgressVO;
import com.codeagent.vo.TierVO;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 排行榜实现。前 100 名缓存 30 秒，个人名次按需查询。
 */
@Service
@RequiredArgsConstructor
public class RankingServiceImpl implements RankingService {

    private static final int TOP_LIMIT = 100;

    private final RankingMapper rankingMapper;

    /** 前 100 名缓存 30 秒，避免排名页频繁访问数据库。 */
    private final Cache<Integer, TopSnapshot> topCache = Caffeine.newBuilder()
            .expireAfterWrite(30, TimeUnit.SECONDS)
            .maximumSize(4)
            .build();

    @Override
    public RankingVO getRanking(Long userId) {
        TopSnapshot snapshot = topCache.get(TOP_LIMIT, key -> loadTop());
        List<RankRowVO> top = new ArrayList<>(snapshot.rows().size());
        RankInfoVO me = null;

        for (RankingRow row : snapshot.rows()) {
            RankRowVO item = toRow(row, userId);
            top.add(item);
            if (userId != null && Objects.equals(row.getUserId(), userId)) {
                me = toInfo(row, snapshot.totalUsers());
            }
        }
        if (me == null && userId != null) {
            me = getRankInfo(userId);
        }

        return RankingVO.builder()
                .top(top)
                .me(me)
                .totalUsers(snapshot.totalUsers())
                .refreshedAt(snapshot.refreshedAt())
                .tiers(getTiers())
                .build();
    }

    @Override
    public RankInfoVO getRankInfo(Long userId) {
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        RankingRow row = rankingMapper.selectUserRank(userId);
        return row == null ? null : toInfo(row, value(row.getTotalUsers()));
    }

    @Override
    public List<TierVO> getTiers() {
        List<TierVO> tiers = new ArrayList<>(RankTier.values().length);
        for (RankTier tier : RankTier.values()) {
            tiers.add(toVO(tier));
        }
        return tiers;
    }

    private TopSnapshot loadTop() {
        List<RankingRow> rows = rankingMapper.selectTop(TOP_LIMIT);
        long totalUsers = rows.isEmpty() ? 0L : value(rows.get(0).getTotalUsers());
        return new TopSnapshot(rows, totalUsers, LocalDateTime.now());
    }

    private RankRowVO toRow(RankingRow row, Long currentUserId) {
        long acCount = value(row.getAcCount());
        RankTier tier = RankTier.of(acCount);
        return RankRowVO.builder()
                .rank(value(row.getRankNo()))
                .userId(row.getUserId())
                .username(row.getUsername())
                .nickname(row.getNickname())
                .acCount(acCount)
                .tier(toVO(tier))
                .reachedAt(row.getReachedAt())
                .me(currentUserId != null && Objects.equals(currentUserId, row.getUserId()))
                .build();
    }

    private RankInfoVO toInfo(RankingRow row, long totalUsers) {
        long acCount = value(row.getAcCount());
        RankTier tier = RankTier.of(acCount);
        long rank = value(row.getRankNo());
        return RankInfoVO.builder()
                .rank(rank)
                .totalUsers(totalUsers)
                .onBoard(rank > 0 && rank <= TOP_LIMIT)
                .acCount(acCount)
                .tier(toVO(tier))
                .progress(toProgress(tier, acCount))
                .reachedAt(row.getReachedAt())
                .build();
    }

    private TierProgressVO toProgress(RankTier tier, long acCount) {
        RankTier next = tier.next();
        Long target = next == null ? null : next.getMinAc();
        double percent = target == null ? 1.0 : Math.min(1.0, (double) acCount / target);
        return TierProgressVO.builder()
                .tier(toVO(tier))
                .currentAc(acCount)
                .targetAc(target)
                .nextTierName(next == null ? null : next.getLabel())
                .maxTier(next == null)
                .percent(percent)
                .build();
    }

    private TierVO toVO(RankTier tier) {
        return TierVO.builder()
                .code(tier.getCode())
                .name(tier.getLabel())
                .color(tier.getColor())
                .minAc(tier.getMinAc())
                .maxAc(tier.getMaxAc())
                .rangeText(tier.rangeText())
                .build();
    }

    private long value(Long value) {
        return value == null ? 0L : value;
    }

    private record TopSnapshot(List<RankingRow> rows, long totalUsers, LocalDateTime refreshedAt) {
    }
}

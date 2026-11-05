package com.codeagent.controller;

import com.codeagent.common.R;
import com.codeagent.common.UserContext;
import com.codeagent.service.RankingService;
import com.codeagent.vo.RankingVO;
import com.codeagent.vo.TierVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 总刷题排行榜接口。
 */
@RestController
@RequestMapping("/api/rank")
@RequiredArgsConstructor
public class RankingController {

    private final RankingService rankingService;

    /**
     * 查询前 100 名、当前用户排名与段位规则。
     *
     * @return 排行榜响应
     */
    @GetMapping
    public R<RankingVO> ranking() {
        return R.ok(rankingService.getRanking(UserContext.getUserId()));
    }

    /**
     * 查询全部段位规则，供前端问号弹窗使用。
     *
     * @return 10 个段位
     */
    @GetMapping("/tiers")
    public R<List<TierVO>> tiers() {
        return R.ok(rankingService.getTiers());
    }
}

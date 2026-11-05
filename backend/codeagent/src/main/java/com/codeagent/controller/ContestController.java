package com.codeagent.controller;

import com.codeagent.common.ErrorCode;
import com.codeagent.common.PageResult;
import com.codeagent.common.R;
import com.codeagent.common.UserContext;
import com.codeagent.exception.BizException;
import com.codeagent.service.ContestService;
import com.codeagent.vo.ContestDetailVO;
import com.codeagent.vo.ContestListItemVO;
import com.codeagent.vo.ContestRankVO;
import com.codeagent.vo.ContestRegisterVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 竞赛接口：列表、详情、报名、排名。
 */
@RestController
@RequestMapping("/api/contest")
@RequiredArgsConstructor
public class ContestController {

    private final ContestService contestService;

    /**
     * 分页查询竞赛列表。
     *
     * @param status 状态过滤 UPCOMING/RUNNING/ENDED，null 表示全部
     * @param page   页码
     * @param size   每页条数
     * @return 分页竞赛列表
     */
    @GetMapping
    public R<PageResult<ContestListItemVO>> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return R.ok(contestService.list(UserContext.getUserId(), status, page, size));
    }

    /**
     * 查询竞赛详情和题单。
     *
     * @param contestId 竞赛 ID
     * @return 竞赛详情
     */
    @GetMapping("/{contestId}")
    public R<ContestDetailVO> detail(@PathVariable Long contestId) {
        return R.ok(contestService.detail(contestId, UserContext.getUserId()));
    }

    /**
     * 报名竞赛；已报名时幂等。
     *
     * @param contestId 竞赛 ID
     * @return 报名状态和报名人数
     */
    @PostMapping("/{contestId}/register")
    public R<ContestRegisterVO> register(@PathVariable Long contestId) {
        return R.ok(contestService.register(contestId, requireUserId()));
    }

    /**
     * 分页查询竞赛排名。
     *
     * @param contestId 竞赛 ID
     * @param page      页码
     * @param size      每页条数
     * @return 分页排名
     */
    @GetMapping("/{contestId}/rank")
    public R<PageResult<ContestRankVO>> rank(
            @PathVariable Long contestId,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        return R.ok(contestService.rank(contestId, UserContext.getUserId(), page, size));
    }

    private Long requireUserId() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return userId;
    }
}

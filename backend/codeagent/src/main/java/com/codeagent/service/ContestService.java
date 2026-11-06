package com.codeagent.service;

import com.codeagent.common.PageResult;
import com.codeagent.dto.AdminContestRequest;
import com.codeagent.vo.ContestDetailVO;
import com.codeagent.vo.ContestListItemVO;
import com.codeagent.vo.ContestRankVO;
import com.codeagent.vo.ContestRegisterVO;

/**
 * 竞赛列表、详情、报名与排名。
 */
public interface ContestService {

    /**
     * 分页查询竞赛，公开可访问。
     *
     * @param userId 当前登录用户 ID，可为 null
     * @param status 状态过滤：UPCOMING/RUNNING/ENDED，null 表示全部
     * @param page   页码，从 1 开始
     * @param size   每页条数，限制 1~50
     * @return 分页竞赛列表
     */
    PageResult<ContestListItemVO> list(Long userId, String status, long page, long size);

    /**
     * 查询竞赛详情和题单，公开可访问。
     *
     * @param contestId 竞赛 ID
     * @param userId    当前登录用户 ID，可为 null
     * @return 竞赛详情
     */
    ContestDetailVO detail(Long contestId, Long userId);

    /**
     * 报名竞赛；已报名时幂等返回成功。
     *
     * @param contestId 竞赛 ID
     * @param userId    当前登录用户 ID
     * @return 报名状态和报名人数
     */
    ContestRegisterVO register(Long contestId, Long userId);

    /**
     * 分页查询竞赛排名。
     *
     * @param contestId 竞赛 ID
     * @param userId    当前登录用户 ID，可为 null
     * @param page      页码
     * @param size      每页条数
     * @return 分页排名
     */
    PageResult<ContestRankVO> rank(Long contestId, Long userId, long page, long size);

    /**
     * 校验用户能否提交竞赛题；非竞赛提交直接返回。
     *
     * @param contestId  竞赛 ID，可为 null
     * @param questionId 题目 ID
     * @param userId     当前登录用户 ID
     */
    void validateSubmission(Long contestId, Long questionId, Long userId);

    /**
     * 管理员创建竞赛，可混合已有题目与竞赛专属新题。
     *
     * @param request 竞赛信息、已有题目 ID 和竞赛专属新题
     * @return 新建竞赛 ID
     */
    Long createContest(AdminContestRequest request);
}

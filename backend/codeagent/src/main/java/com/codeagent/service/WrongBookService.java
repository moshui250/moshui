package com.codeagent.service;

import com.codeagent.common.PageResult;
import com.codeagent.vo.WrongBookItemVO;

/**
 * 错题本服务：写入、分页查询和掌握状态维护。
 */
public interface WrongBookService {

    /**
     * 记录一次未通过的提交：不存在则新增，存在则累加错误次数并刷新时间。
     * 调用方只应在非 AC 且非 SE 的判题终态调用；AC 不会自动标记已掌握。
     *
     * @param userId     用户 ID
     * @param questionId 题目 ID
     * @param submitId   最近一次未通过的提交 ID
     */
    void recordWrong(Long userId, Long questionId, Long submitId);

    /**
     * 分页查询当前用户的错题本，按最近答错时间倒序。
     *
     * @param page             页码，小于 1 时按 1 处理
     * @param size             每页条数，小于 1 用默认 10，超过 50 按 50
     * @param mastered         是否已掌握；null 或 false 查未掌握，true 查已掌握
     * @param knowledgePointId 知识点 ID，null 表示不限
     * @return 分页错题列表
     */
    PageResult<WrongBookItemVO> list(long page, long size, Boolean mastered, Long knowledgePointId);

    /**
     * 更新当前用户某道错题的掌握状态。
     * true 时写入 mastered_time，false 时清空 mastered_time。
     *
     * @param questionId 题目 ID
     * @param mastered   目标掌握状态
     */
    void updateMastered(Long questionId, boolean mastered);
}

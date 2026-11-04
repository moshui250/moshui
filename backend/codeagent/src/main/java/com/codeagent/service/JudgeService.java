package com.codeagent.service;

import com.codeagent.common.PageResult;
import com.codeagent.vo.JudgeHistoryVO;
import com.codeagent.vo.JudgeResultVO;

/**
 * 异步判题服务。
 */
public interface JudgeService {

    /**
     * 创建提交记录并投递到判题线程池。
     *
     * @param userId     当前登录用户 ID
     * @param questionId 题目 ID
     * @param language   语言代码 CPP / JAVA
     * @param code       完整源代码
     * @return 新建的提交记录 ID
     */
    Long submit(Long userId, Long questionId, String language, String code);

    /**
     * 查询某次提交的判题结果，只能查询自己的提交。
     *
     * @param submitId 提交记录 ID
     * @param userId   当前登录用户 ID
     * @return 判题结果，隐藏用例通过时不包含其输入/期望/实际内容
     */
    JudgeResultVO result(Long submitId, Long userId);

    /**
     * 分页查询当前用户的提交历史。
     *
     * @param userId     当前登录用户 ID
     * @param questionId 题目 ID，null 表示查询全部题目
     * @param page       页码，从 1 开始
     * @param size       每页条数，服务端限制为 1~50
     * @return 分页提交历史
     */
    PageResult<JudgeHistoryVO> history(Long userId, Long questionId, long page, long size);
}

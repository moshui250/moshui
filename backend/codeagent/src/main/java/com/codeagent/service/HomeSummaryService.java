package com.codeagent.service;

import com.codeagent.vo.HomeSummaryVO;

/**
 * 首页公开统计服务。
 */
public interface HomeSummaryService {

    /**
     * 获取首页总览：全站通过率、题目数、知识点数和本周七天统计。
     *
     * @return 首页总览响应
     */
    HomeSummaryVO getSummary();
}

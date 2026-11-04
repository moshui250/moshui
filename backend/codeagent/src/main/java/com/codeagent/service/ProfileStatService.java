package com.codeagent.service;

import com.codeagent.vo.ProfileStatVO;

/**
 * 学情看板统计服务，带 30 秒进程内缓存。
 */
public interface ProfileStatService {

    /**
     * 获取当前用户的看板统计；同一用户 30 秒内复用缓存。
     *
     * @param userId 当前用户 ID
     * @return 三项指标、知识点错误率和最近 5 次提交
     */
    ProfileStatVO get(Long userId);

    /**
     * 主动失效某用户的统计数据，用于提交完成或错题掌握状态变化后立即刷新。
     *
     * @param userId 当前用户 ID
     */
    void invalidate(Long userId);
}

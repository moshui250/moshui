package com.codeagent.service;

import com.codeagent.common.ErrorCode;
import com.codeagent.exception.BizException;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 单机版判题限流器：每个用户 60 秒内最多 10 次提交。
 */
@Component
public class JudgeRateLimiter {

    private static final int LIMIT = 10;
    private static final long WINDOW_MS = 60_000L;

    private final Map<Long, Deque<Long>> windows = new ConcurrentHashMap<>();

    /**
     * 检查并记录一次提交；超过窗口配额时抛 42900。
     *
     * @param userId 当前登录用户 ID，作为限流 key
     * @throws BizException 超过 10 次/分钟时抛出 42900
     */
    public void check(Long userId) {
        long now = System.currentTimeMillis();
        Deque<Long> window = windows.computeIfAbsent(userId, key -> new ArrayDeque<>());
        synchronized (window) {
            while (!window.isEmpty() && now - window.peekFirst() >= WINDOW_MS) {
                window.pollFirst();
            }
            if (window.size() >= LIMIT) {
                throw new BizException(ErrorCode.TOO_MANY_REQUESTS, "提交过于频繁，请稍后再试");
            }
            window.addLast(now);
        }
    }
}

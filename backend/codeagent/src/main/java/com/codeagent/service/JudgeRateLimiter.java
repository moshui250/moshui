package com.codeagent.service;

import com.codeagent.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 判题提交限流：每个用户 60 秒内最多 10 次。
 */
@Component
@RequiredArgsConstructor
public class JudgeRateLimiter {

    private static final int LIMIT = 10;
    private static final long WINDOW_MS = 60_000L;
    private static final String MESSAGE = "提交过于频繁，请稍后再试";

    private final RateLimitService rateLimitService;

    /**
     * 检查并记录一次提交；超过窗口配额时抛 42900。
     *
     * @param userId 当前登录用户 ID，作为限流 key
     * @throws BizException 超过 10 次/分钟时抛出 42900
     */
    public void check(Long userId) {
        rateLimitService.check("judge:submit:" + userId, LIMIT, WINDOW_MS, MESSAGE);
    }
}

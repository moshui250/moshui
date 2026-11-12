package com.codeagent.ai;

import com.codeagent.common.ErrorCode;
import com.codeagent.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * go-judge 熔断器：连续 3 次调用失败后打开 60 秒，期间直接拒绝提交，避免线程池被拖垮。
 */
@Slf4j
@Component
public class GoJudgeCircuitBreaker {

    private static final int FAILURE_THRESHOLD = 3;
    private static final long OPEN_MILLIS = 60_000L;

    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private final AtomicLong openUntil = new AtomicLong(0L);

    /**
     * 熔断窗口内直接拒绝；到期后进入半开探测。
     */
    public void ensureAvailable() {
        long until = openUntil.get();
        if (until == 0L) {
            return;
        }
        if (System.currentTimeMillis() < until) {
            throw new BizException(ErrorCode.JUDGE_UNAVAILABLE, "判题服务临时不可用，请稍后重试");
        }
        openUntil.compareAndSet(until, 0L);
        consecutiveFailures.set(0);
        log.info("go-judge 熔断窗口结束，恢复探测");
    }

    /**
     * 一次调用成功后清零失败计数。
     */
    public void recordSuccess() {
        consecutiveFailures.set(0);
        openUntil.set(0L);
    }

    /**
     * 一次调用失败后累计；达到阈值时打开 60 秒。
     */
    public void recordFailure() {
        int failures = consecutiveFailures.incrementAndGet();
        if (failures >= FAILURE_THRESHOLD) {
            openUntil.set(System.currentTimeMillis() + OPEN_MILLIS);
            consecutiveFailures.set(0);
            log.error("go-judge 连续 {} 次失败，熔断 {} 秒", FAILURE_THRESHOLD, OPEN_MILLIS / 1000);
        }
    }

    /**
     * 当前是否处于熔断窗口。
     *
     * @return 打开中返回 true
     */
    public boolean isOpen() {
        long until = openUntil.get();
        return until > 0L && System.currentTimeMillis() < until;
    }
}

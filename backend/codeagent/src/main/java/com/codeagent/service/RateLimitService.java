package com.codeagent.service;

import com.codeagent.common.ErrorCode;
import com.codeagent.exception.BizException;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 单机版滑动窗口限流器。
 * 使用 ConcurrentHashMap + 每个 key 独立窗口，不引入 Redis；定期清理长时间无请求的 key。
 */
@Component
public class RateLimitService {

    private static final long CLEANUP_INTERVAL_MS = 5 * 60_000L;
    private static final long IDLE_EXPIRE_MS = 60 * 60_000L;

    private final Map<String, Deque<Long>> windows = new ConcurrentHashMap<>();
    private final AtomicLong lastCleanup = new AtomicLong(0L);

    /**
     * 检查并记录一次请求。
     *
     * @param key      限流键，例如 "judge:submit:1"
     * @param limit    窗口内最大请求数
     * @param windowMs 窗口长度，单位毫秒
     * @param message  超限时返回给用户的提示
     * @throws BizException 超过窗口配额时抛出 42900
     */
    public void check(String key, int limit, long windowMs, String message) {
        long now = System.currentTimeMillis();
        cleanup(now);
        Deque<Long> window = windows.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        synchronized (window) {
            while (!window.isEmpty() && now - window.peekFirst() >= windowMs) {
                window.pollFirst();
            }
            if (window.size() >= limit) {
                throw new BizException(ErrorCode.TOO_MANY_REQUESTS, message);
            }
            window.addLast(now);
        }
    }

    /**
     * 定期删除空窗口和长时间未访问的 key，避免内存无限增长。
     *
     * @param now 当前时间戳
     */
    private void cleanup(long now) {
        long last = lastCleanup.get();
        if (now - last < CLEANUP_INTERVAL_MS || !lastCleanup.compareAndSet(last, now)) {
            return;
        }
        windows.entrySet().removeIf(entry -> {
            Deque<Long> window = entry.getValue();
            synchronized (window) {
                return window.isEmpty() || now - window.peekLast() > IDLE_EXPIRE_MS;
            }
        });
    }
}

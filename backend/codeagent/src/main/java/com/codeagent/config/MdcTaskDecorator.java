package com.codeagent.config;

import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

import java.util.Map;

/**
 * 把提交线程的 MDC（traceId）复制到异步任务线程，保证判题/Agent 日志也能串起来。
 */
public class MdcTaskDecorator implements TaskDecorator {

    /**
     * 包装原始任务，在异步线程执行前恢复 MDC，执行后清理。
     *
     * @param runnable 原始任务
     * @return 带 MDC 上下文的任务
     */
    @Override
    public Runnable decorate(Runnable runnable) {
        Map<String, String> context = MDC.getCopyOfContextMap();
        return () -> {
            if (context != null) {
                MDC.setContextMap(context);
            } else {
                MDC.clear();
            }
            try {
                runnable.run();
            } finally {
                MDC.clear();
            }
        };
    }
}

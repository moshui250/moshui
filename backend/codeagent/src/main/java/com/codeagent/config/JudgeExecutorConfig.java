package com.codeagent.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 判题线程池配置。判题任务会阻塞等待 Piston，因此和 Web 请求线程隔离。
 */
@Configuration
public class JudgeExecutorConfig {

    /**
     * 创建判题专用线程池：2 个常驻线程、最多 4 个线程、等待队列 50。
     *
     * @return 判题线程池，队列满时使用 AbortPolicy 抛出拒绝异常
     */
    @Bean(name = "judgeExecutor")
    public ThreadPoolTaskExecutor judgeExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("judge-");
        executor.setTaskDecorator(new MdcTaskDecorator());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}

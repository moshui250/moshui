package com.codeagent.ai;

import com.codeagent.config.GoJudgeProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 启动时探测 go-judge 版本，确认判题服务可达；不阻塞应用启动。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GoJudgeRuntimeInitializer implements ApplicationRunner {

    private final GoJudgeClient goJudgeClient;
    private final GoJudgeProperties properties;

    @Override
    public void run(ApplicationArguments args) {
        try {
            Map<String, Object> version = goJudgeClient.fetchVersion();
            log.info("go-judge 就绪: version={}, os={}, arch={}, path={}, javaHome={}",
                    version.get("buildVersion"), version.get("os"), version.get("platform"),
                    properties.getPath(), properties.getJavaHome());
        } catch (Exception e) {
            log.warn("go-judge 探测失败，判题功能可能不可用: {}", e.getMessage());
        }
    }
}

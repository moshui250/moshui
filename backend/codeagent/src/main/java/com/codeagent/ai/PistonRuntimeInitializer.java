package com.codeagent.ai;

import com.codeagent.config.PistonProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 启动时读取 Piston 运行时列表，打印真实版本并校准配置中的版本号。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PistonRuntimeInitializer implements ApplicationRunner {

    private final PistonClient pistonClient;
    private final PistonProperties properties;

    /**
     * 应用启动后查询 /runtimes。查询失败只记录警告，不阻止应用启动。
     *
     * @param args 启动参数，未使用
     */
    @Override
    public void run(ApplicationArguments args) {
        try {
            List<PistonRuntime> runtimes = pistonClient.fetchRuntimes();
            if (runtimes == null || runtimes.isEmpty()) {
                log.warn("Piston /runtimes 返回空列表，无法校准语言版本");
                return;
            }
            runtimes.forEach(runtime -> log.info("Piston runtime available: language={}, version={}, aliases={}",
                    runtime.language(), runtime.version(), runtime.aliases()));

            for (Map.Entry<String, PistonProperties.Language> entry : properties.getLanguages().entrySet()) {
                PistonProperties.Language configured = entry.getValue();
                PistonRuntime matched = findRuntime(runtimes, entry.getKey(), configured.getName());
                if (matched == null) {
                    log.warn("Piston 未提供配置语言 {}（{}）", entry.getKey(), configured.getName());
                    continue;
                }
                log.info("Piston 判题语言就绪: {} -> {} {}", entry.getKey(), matched.language(), matched.version());
                if (!Objects.equals(configured.getVersion(), matched.version())) {
                    log.warn("Piston 版本已变化: {} 配置={} 实际={}，已使用实际版本",
                            entry.getKey(), configured.getVersion(), matched.version());
                    configured.setVersion(matched.version());
                }
            }
        } catch (Exception e) {
            log.warn("Piston 运行时探测失败，判题功能可能不可用: {}", e.getMessage());
        }
    }

    /**
     * 按语言名或别名匹配一个运行时。
     *
     * @param runtimes   Piston 返回的运行时列表
     * @param key        配置键，例如 cpp、java
     * @param language   配置的 Piston 语言名，例如 c++、java
     * @return 匹配项；没有匹配时返回 null
     */
    private PistonRuntime findRuntime(List<PistonRuntime> runtimes, String key, String language) {
        return runtimes.stream()
                .filter(runtime -> equalsIgnoreCase(runtime.language(), language)
                        || equalsIgnoreCase(runtime.language(), key)
                        || (runtime.aliases() != null && runtime.aliases().stream()
                        .anyMatch(alias -> equalsIgnoreCase(alias, key) || equalsIgnoreCase(alias, language))))
                .findFirst()
                .orElse(null);
    }

    /**
     * 空安全的忽略大小写比较。
     *
     * @param left  左值
     * @param right 右值
     * @return 两者都非空且忽略大小写相等时返回 true
     */
    private boolean equalsIgnoreCase(String left, String right) {
        return left != null && right != null && left.equalsIgnoreCase(right);
    }
}

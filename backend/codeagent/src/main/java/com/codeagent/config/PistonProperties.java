package com.codeagent.config;

import com.codeagent.common.ErrorCode;
import com.codeagent.exception.BizException;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Piston 判题服务的配置项，版本号来自 /runtimes 接口，不在代码里猜测。
 */
@Data
@Component
@ConfigurationProperties(prefix = "piston")
public class PistonProperties {

    /** Piston 服务根地址，公共 API 为 https://emkc.org。 */
    private String baseUrl = "https://emkc.org";

    /** Piston v2 API 前缀；公共 API 为 /api/v2/piston，自托管为 /api/v2。 */
    private String apiPath = "/api/v2/piston";

    /** 建立连接超时，单位毫秒。 */
    private int connectTimeoutMs = 5000;

    /** 读取响应超时，单位毫秒；需要覆盖最慢的一次判题。 */
    private int readTimeoutMs = 35000;

    /** 单个用例的运行超时，单位毫秒。 */
    private int runTimeoutMs = 3000;

    /** 编译超时，单位毫秒。 */
    private int compileTimeoutMs = 10000;

    /** 编译和运行的内存上限，单位字节。 */
    private long memoryLimit = 268435456L;

    /** 语言键到 Piston 语言名、版本的映射，键为 cpp / java。 */
    private Map<String, Language> languages = new LinkedHashMap<>();

    /**
     * 按业务语言键取 Piston 语言配置。
     *
     * @param code 业务语言代码，支持 CPP / C++ / JAVA，大小写不敏感
     * @return 对应语言的 Piston 名称和版本配置
     * @throws BizException 语言不支持或版本未配置时抛出 50301
     */
    public Language requireLanguage(String code) {
        String key = code == null ? "" : code.trim().toLowerCase(Locale.ROOT);
        if ("c++".equals(key)) {
            key = "cpp";
        }
        Language language = languages.get(key);
        if (language == null || language.getName() == null || language.getName().isBlank()
                || language.getVersion() == null || language.getVersion().isBlank()) {
            throw new BizException(ErrorCode.JUDGE_UNAVAILABLE, "判题语言未配置或版本缺失");
        }
        return language;
    }

    /**
     * 单个语言的 Piston 名称与版本。
     */
    @Data
    public static class Language {

        /** Piston API 使用的语言名，例如 c++、java。 */
        private String name;

        /** Piston 运行时版本，例如 10.2.0、15.0.2。 */
        private String version;
    }
}

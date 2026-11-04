package com.codeagent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * DeepSeek 对话模型配置。
 * 密钥只从环境变量读取，不写入配置文件。
 */
@Data
@Component
@ConfigurationProperties(prefix = "deepseek")
public class DeepSeekProperties {

    /** OpenAI 兼容的 DeepSeek 服务地址。 */
    private String baseUrl = "https://api.deepseek.com";

    /** DeepSeek API Key，未配置时调用会返回 50302。 */
    private String apiKey;

    /** 对话模型名称。 */
    private String model = "deepseek-chat";

    /** 采样温度，讲解场景使用较低值。 */
    private double temperature = 0.2;

    /** 建立连接超时，单位毫秒。 */
    private int connectTimeoutMs = 5000;

    /** 读取响应超时，单位毫秒。 */
    private int readTimeoutMs = 60000;
}

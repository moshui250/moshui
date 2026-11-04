package com.codeagent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * DashScope OpenAI 兼容视觉接口配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "dashscope")
public class DashScopeProperties {

    /**
     * OpenAI 兼容模式基础地址。
     */
    private String baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";

    /**
     * DashScope API Key，只从环境变量读取。
     */
    private String apiKey;

    /**
     * 视觉模型名称。
     */
    private String model = "qwen-vl-max";

    /**
     * 主模型不可用（无权限/不存在）时的兼容模型。
     */
    private String fallbackModel = "qwen3-vl-plus";

    /**
     * 建立连接超时。
     */
    private int connectTimeoutMs = 5000;

    /**
     * 等待模型响应超时。
     */
    private int readTimeoutMs = 60000;
}

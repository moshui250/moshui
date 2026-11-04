package com.codeagent.ai.explain;

import com.codeagent.common.ErrorCode;
import com.codeagent.config.DeepSeekProperties;
import com.codeagent.exception.BizException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

/**
 * DeepSeek Chat Completions 客户端。
 * 使用 OpenAI 兼容协议，避免与 embedding 的 SiliconFlow 配置互相覆盖。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeepSeekClient {

    private final DeepSeekProperties properties;

    /**
     * 调用 DeepSeek 生成回答。
     *
     * @param systemPrompt 系统提示词，约束回答范围与格式
     * @param userPrompt   用户提示词，包含检索上下文和问题
     * @return 模型返回的文本；空白响应视为 AI 服务异常
     */
    public String chat(String systemPrompt, String userPrompt) {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new BizException(ErrorCode.AI_UNAVAILABLE, "未配置 DEEPSEEK_API_KEY");
        }

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.getConnectTimeoutMs());
        factory.setReadTimeout(properties.getReadTimeoutMs());
        RestClient client = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .requestFactory(factory)
                .build();

        Map<String, Object> body = Map.of(
                "model", properties.getModel(),
                "temperature", properties.getTemperature(),
                "stream", false,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)));

        try {
            DeepSeekResponse response = client.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .body(body)
                    .retrieve()
                    .body(DeepSeekResponse.class);

            if (response == null || response.choices() == null || response.choices().isEmpty()) {
                throw new BizException(ErrorCode.AI_UNAVAILABLE, "DeepSeek 返回为空");
            }
            String content = response.choices().get(0).message() == null
                    ? null : response.choices().get(0).message().content();
            if (content == null || content.isBlank()) {
                throw new BizException(ErrorCode.AI_UNAVAILABLE, "DeepSeek 返回内容为空");
            }
            return content.trim();
        } catch (BizException e) {
            throw e;
        } catch (RestClientResponseException e) {
            log.warn("DeepSeek HTTP 调用失败: status={}, body={}",
                    e.getStatusCode().value(), e.getResponseBodyAsString());
            throw new BizException(ErrorCode.AI_UNAVAILABLE, "AI 服务调用失败，请稍后重试");
        } catch (ResourceAccessException e) {
            log.warn("DeepSeek 网络调用失败: {}", e.getMessage());
            throw new BizException(ErrorCode.AI_UNAVAILABLE, "AI 服务超时，请稍后重试");
        }
    }

    /**
     * DeepSeek Chat Completions 响应体。
     *
     * @param choices 候选回答列表
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DeepSeekResponse(List<Choice> choices) {
    }

    /**
     * 单个候选回答。
     *
     * @param message 模型消息
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Choice(Message message) {
    }

    /**
     * 模型消息内容。
     *
     * @param content 文本回答
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Message(String content) {
    }
}

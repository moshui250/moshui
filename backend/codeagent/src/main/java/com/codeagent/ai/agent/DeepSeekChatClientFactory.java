package com.codeagent.ai.agent;

import com.codeagent.common.ErrorCode;
import com.codeagent.config.DeepSeekProperties;
import com.codeagent.exception.BizException;
import io.micrometer.observation.ObservationRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

/**
 * 为 Agent 单独创建 DeepSeek ChatClient。
 * 不复用 SiliconFlow 的 embedding 配置，避免 base-url 和 api-key 串用。
 */
@Component
@RequiredArgsConstructor
public class DeepSeekChatClientFactory {

    private final DeepSeekProperties properties;
    private final AgentTools agentTools;
    private final ToolCallLoggingAdvisor toolCallLoggingAdvisor;

    /**
     * 创建绑定四个工具和工具记录 Advisor 的 ChatClient。
     *
     * @return 可执行 Tool Calling 的 ChatClient
     */
    public ChatClient create() {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new BizException(ErrorCode.AI_UNAVAILABLE, "未配置 DEEPSEEK_API_KEY");
        }

        OpenAiApi api = OpenAiApi.builder()
                .baseUrl(properties.getBaseUrl())
                .apiKey(properties.getApiKey())
                .build();

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(properties.getModel())
                .temperature(properties.getTemperature())
                .parallelToolCalls(false)
                .build();

        OpenAiChatModel model = OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(options)
                .toolCallingManager(ToolCallingManager.builder().build())
                .retryTemplate(RetryTemplate.defaultInstance())
                .observationRegistry(ObservationRegistry.NOOP)
                .build();

        return ChatClient.builder(model)
                .defaultSystem(AgentPrompts.SYSTEM)
                .defaultTools(agentTools)
                .defaultAdvisors(toolCallLoggingAdvisor)
                .build();
    }
}

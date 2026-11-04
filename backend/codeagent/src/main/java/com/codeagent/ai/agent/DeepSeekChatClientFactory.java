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
        return ChatClient.builder(buildModel())
                .defaultSystem(AgentPrompts.SYSTEM)
                .defaultTools(agentTools)
                .defaultAdvisors(toolCallLoggingAdvisor)
                .build();
    }

    /**
     * 创建仅用于流式正文生成的 ChatClient，不注册任何工具。
     * 工具编排已由流式服务确定性执行，避免 stream() 再触发一次工具调用。
     *
     * @param systemPrompt 本轮系统提示词
     * @return 无工具 ChatClient
     */
    public ChatClient createForAnswer(String systemPrompt) {
        return ChatClient.builder(buildModel())
                .defaultSystem(systemPrompt)
                .build();
    }

    /**
     * 构建指向 DeepSeek 的底层 ChatModel。
     *
     * @return OpenAiChatModel
     */
    private OpenAiChatModel buildModel() {
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

        return OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(options)
                .toolCallingManager(ToolCallingManager.builder().build())
                .retryTemplate(RetryTemplate.defaultInstance())
                .observationRegistry(ObservationRegistry.NOOP)
                .build();
    }
}

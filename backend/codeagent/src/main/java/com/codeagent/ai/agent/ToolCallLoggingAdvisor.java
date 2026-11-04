package com.codeagent.ai.agent;

import com.codeagent.entity.ChatMessage;
import com.codeagent.mapper.ChatMessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.stereotype.Component;

/**
 * 记录本轮 Agent 的每次工具调用。
 * 工具方法只负责把调用记录写入上下文，本 Advisor 在模型返回后统一落 chat_message。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ToolCallLoggingAdvisor implements CallAdvisor {

    private final ChatMessageMapper chatMessageMapper;

    /**
     * 执行后续调用链，并在结束后把工具记录写入 chat_message（role=tool）。
     *
     * @param request 当前 ChatClient 请求
     * @param chain   后续 Advisor / 模型调用链
     * @return 模型响应
     */
    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        try {
            return chain.nextCall(request);
        } finally {
            persistToolMessages();
        }
    }

    /**
     * 逐条写入工具消息。写入失败只记录日志，不影响主回答。
     */
    private void persistToolMessages() {
        AgentRunContext context = AgentRunContext.current();
        if (context == null || context.getSessionId() == null
                || context.isToolMessagesLogged() || context.getToolCalls().isEmpty()) {
            return;
        }
        try {
            int seq = chatMessageMapper.selectMaxSeq(context.getSessionId());
            for (AgentRunContext.ToolCallRecord record : context.getToolCalls()) {
                log.info("Agent 工具调用: {} - {}", record.getName(), record.getSummary());
                ChatMessage message = new ChatMessage();
                message.setSessionId(context.getSessionId());
                message.setUserId(context.getUserId());
                message.setQuestionId(context.getQuestionId());
                message.setSeq(++seq);
                message.setRole("tool");
                message.setContent(record.getSummary() == null ? record.getName() : record.getSummary());
                message.setToolName(record.getName());
                message.setToolInput(record.getInput());
                message.setToolOutput(record.getOutput());
                message.setHintLevel(1);
                chatMessageMapper.insert(message);
            }
            context.markToolMessagesLogged();
        } catch (Exception e) {
            log.warn("工具调用记录落库失败: {}", e.getMessage());
        }
    }

    /**
     * Advisor 名称。
     *
     * @return 固定名称
     */
    @Override
    public String getName() {
        return "toolCallLoggingAdvisor";
    }

    /**
     * Advisor 顺序；放在默认链最前面，确保 finally 覆盖完整调用。
     *
     * @return 顺序值
     */
    @Override
    public int getOrder() {
        return 0;
    }
}

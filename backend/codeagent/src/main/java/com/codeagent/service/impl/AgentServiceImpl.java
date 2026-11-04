package com.codeagent.service.impl;

import com.codeagent.ai.agent.AgentRunContext;
import com.codeagent.ai.agent.DeepSeekChatClientFactory;
import com.codeagent.common.ErrorCode;
import com.codeagent.dto.AgentRunRequest;
import com.codeagent.dto.QuestionDetailRow;
import com.codeagent.entity.ChatMessage;
import com.codeagent.entity.ChatSession;
import com.codeagent.exception.BizException;
import com.codeagent.mapper.ChatMessageMapper;
import com.codeagent.mapper.ChatSessionMapper;
import com.codeagent.mapper.QuestionMapper;
import com.codeagent.service.AgentService;
import com.codeagent.vo.AgentRunVO;
import com.codeagent.vo.AgentToolCallVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

/**
 * AI 调试 Agent 的默认实现。
 * 负责会话、消息落库和 ChatClient 调用，工具执行由 AgentTools 完成。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentServiceImpl implements AgentService {

    private static final Pattern CODE_BLOCK = Pattern.compile("(?s)```.*?```");

    private final QuestionMapper questionMapper;
    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final DeepSeekChatClientFactory chatClientFactory;
    private final ObjectMapper objectMapper;

    /**
     * 执行一轮带工具调用能力的 AI 调试对话。
     *
     * @param userId  当前登录用户 ID
     * @param request 题目、问题、可选语言和代码
     * @return 回答、工具调用时间线、降级标记与会话 ID
     */
    @Override
    public AgentRunVO run(Long userId, AgentRunRequest request) {
        QuestionDetailRow question = questionMapper.selectDetailRow(request.getQuestionId());
        if (question == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "题目不存在");
        }
        if ("PERSONAL".equals(question.getType()) && !userId.equals(question.getUserId())) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权访问该资源");
        }

        ChatSession session = chatSessionMapper.selectLatest(userId, request.getQuestionId());
        if (session == null) {
            session = new ChatSession();
            session.setUserId(userId);
            session.setQuestionId(request.getQuestionId());
            session.setTitle("AI 调试 · " + question.getTitle());
            chatSessionMapper.insert(session);
        } else {
            chatSessionMapper.touch(session.getId());
        }

        int seq = chatMessageMapper.selectMaxSeq(session.getId()) + 1;
        saveMessage(session, userId, request.getQuestionId(), seq, "user",
                request.getMessage(), null, null, null);

        AgentRunContext context = AgentRunContext.begin(
                userId, request.getQuestionId(),
                normalizeLanguage(request.getLanguage()), request.getCode());
        context.setSessionId(session.getId());
        try {
            String prompt = buildPrompt(question, request);
            ChatClient client = chatClientFactory.create();
            String answer = client.prompt().user(prompt).call().content();
            answer = cleanAnswer(answer, context.getSourceTitles());

            List<AgentToolCallVO> toolCalls = context.getToolCalls().stream()
                    .map(record -> new AgentToolCallVO(
                            record.getName(),
                            record.getSummary(),
                            record.getLatencyMs(),
                            record.getOutput() != null && !record.getOutput().isBlank()))
                    .toList();

            int assistantSeq = chatMessageMapper.selectMaxSeq(session.getId()) + 1;
            saveMessage(session, userId, request.getQuestionId(), assistantSeq, "assistant",
                    answer, "agent", null, toolCallsJson(toolCalls));
            chatSessionMapper.touch(session.getId());

            return new AgentRunVO(answer, toolCalls, context.isDegraded(), session.getId());
        } finally {
            AgentRunContext.clear();
        }
    }

    /**
     * 把题目、代码和学生问题拼成 Agent 输入。
     *
     * @param question 题目信息
     * @param request  请求
     * @return 用户提示词
     */
    private String buildPrompt(QuestionDetailRow question, AgentRunRequest request) {
        String language = normalizeLanguage(request.getLanguage());
        String code = request.getCode() == null || request.getCode().isBlank()
                ? "（未提供代码）" : request.getCode();
        return """
                题目：%s
                知识点：%s
                当前语言：%s
                当前代码：
                ```%s
                %s
                ```
                学生问题：%s
                """.formatted(
                safe(question.getTitle()),
                safe(question.getKnowledgePointName()),
                language,
                language.toLowerCase(),
                code,
                safe(request.getMessage()));
    }

    /**
     * 保存一条对话消息。
     *
     * @param session    会话
     * @param userId     用户 ID
     * @param questionId 题目 ID
     * @param seq        会话内序号
     * @param role       角色 user / assistant / tool
     * @param content    消息内容
     * @param toolName   工具名，可为空
     * @param toolInput  工具入参，可为空
     * @param toolOutput 工具输出，可为空
     */
    private void saveMessage(ChatSession session, Long userId, Long questionId, int seq,
                             String role, String content, String toolName,
                             String toolInput, String toolOutput) {
        ChatMessage message = new ChatMessage();
        message.setSessionId(session.getId());
        message.setUserId(userId);
        message.setQuestionId(questionId);
        message.setSeq(seq);
        message.setRole(role);
        message.setContent(content);
        message.setToolName(toolName);
        message.setToolInput(toolInput);
        message.setToolOutput(toolOutput);
        message.setHintLevel(1);
        chatMessageMapper.insert(message);
    }

    /**
     * 去掉模型可能输出的完整代码块，控制 200 字以内，并强制带检索来源。
     *
     * @param answer       原始回答
     * @param sourceTitles 本轮检索命中标题
     * @return 清理后的回答
     */
    private String cleanAnswer(String answer, List<String> sourceTitles) {
        if (answer == null || answer.isBlank()) {
            return "知识库暂未覆盖，建议先运行样例，重点检查数组边界和循环条件。";
        }
        String cleaned = CODE_BLOCK.matcher(answer)
                .replaceAll("（代码已省略，请先按提示自行修改）")
                .trim();

        int sourceIndex = cleaned.lastIndexOf("📎");
        String body = (sourceIndex >= 0 ? cleaned.substring(0, sourceIndex) : cleaned).trim();
        String sourceLine = buildSourceLine(sourceTitles,
                sourceIndex >= 0 ? cleaned.substring(sourceIndex).trim() : "");

        int maxBody = sourceLine.isBlank() ? 200 : Math.max(40, 200 - sourceLine.length() - 2);
        if (body.length() > maxBody) {
            body = shorten(body, maxBody);
        }
        return sourceLine.isBlank() ? body : body + "\n\n" + sourceLine;
    }

    /**
     * 优先引用错误手册，其次引用检索到的前两个标题。
     *
     * @param titles     检索标题
     * @param modelLine  模型自己生成的来源行
     * @return 来源行；没有来源时返回空串
     */
    private String buildSourceLine(List<String> titles, String modelLine) {
        List<String> citations = new java.util.ArrayList<>();
        titles.stream().filter(title -> title.contains("越界")).findFirst().ifPresent(citations::add);
        for (String title : titles) {
            if (citations.size() >= 2) {
                break;
            }
            if (!citations.contains(title)) {
                citations.add(title);
            }
        }
        if (citations.isEmpty()) {
            if (modelLine.isBlank()) {
                return "";
            }
            return modelLine.length() <= 60 ? modelLine : modelLine.substring(0, 60);
        }
        String line = "📎 参考来源：" + String.join("、", citations);
        return line.length() <= 80 ? line : line.substring(0, 80);
    }

    /**
     * 在标点处截断，尽量不把一句话切在半截。
     *
     * @param text 原文
     * @param max  最大长度
     * @return 截断文本
     */
    private String shorten(String text, int max) {
        if (text.length() <= max) {
            return text;
        }
        int end = -1;
        for (int i = max - 1; i >= Math.max(0, max / 2); i--) {
            char c = text.charAt(i);
            if (c == '。' || c == '！' || c == '？' || c == '；' || c == '\n') {
                end = i + 1;
                break;
            }
        }
        if (end <= 0) {
            end = Math.max(1, max - 1);
        }
        return text.substring(0, end).trim() + "…";
    }

    /**
     * 统一语言代码。
     *
     * @param language 原始语言
     * @return CPP 或 JAVA
     */
    private String normalizeLanguage(String language) {
        String value = language == null ? "" : language.trim().toUpperCase();
        return "JAVA".equals(value) ? "JAVA" : "CPP";
    }

    /**
     * 序列化工具调用时间线，供消息表追溯。
     *
     * @param toolCalls 工具调用
     * @return JSON；失败返回 []
     */
    private String toolCallsJson(List<AgentToolCallVO> toolCalls) {
        try {
            return objectMapper.writeValueAsString(toolCalls);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    /**
     * 空值转空串。
     *
     * @param value 原值
     * @return 原值或空串
     */
    private String safe(String value) {
        return value == null ? "" : value;
    }
}

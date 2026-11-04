package com.codeagent.service.impl;

import com.codeagent.ai.agent.AgentRunContext;
import com.codeagent.ai.agent.AgentTools;
import com.codeagent.ai.agent.DeepSeekChatClientFactory;
import com.codeagent.common.ErrorCode;
import com.codeagent.dto.AgentStreamRequest;
import com.codeagent.entity.ChatMessage;
import com.codeagent.entity.ChatSession;
import com.codeagent.entity.KnowledgePoint;
import com.codeagent.entity.QuestionSolution;
import com.codeagent.exception.BizException;
import com.codeagent.mapper.ChatMessageMapper;
import com.codeagent.mapper.ChatSessionMapper;
import com.codeagent.mapper.KnowledgePointMapper;
import com.codeagent.mapper.QuestionMapper;
import com.codeagent.mapper.QuestionSolutionMapper;
import com.codeagent.service.AgentStreamService;
import com.codeagent.vo.AgentHistoryVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 流式 Agent 调试实现。
 * 工具编排在本类中确定性执行，模型只负责把工具结果组织成第一层提示。
 */
@Slf4j
@Service
public class AgentStreamServiceImpl implements AgentStreamService {

    private static final long SSE_TIMEOUT_MS = 180_000L;

    private static final int MAX_TOOL_EVENTS = 4;

    private static final int TOOL_SEQ_GAP = MAX_TOOL_EVENTS + 1;

    private static final Pattern CODE_BLOCK = Pattern.compile("(?s)```[^\\n]*\\n?(.*?)```");

    private static final String HINT_SYSTEM = """
            你是编程助教。只能依据 <context> 回答，context 没有的内容直接说“知识库暂未覆盖，建议先检查输入输出和边界条件”。
            不要编造 API，不要编造语法，不要输出完整代码。回答用中文，不超过 200 字。
            第一句必须指出具体行号和原因，格式如“第 7 行：循环上界写成 <=，导致数组下标越界”。
            末尾另起一行写“📎 参考来源：<title>”。
            """;

    private final QuestionMapper questionMapper;
    private final KnowledgePointMapper knowledgePointMapper;
    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final QuestionSolutionMapper questionSolutionMapper;
    private final AgentTools agentTools;
    private final DeepSeekChatClientFactory chatClientFactory;
    private final ObjectMapper objectMapper;
    private final ThreadPoolTaskExecutor streamExecutor;

    public AgentStreamServiceImpl(QuestionMapper questionMapper,
                                  KnowledgePointMapper knowledgePointMapper,
                                  ChatSessionMapper chatSessionMapper,
                                  ChatMessageMapper chatMessageMapper,
                                  QuestionSolutionMapper questionSolutionMapper,
                                  AgentTools agentTools,
                                  DeepSeekChatClientFactory chatClientFactory,
                                  ObjectMapper objectMapper,
                                  @Qualifier("agentStreamExecutor") ThreadPoolTaskExecutor streamExecutor) {
        this.questionMapper = questionMapper;
        this.knowledgePointMapper = knowledgePointMapper;
        this.chatSessionMapper = chatSessionMapper;
        this.chatMessageMapper = chatMessageMapper;
        this.questionSolutionMapper = questionSolutionMapper;
        this.agentTools = agentTools;
        this.chatClientFactory = chatClientFactory;
        this.objectMapper = objectMapper;
        this.streamExecutor = streamExecutor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SseEmitter stream(Long userId, AgentStreamRequest request) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        ScheduledExecutorService heartbeat = newHeartbeat();
        AtomicBoolean clientGone = new AtomicBoolean(false);
        Runnable stopHeartbeat = () -> heartbeat.shutdownNow();
        emitter.onCompletion(stopHeartbeat);
        emitter.onError(error -> stopHeartbeat.run());
        emitter.onTimeout(() -> {
            stopHeartbeat.run();
            emitter.complete();
        });

        try {
            var question = questionMapper.selectDetailRow(request.getQuestionId());
            if (question == null) {
                throw new BizException(ErrorCode.NOT_FOUND, "题目不存在");
            }
            if ("PERSONAL".equalsIgnoreCase(question.getType())
                    && !userId.equals(question.getUserId())) {
                throw new BizException(ErrorCode.FORBIDDEN, "无权访问该资源");
            }

            ChatSession session = resolveSession(userId, request);
            int userSeq = chatMessageMapper.selectMaxSeq(session.getId()) + 1;
            saveMessage(userId, request, session.getId(), userSeq, "user",
                    request.getMessage(), null, null, null, 1);
            ChatMessage assistant = saveMessage(userId, request, session.getId(),
                    userSeq + TOOL_SEQ_GAP, "assistant", "", null, null, null,
                    isSolution(request) ? 2 : 1);

            boolean metaSent = send(emitter, "meta", Map.of(
                    "sessionId", session.getId(),
                    "messageId", assistant.getId()));
            if (!metaSent) {
                stopHeartbeat.run();
                emitter.complete();
                return emitter;
            }

            heartbeat.scheduleAtFixedRate(() -> sendComment(emitter),
                    15, 15, TimeUnit.SECONDS);

            String language = normalizeLanguage(request.getLanguage());
            String code = request.getCode() == null ? "" : request.getCode();
            String knowledgePointCode = knowledgePointCode(question.getKnowledgePointId());

            streamExecutor.execute(() -> runStream(emitter, userId, request, question,
                    session, assistant, userSeq, language, code, knowledgePointCode,
                    heartbeat, clientGone));
        } catch (BizException e) {
            sendError(emitter, e.getErrorCode().getCode(), e.getMessage());
            stopHeartbeat.run();
            emitter.complete();
        } catch (Exception e) {
            log.warn("建立 Agent 流失败: {}", e.getMessage());
            sendError(emitter, ErrorCode.AI_UNAVAILABLE.getCode(), "Agent 服务暂时不可用");
            stopHeartbeat.run();
            emitter.complete();
        }
        return emitter;
    }

    /**
     * 后台执行工具编排和模型流式输出。
     */
    private void runStream(SseEmitter emitter,
                           Long userId,
                           AgentStreamRequest request,
                           com.codeagent.dto.QuestionDetailRow question,
                           ChatSession session,
                           ChatMessage assistant,
                           int userSeq,
                           String language,
                           String code,
                           String knowledgePointCode,
                           ScheduledExecutorService heartbeat,
                           AtomicBoolean clientGone) {
        AgentRunContext context = AgentRunContext.begin(userId, request.getQuestionId(), language, code);
        context.setSessionId(session.getId());
        AtomicInteger toolSeq = new AtomicInteger(userSeq);

        try {
            if (isSolution(request)) {
                streamSolution(emitter, request, assistant, language, context, clientGone);
                return;
            }

            send(emitter, "thinking", Map.of("delta", "正在定位错误并检索相关资料…"));

            context.setToolEventListener(new AgentRunContext.ToolEventListener() {
                @Override
                public void onToolCall(AgentRunContext.ToolCallRecord call) {
                    send(emitter, "tool_call", Map.of(
                            "id", call.getId(),
                            "name", call.getName(),
                            "args", parseJsonOrText(call.getInput())));
                }

                @Override
                public void onToolResult(AgentRunContext.ToolCallRecord call) {
                    send(emitter, "tool_result", Map.of(
                            "id", call.getId(),
                            "name", call.getName(),
                            "summary", call.getSummary() == null ? "" : call.getSummary(),
                            "latencyMs", call.getLatencyMs()));
                    int seq = toolSeq.incrementAndGet();
                    saveMessage(userId, request, session.getId(), seq, "tool",
                            call.getSummary(), call.getName(), call.getInput(),
                            call.getOutput(), 1);
                }
            });

            log.info("Agent 工具调用: locate_error");
            agentTools.locateError(request.getQuestionId(), code, null, request.getSubmitId());
            String query = request.getMessage() + " " + question.getTitle() + " "
                    + (question.getKnowledgePointName() == null ? "" : question.getKnowledgePointName())
                    + " 数组越界 边界条件";
            log.info("Agent 工具调用: retrieve_knowledge");
            agentTools.retrieveKnowledge(query, knowledgePointCode, 4);

            String prompt = buildHintPrompt(request, question, context);
            ChatClient client = chatClientFactory.createForAnswer(HINT_SYSTEM);
            StringBuilder raw = new StringBuilder();
            for (String chunk : client.prompt().user(prompt).stream().content().toIterable()) {
                if (chunk == null || chunk.isEmpty() || clientGone.get()) {
                    continue;
                }
                raw.append(chunk);
            }

            String answer = finalizeHint(raw.toString(), context);
            for (int i = 0; i < answer.length() && !clientGone.get(); i += 12) {
                String delta = answer.substring(i, Math.min(answer.length(), i + 12));
                if (!send(emitter, "delta", Map.of("delta", delta))) {
                    break;
                }
                sleepQuietly(10L);
            }
            chatMessageMapper.updateContent(assistant.getId(), answer);
            send(emitter, "done", Map.of(
                    "messageId", assistant.getId(),
                    "hintLevel", 1,
                    "canReveal", true));
        } catch (BizException e) {
            fail(emitter, assistant, e.getErrorCode().getCode(), e.getMessage());
        } catch (Exception e) {
            log.warn("Agent 流处理失败: {}", e.getMessage(), e);
            fail(emitter, assistant, ErrorCode.AI_UNAVAILABLE.getCode(), "Agent 调试失败，请稍后重试");
        } finally {
            heartbeat.shutdownNow();
            AgentRunContext.clear();
            emitter.complete();
        }
    }

    /**
     * SOLUTION 模式：直接读取数据库题解，不经过模型。
     */
    private void streamSolution(SseEmitter emitter, AgentStreamRequest request,
                                ChatMessage assistant, String language,
                                AgentRunContext context, AtomicBoolean clientGone) {
        send(emitter, "thinking", Map.of("delta", "正在读取参考代码…"));
        QuestionSolution solution = questionSolutionMapper.selectLatest(request.getQuestionId(), language);
        if (solution == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "暂无该语言的参考代码");
        }
        String explanation = solution.getExplanation() == null ? "" : solution.getExplanation().trim();
        String code = solution.getContent() == null ? "" : solution.getContent();
        String markdown = (explanation.isEmpty() ? "" : explanation + "\n\n")
                + "```" + language.toLowerCase() + "\n" + code + "\n```";
        for (int i = 0; i < markdown.length() && !clientGone.get(); i += 12) {
            String delta = markdown.substring(i, Math.min(markdown.length(), i + 12));
            if (!send(emitter, "delta", Map.of("delta", delta))) {
                break;
            }
            sleepQuietly(12L);
        }
        chatMessageMapper.updateContent(assistant.getId(), markdown);
        send(emitter, "done", Map.of(
                "messageId", assistant.getId(),
                "hintLevel", 2,
                "canReveal", false));
    }

    /**
     * 构造 HINT 模式的上下文提示词。
     */
    private String buildHintPrompt(AgentStreamRequest request,
                                   com.codeagent.dto.QuestionDetailRow question,
                                   AgentRunContext context) {
        StringBuilder toolContext = new StringBuilder();
        for (AgentRunContext.ToolCallRecord call : context.getToolCalls()) {
            toolContext.append("[").append(call.getName()).append("] ")
                    .append(call.getSummary() == null ? "" : call.getSummary())
                    .append("\n")
                    .append(truncate(call.getOutput(), 3000))
                    .append("\n---\n");
        }
        return "<context>\n" + toolContext + "</context>\n"
                + "题目标题：" + question.getTitle() + "\n"
                + "知识点：" + safe(question.getKnowledgePointName()) + "\n"
                + "用户问题：" + request.getMessage() + "\n"
                + "用户代码：\n" + truncate(request.getCode(), 4000) + "\n";
    }

    /**
     * 解析会话：显式传入且归属正确时复用，否则最近会话，最后新建。
     */
    private ChatSession resolveSession(Long userId, AgentStreamRequest request) {
        if (request.getSessionId() != null) {
            ChatSession owned = chatSessionMapper.selectOwned(
                    request.getSessionId(), userId, request.getQuestionId());
            if (owned != null) {
                return owned;
            }
        }
        ChatSession latest = chatSessionMapper.selectLatest(userId, request.getQuestionId());
        if (latest != null) {
            return latest;
        }
        ChatSession session = new ChatSession();
        session.setUserId(userId);
        session.setQuestionId(request.getQuestionId());
        session.setTitle(truncate(request.getMessage(), 40));
        chatSessionMapper.insert(session);
        return session;
    }

    /**
     * 写入一条消息并回填主键。
     */
    private ChatMessage saveMessage(Long userId, AgentStreamRequest request, Long sessionId,
                                    int seq, String role, String content, String toolName,
                                    String toolInput, String toolOutput, int hintLevel) {
        ChatMessage message = new ChatMessage();
        message.setSessionId(sessionId);
        message.setUserId(userId);
        message.setQuestionId(request.getQuestionId());
        message.setSeq(seq);
        message.setRole(role);
        message.setContent(content == null ? "" : content);
        message.setToolName(toolName);
        message.setToolInput(toolInput);
        message.setToolOutput(toolOutput);
        message.setHintLevel(hintLevel);
        chatMessageMapper.insert(message);
        chatSessionMapper.touch(sessionId);
        return message;
    }

    /**
     * 从知识点 ID 取编码。
     */
    private String knowledgePointCode(Long knowledgePointId) {
        if (knowledgePointId == null) {
            return null;
        }
        KnowledgePoint point = knowledgePointMapper.selectById(knowledgePointId);
        return point == null ? null : point.getCode();
    }

    /**
     * 只保留 HINT 模式允许的代码块；超过 3 行时替换为提示语。
     */
    private String sanitizeHint(String raw) {
        String text = raw == null ? "" : raw.trim();
        Matcher matcher = CODE_BLOCK.matcher(text);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String body = matcher.group(1) == null ? "" : matcher.group(1);
            long lines = body.lines().filter(line -> !line.isBlank()).count();
            String replacement = lines > 3
                    ? "先按提示自己改，需要代码请点『看参考代码』"
                    : matcher.group();
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);
        return buffer.toString().trim();
    }

    /**
     * 统一收口 HINT 回答：去代码块、补来源、限制总长度。
     */
    private String finalizeHint(String raw, AgentRunContext context) {
        String answer = sanitizeHint(raw);
        if (answer.isBlank()) {
            answer = "第 1 行：暂未定位到明确错误行，建议先检查输入边界和数组下标。";
        }
        String body = answer.replaceAll("(?m)^📎 参考来源：.*$", "").trim();
        String suffix = "\n📎 参考来源：" + sourceText(context);
        int bodyLimit = Math.max(80, 200 - suffix.length());
        if (body.length() > bodyLimit) {
            body = body.substring(0, bodyLimit - 1) + "…";
        }
        return body + suffix;
    }

    /**
     * 取来源标题，优先“越界”手册，再补前两个标题。
     */
    private String sourceText(AgentRunContext context) {
        List<String> titles = context.getSourceTitles();
        if (titles.isEmpty()) {
            return "知识库";
        }
        String priority = null;
        for (String title : titles) {
            if (title != null && title.contains("越界")) {
                priority = title;
                break;
            }
        }
        List<String> chosen = new ArrayList<>();
        if (priority != null) {
            chosen.add(priority);
        }
        for (String title : titles) {
            if (title == null || chosen.contains(title)) {
                continue;
            }
            chosen.add(title);
            if (chosen.size() >= 2) {
                break;
            }
        }
        return String.join("、", chosen);
    }

    /**
     * 读取历史并分组。
     */
    @Override
    public AgentHistoryVO history(Long userId, Long questionId) {
        AgentHistoryVO result = new AgentHistoryVO();
        Map<Long, AgentHistoryVO.Session> sessions = new LinkedHashMap<>();
        for (ChatSession session : chatSessionMapper.selectByUserAndQuestion(userId, questionId)) {
            AgentHistoryVO.Session item = new AgentHistoryVO.Session();
            item.setId(session.getId());
            item.setTitle(session.getTitle());
            item.setCreateTime(session.getCreateTime());
            item.setUpdateTime(session.getUpdateTime());
            sessions.put(session.getId(), item);
            result.getSessions().add(item);
        }
        for (ChatMessage message : chatMessageMapper.selectByUserAndQuestion(userId, questionId)) {
            AgentHistoryVO.Session session = sessions.get(message.getSessionId());
            if (session == null) {
                continue;
            }
            AgentHistoryVO.Message item = new AgentHistoryVO.Message();
            item.setId(message.getId());
            item.setRole(message.getRole());
            item.setContent(message.getContent());
            item.setToolName(message.getToolName());
            item.setToolInput(parseJson(message.getToolInput()));
            item.setToolOutput(parseJson(message.getToolOutput()));
            item.setHintLevel(message.getHintLevel());
            item.setCreateTime(message.getCreateTime());
            session.getMessages().add(item);
        }
        return result;
    }

    /**
     * 发送 SSE 事件；客户端断开后不再写。
     */
    private boolean send(SseEmitter emitter, String name, Object data) {
        try {
            synchronized (emitter) {
                emitter.send(SseEmitter.event()
                        .name(name)
                        .data(data, MediaType.APPLICATION_JSON));
            }
            return true;
        } catch (Exception e) {
            log.debug("SSE 发送失败 {}: {}", name, e.getMessage());
            return false;
        }
    }

    /**
     * 发送 SSE 注释作为心跳。
     */
    private void sendComment(SseEmitter emitter) {
        try {
            synchronized (emitter) {
                emitter.send(SseEmitter.event().comment("ping"));
            }
        } catch (Exception e) {
            // 客户端已断开，交给 onCompletion / onError 清理
        }
    }

    /**
     * 发送流建立阶段的 error 事件。
     */
    private void sendError(SseEmitter emitter, int code, String message) {
        send(emitter, "error", Map.of("code", code,
                "message", message == null ? "Agent 调试失败" : message));
    }

    /**
     * 发送 error 事件并落库失败说明。
     */
    private void fail(SseEmitter emitter, ChatMessage assistant, int code, String message) {
        try {
            chatMessageMapper.updateContent(assistant.getId(), "[生成失败] " + message);
        } catch (Exception ignored) {
            // 落库失败不应掩盖 SSE 错误
        }
        send(emitter, "error", Map.of("code", code,
                "message", message == null ? "Agent 调试失败" : message));
    }

    private boolean isSolution(AgentStreamRequest request) {
        return "SOLUTION".equalsIgnoreCase(request.getMode());
    }

    private String normalizeLanguage(String language) {
        if (language != null && "JAVA".equalsIgnoreCase(language)) {
            return "JAVA";
        }
        return "CPP";
    }

    private ScheduledExecutorService newHeartbeat() {
        return Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "agent-sse-heartbeat");
            thread.setDaemon(true);
            return thread;
        });
    }

    private Object parseJsonOrText(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        try {
            return objectMapper.readTree(value);
        } catch (Exception e) {
            return value;
        }
    }

    private JsonNode parseJson(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(value);
        } catch (Exception e) {
            return objectMapper.getNodeFactory().textNode(value);
        }
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

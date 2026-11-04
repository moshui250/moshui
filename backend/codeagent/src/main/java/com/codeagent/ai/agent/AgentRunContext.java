package com.codeagent.ai.agent;

import com.codeagent.common.ErrorCode;
import com.codeagent.exception.BizException;
import com.codeagent.vo.JudgeResultVO;

import java.util.ArrayList;
import java.util.List;

/**
 * 单次 Agent 请求的上下文。
 * 工具方法通过 ThreadLocal 读取当前请求的用户、题目、代码和调用预算。
 */
public final class AgentRunContext {

    private static final ThreadLocal<AgentRunContext> HOLDER = new ThreadLocal<>();

    private static final int MAX_TOOL_CALLS = 4;

    private static final long TIMEOUT_NANOS = 60_000_000_000L;

    private final Long userId;
    private final Long questionId;
    private final String language;
    private final String code;
    private final long deadlineNanos;
    private final List<ToolCallRecord> toolCalls = new ArrayList<>();
    private final List<String> sourceTitles = new ArrayList<>();

    private Long sessionId;
    private int toolCallCount;
    private int judgeCallCount;
    private Long lastSubmitId;
    private JudgeResultVO lastJudgeResult;
    private boolean degraded;
    private boolean toolMessagesLogged;
    private ToolCallRecord currentCall;

    private AgentRunContext(Long userId, Long questionId, String language, String code) {
        this.userId = userId;
        this.questionId = questionId;
        this.language = language;
        this.code = code;
        this.deadlineNanos = System.nanoTime() + TIMEOUT_NANOS;
    }

    /**
     * 创建并绑定当前线程的上下文。
     *
     * @param userId     当前登录用户 ID
     * @param questionId 当前题目 ID
     * @param language   当前语言 CPP / JAVA
     * @param code       用户当前代码，可为空
     * @return 新建上下文
     */
    public static AgentRunContext begin(Long userId, Long questionId, String language, String code) {
        AgentRunContext context = new AgentRunContext(userId, questionId, language, code);
        HOLDER.set(context);
        return context;
    }

    /**
     * 获取当前线程上下文。
     *
     * @return 上下文；工具被直接调用时可能为 null
     */
    public static AgentRunContext current() {
        return HOLDER.get();
    }

    /**
     * 解除当前线程绑定，避免线程池复用时串数据。
     */
    public static void clear() {
        HOLDER.remove();
    }

    /**
     * 开始一次工具调用。达到 4 次上限时返回 null，调用方应直接返回提示而不是继续执行。
     *
     * @param name  工具名
     * @param input 工具入参 JSON
     * @return 调用记录；超限返回 null
     */
    public ToolCallRecord beginTool(String name, String input) {
        if (System.nanoTime() > deadlineNanos) {
            throw new BizException(ErrorCode.AI_UNAVAILABLE, "工具执行超时（>60s），请稍后重试");
        }
        if (toolCallCount >= MAX_TOOL_CALLS) {
            return null;
        }
        toolCallCount++;
        currentCall = new ToolCallRecord(name, input);
        return currentCall;
    }

    /**
     * 结束一次工具调用并记录耗时。
     *
     * @param summary 面向时间线的摘要
     * @param output  工具输出 JSON
     */
    public void endTool(String summary, String output) {
        if (currentCall == null) {
            return;
        }
        currentCall.summary = summary;
        if (output != null) {
            currentCall.output = output;
        }
        currentCall.latencyMs = (System.nanoTime() - currentCall.startedNanos) / 1_000_000L;
        toolCalls.add(currentCall);
        currentCall = null;
    }

    /**
     * 检查工具执行是否已经超过 60 秒。
     */
    public void checkDeadline() {
        if (System.nanoTime() > deadlineNanos) {
            throw new BizException(ErrorCode.AI_UNAVAILABLE, "工具执行超时（>60s），请稍后重试");
        }
    }

    /**
     * 计算判题轮询的剩余时间，至少保留 1 秒给结果落库。
     *
     * @return 剩余毫秒数
     */
    public long remainingMillis() {
        return Math.max(0L, (deadlineNanos - System.nanoTime()) / 1_000_000L);
    }

    /**
     * 判断是否已经调用过判题工具。
     *
     * @return 已调用返回 true
     */
    public boolean hasJudged() {
        return judgeCallCount > 0 || lastSubmitId != null;
    }

    /**
     * 记录一次判题调用。
     *
     * @param submitId 提交记录 ID
     */
    public void markJudged(Long submitId) {
        judgeCallCount++;
        lastSubmitId = submitId;
    }

    /**
     * 判断工具调用次数是否已达到上限。
     *
     * @return 达到上限返回 true
     */
    public boolean toolLimitReached() {
        return toolCallCount >= MAX_TOOL_CALLS;
    }

    /**
     * 当前用户 ID。
     *
     * @return 用户 ID
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * 当前题目 ID。
     *
     * @return 题目 ID
     */
    public Long getQuestionId() {
        return questionId;
    }

    /**
     * 当前语言。
     *
     * @return CPP 或 JAVA
     */
    public String getLanguage() {
        return language;
    }

    /**
     * 当前代码。
     *
     * @return 用户代码
     */
    public String getCode() {
        return code;
    }

    /**
     * 当前会话 ID。
     *
     * @return 会话 ID
     */
    public Long getSessionId() {
        return sessionId;
    }

    /**
     * 绑定会话 ID。
     *
     * @param sessionId 会话 ID
     */
    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    /**
     * 已调用工具次数。
     *
     * @return 次数
     */
    public int getToolCallCount() {
        return toolCallCount;
    }

    /**
     * 已调用判题次数。
     *
     * @return 次数
     */
    public int getJudgeCallCount() {
        return judgeCallCount;
    }

    /**
     * 最近一次判题提交 ID。
     *
     * @return 提交 ID；没有则为 null
     */
    public Long getLastSubmitId() {
        return lastSubmitId;
    }

    /**
     * 记录最近一次判题提交 ID。
     *
     * @param lastSubmitId 提交 ID
     */
    public void setLastSubmitId(Long lastSubmitId) {
        this.lastSubmitId = lastSubmitId;
    }

    /**
     * 最近一次判题结果。
     *
     * @return 判题结果；没有则为 null
     */
    public JudgeResultVO getLastJudgeResult() {
        return lastJudgeResult;
    }

    /**
     * 缓存最近一次判题结果。
     *
     * @param lastJudgeResult 判题结果
     */
    public void setLastJudgeResult(JudgeResultVO lastJudgeResult) {
        this.lastJudgeResult = lastJudgeResult;
    }

    /**
     * 是否已降级到关键词检索。
     *
     * @return 降级返回 true
     */
    public boolean isDegraded() {
        return degraded;
    }

    /**
     * 标记本轮已降级。
     */
    public void markDegraded() {
        this.degraded = true;
    }

    /**
     * 工具消息是否已经落库。
     *
     * @return 已落库返回 true
     */
    public boolean isToolMessagesLogged() {
        return toolMessagesLogged;
    }

    /**
     * 标记工具消息已落库，避免 Advisor 重复写入。
     */
    public void markToolMessagesLogged() {
        this.toolMessagesLogged = true;
    }

    /**
     * 本轮工具调用记录。
     *
     * @return 调用记录
     */
    public List<ToolCallRecord> getToolCalls() {
        return toolCalls;
    }

    /**
     * 记录本次检索命中的标题，供最终回答强制带来源。
     *
     * @param titles 标题列表
     */
    public void addSourceTitles(List<String> titles) {
        for (String title : titles) {
            if (title != null && !title.isBlank() && !sourceTitles.contains(title)) {
                sourceTitles.add(title);
            }
        }
    }

    /**
     * 本轮检索命中的标题。
     *
     * @return 标题列表
     */
    public List<String> getSourceTitles() {
        return sourceTitles;
    }

    /**
     * 单次工具调用记录，供 Advisor 落库和时间线展示。
     */
    public static final class ToolCallRecord {
        private final String name;
        private final String input;
        private final long startedNanos = System.nanoTime();
        private String summary;
        private String output;
        private long latencyMs;

        private ToolCallRecord(String name, String input) {
            this.name = name;
            this.input = input;
        }

        public String getName() {
            return name;
        }

        public String getInput() {
            return input;
        }

        public String getSummary() {
            return summary;
        }

        public String getOutput() {
            return output;
        }

        public long getLatencyMs() {
            return latencyMs;
        }
    }
}

package com.codeagent.ai.agent;

import com.codeagent.ai.explain.DeepSeekClient;
import com.codeagent.common.ErrorCode;
import com.codeagent.exception.BizException;
import com.codeagent.service.JudgeService;
import com.codeagent.service.QuestionService;
import com.codeagent.vo.JudgeCaseResultVO;
import com.codeagent.vo.JudgeResultVO;
import com.codeagent.vo.QuestionRecommendVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Agent 的四个工具。
 * 每个方法都通过 {@link AgentRunContext} 共享同一轮的调用预算、最近判题结果和用户代码。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentTools {

    private static final Set<String> TERMINAL = Set.of(
            "AC", "WA", "TLE", "MLE", "RE", "CE", "SE");

    private static final Pattern ERROR_LINE = Pattern.compile("(?i)(?:line\\s+|:)(\\d+)");

    private static final Pattern DECLARATION = Pattern.compile(
            "\\b(?:int|long|double|float|boolean|char|String)\\s+([A-Za-z_]\\w*)\\s*;");

    private final JudgeService judgeService;
    private final QuestionService questionService;
    private final KnowledgeRetriever knowledgeRetriever;
    private final DeepSeekClient deepSeekClient;
    private final ObjectMapper objectMapper;

    /**
     * 调用判题服务验证当前代码。单轮最多一次，已有判题结果时直接复用。
     *
     * @param questionId 题目 ID
     * @param language   语言 CPP / JAVA
     * @param code       可选代码；不传时使用本轮上下文中的代码
     * @return 判题摘要 JSON
     */
    @Tool(name = "judge_code", description = "当用户修改了代码、需要真实判题结果时使用。每轮最多调用一次；已有本轮判题结果时必须复用，不要重复提交。")
    public String judgeCode(
            @ToolParam(description = "题目 ID") Long questionId,
            @ToolParam(description = "语言，CPP 或 JAVA") String language,
            @ToolParam(description = "可选，完整源代码；不传则使用本轮用户当前代码", required = false) String code) {
        AgentRunContext context = requireContext();
        AgentRunContext.ToolCallRecord call = context.beginTool("judge_code",
                json(Map.of("questionId", questionId == null ? 0L : questionId, "language", safe(language))));
        if (call == null) {
            return limitJson();
        }
        try {
            if (context.getLastJudgeResult() != null) {
                return finish(context, "复用本轮判题结果", judgeJson(context.getLastJudgeResult(), true));
            }
            if (context.getJudgeCallCount() > 0) {
                if (context.getLastSubmitId() != null) {
                    JudgeResultVO existing = judgeService.result(context.getLastSubmitId(), context.getUserId());
                    context.setLastJudgeResult(existing);
                    return finish(context, "复用本轮判题结果", judgeJson(existing, true));
                }
                return finish(context, "判题次数已用完", json(Map.of("error", "judge_code 每轮最多调用 1 次，请使用已有判题结果")));
            }

            Long targetQuestionId = resolveQuestionId(questionId, context);
            String targetLanguage = normalizeLanguage(language == null ? context.getLanguage() : language);
            String targetCode = code == null || code.isBlank() ? context.getCode() : code;
            if (targetCode == null || targetCode.isBlank()) {
                return finish(context, "缺少代码", json(Map.of("error", "缺少代码，无法判题")));
            }

            Long submitId = judgeService.submit(context.getUserId(), targetQuestionId,
                    targetLanguage, targetCode);
            context.markJudged(submitId);
            JudgeResultVO result = waitForResult(context, submitId);
            context.setLastJudgeResult(result);
            return finish(context, "判题完成", judgeJson(result, false));
        } catch (BizException e) {
            if (e.getErrorCode() == ErrorCode.AI_UNAVAILABLE) {
                throw e;
            }
            return finish(context, "判题失败", json(Map.of("error", safe(e.getMessage()))));
        } catch (Exception e) {
            log.warn("judge_code 执行失败: {}", e.getMessage());
            return finish(context, "判题失败", json(Map.of("error", "判题失败：" + safe(e.getMessage()))));
        } finally {
            context.endTool("judge_code 完成", null);
        }
    }

    /**
     * 检索知识点、题解和错误手册，供回答引用。
     *
     * @param query              检索问题
     * @param knowledgePointCode 知识点编码，可为空
     * @param topK               返回条数，默认 4，最大 8
     * @return 知识片段 JSON，包含 degraded 标记
     */
    @Tool(name = "retrieve_knowledge", description = "当需要解释语法、知识点、报错原因，或拿到判题失败用例后需要查资料时使用。回答中的来源标题必须来自这里。")
    public String retrieveKnowledge(
            @ToolParam(description = "检索问题，尽量具体") String query,
            @ToolParam(description = "知识点编码，例如 array/string；不知道时传空串", required = false) String knowledgePointCode,
            @ToolParam(description = "返回条数，默认 4，最大 8", required = false) Integer topK) {
        AgentRunContext context = requireContext();
        AgentRunContext.ToolCallRecord call = context.beginTool("retrieve_knowledge",
                json(Map.of("query", safe(query), "knowledgePointCode", safe(knowledgePointCode))));
        if (call == null) {
            return limitJson();
        }
        try {
            KnowledgeRetriever.RetrievalResult result = knowledgeRetriever.retrieve(
                    query, knowledgePointCode, topK == null ? 4 : topK);
            if (safe(query).contains("越界") && result.chunks().stream()
                    .noneMatch(chunk -> chunk.title() != null && chunk.title().contains("越界"))) {
                KnowledgeRetriever.RetrievalResult generic = knowledgeRetriever.retrieve(
                        query, null, Math.max(topK == null ? 4 : topK, 8));
                if (generic.chunks().stream()
                        .anyMatch(chunk -> chunk.title() != null && chunk.title().contains("越界"))) {
                    result = generic;
                }
            }
            if (result.degraded()) {
                context.markDegraded();
            }
            List<Map<String, Object>> hits = result.chunks().stream()
                    .map(chunk -> {
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("docId", chunk.docId());
                        item.put("title", chunk.title());
                        item.put("snippet", chunk.snippet());
                        item.put("sourceType", chunk.sourceType());
                        item.put("score", chunk.score());
                        return item;
                    })
                    .toList();
            String output = json(Map.of("degraded", result.degraded(), "hits", hits));
            context.addSourceTitles(result.chunks().stream()
                    .map(KnowledgeRetriever.RetrievedChunk::title)
                    .toList());
            call = null;
            context.endTool("检索到 " + hits.size() + " 条资料", output);
            return output;
        } finally {
            if (call != null) {
                context.endTool("retrieve_knowledge 完成", call.getOutput());
            }
        }
    }

    /**
     * 结合判题失败用例、错误文本和代码行做错误定位。
     *
     * @param questionId   题目 ID
     * @param code         用户代码
     * @param errorMessage 可选错误信息
     * @param submitId     可选提交记录 ID；不传则复用本轮判题结果
     * @return 可疑行和错误类别 JSON
     */
    @Tool(name = "locate_error", description = "当用户提交失败、编译报错或需要定位具体代码行时使用。优先使用已有判题结果；输出可疑行号、原因和类别。")
    public String locateError(
            @ToolParam(description = "题目 ID") Long questionId,
            @ToolParam(description = "用户完整代码") String code,
            @ToolParam(description = "可选错误信息或编译输出", required = false) String errorMessage,
            @ToolParam(description = "可选提交记录 ID；不传则复用本轮判题结果", required = false) Long submitId) {
        AgentRunContext context = requireContext();
        AgentRunContext.ToolCallRecord call = context.beginTool("locate_error",
                json(Map.of("questionId", questionId == null ? 0L : questionId,
                        "submitId", submitId == null ? 0L : submitId)));
        if (call == null) {
            return limitJson();
        }
        try {
            String source = code == null || code.isBlank() ? context.getCode() : code;
            if (source == null || source.isBlank()) {
                return json(Map.of("error", "缺少代码，无法定位错误"));
            }

            JudgeResultVO result = loadJudgeResult(context, submitId);
            String message = firstNonBlank(errorMessage,
                    result == null ? null : result.getCompileOutput(),
                    result == null ? null : result.getErrorMessage(),
                    firstFailedStderr(result));

            String category = categoryOf(result, message, source);
            List<Map<String, Object>> suspects = ruleSuspects(source, message, category);
            if (suspects.isEmpty() && context.remainingMillis() > 5_000) {
                suspects = llmSuspects(source, message);
            }
            if (suspects.isEmpty()) {
                suspects = List.of(Map.of("line", 1, "reason", "未能从错误信息中定位明确行号，请结合失败用例检查输入边界"));
            }

            Map<String, Object> output = new LinkedHashMap<>();
            output.put("suspectLines", suspects);
            output.put("category", category);
            output.put("firstFail", firstFail(result));
            String text = json(output);
            context.endTool("定位到 " + suspects.size() + " 个可疑行", text);
            call = null;
            return text;
        } catch (BizException e) {
            if (e.getErrorCode() == ErrorCode.AI_UNAVAILABLE) {
                throw e;
            }
            return finish(context, "判题失败", json(Map.of("error", safe(e.getMessage()))));
        } catch (Exception e) {
            log.warn("locate_error 执行失败: {}", e.getMessage());
            return json(Map.of("error", "错误定位失败：" + safe(e.getMessage())));
        } finally {
            if (call != null) {
                context.endTool("locate_error 完成", call.getOutput());
            }
        }
    }

    /**
     * 推荐同知识点或难度接近的题目。
     *
     * @param questionId 当前题目 ID
     * @param limit      推荐数量，默认 3
     * @return 推荐列表 JSON
     */
    @Tool(name = "recommend_similar", description = "当用户通过题目、需要继续练习同类题，或想换一道题时使用。")
    public String recommendSimilar(
            @ToolParam(description = "当前题目 ID") Long questionId,
            @ToolParam(description = "推荐数量，默认 3，最大 5", required = false) Integer limit) {
        AgentRunContext context = requireContext();
        AgentRunContext.ToolCallRecord call = context.beginTool("recommend_similar",
                json(Map.of("questionId", questionId == null ? 0 : questionId)));
        if (call == null) {
            return limitJson();
        }
        try {
            Long target = resolveQuestionId(questionId, context);
            List<QuestionRecommendVO> recommendations = questionService.recommend(
                    target, limit == null ? 3 : limit);
            List<Map<String, Object>> items = recommendations.stream()
                    .map(item -> {
                        Map<String, Object> map = new LinkedHashMap<>();
                        map.put("id", item.getId());
                        map.put("title", item.getTitle());
                        map.put("difficulty", item.getDifficulty());
                        map.put("knowledgePoint", item.getKnowledgePointName());
                        map.put("reason", item.getReason());
                        return map;
                    })
                    .toList();
            String text = json(items);
            context.endTool("推荐 " + items.size() + " 道题", text);
            call = null;
            return text;
        } finally {
            if (call != null) {
                context.endTool("recommend_similar 完成", call.getOutput());
            }
        }
    }

    /**
     * 轮询判题结果，直到终态或本轮工具预算耗尽。
     *
     * @param context  当前 Agent 上下文
     * @param submitId 提交记录 ID
     * @return 最后一次判题结果
     */
    private JudgeResultVO waitForResult(AgentRunContext context, Long submitId) {
        JudgeResultVO result = null;
        long maxWaitMillis = Math.min(50_000L, Math.max(1_000L, context.remainingMillis() - 1_000L));
        long deadline = System.currentTimeMillis() + maxWaitMillis;
        while (System.currentTimeMillis() < deadline) {
            result = judgeService.result(submitId, context.getUserId());
            if (result != null && TERMINAL.contains(result.getStatus())) {
                return result;
            }
            try {
                Thread.sleep(300L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return result == null ? new JudgeResultVO() : result;
    }

    /**
     * 读取判题结果：优先使用指定提交，其次使用本轮最近一次判题。
     *
     * @param context  当前上下文
     * @param submitId 可选提交 ID
     * @return 判题结果；没有则为 null
     */
    private JudgeResultVO loadJudgeResult(AgentRunContext context, Long submitId) {
        if (submitId != null) {
            JudgeResultVO result = judgeService.result(submitId, context.getUserId());
            context.setLastSubmitId(submitId);
            context.setLastJudgeResult(result);
            return result;
        }
        if (context.getLastJudgeResult() != null) {
            return context.getLastJudgeResult();
        }
        if (context.getLastSubmitId() != null) {
            JudgeResultVO result = judgeService.result(context.getLastSubmitId(), context.getUserId());
            context.setLastJudgeResult(result);
            return result;
        }
        return null;
    }

    /**
     * 用规则从错误文本和代码中提取可疑行。
     *
     * @param code     用户代码
     * @param message  错误信息
     * @param category 错误类别
     * @return 可疑行列表
     */
    private List<Map<String, Object>> ruleSuspects(String code, String message, String category) {
        String[] lines = code.split("\\R", -1);
        Map<Integer, String> suspects = new LinkedHashMap<>();

        int errorLine = errorLine(message);
        if (errorLine > 0 && errorLine <= lines.length) {
            suspects.put(errorLine, "错误信息指向第 " + errorLine + " 行");
        }

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            int lineNo = i + 1;
            if (line.isEmpty()) {
                continue;
            }
            if (line.contains("<=") && line.matches(".*\\[[^]]+].*")) {
                suspects.putIfAbsent(lineNo, "循环上界使用了 <=，数组下标最大只能到 n-1");
            }
            if (line.contains("null") && (line.contains(".") || line.contains("["))) {
                suspects.putIfAbsent(lineNo, "这里可能解引用 null，先判断对象是否为空");
            }
            if ((line.contains("/") || line.contains("%")) && !line.contains("//")
                    && category.equals("RE")) {
                suspects.putIfAbsent(lineNo, "除数可能为 0，运算前先判断分母");
            }
            if (line.matches(".*\\b(?:while|for)\\s*\\(.*<=.*")) {
                suspects.putIfAbsent(lineNo, "循环边界可能多走一次，检查是否应为 <");
            }
        }

        Set<String> uninitialized = new LinkedHashSet<>();
        for (String line : lines) {
            Matcher matcher = DECLARATION.matcher(line);
            while (matcher.find()) {
                uninitialized.add(matcher.group(1));
            }
        }
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            for (String variable : uninitialized) {
                if (line.matches(".*\\b" + Pattern.quote(variable) + "\\b.*")
                        && !line.matches(".*\\b(?:int|long|double|float|boolean|char|String)\\s+"
                        + Pattern.quote(variable) + "\\s*;.*")) {
                    suspects.putIfAbsent(i + 1, "变量 " + variable + " 可能未初始化就被使用");
                }
            }
        }

        return suspects.entrySet().stream()
                .limit(4)
                .map(entry -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("line", entry.getKey());
                    item.put("reason", entry.getValue());
                    return item;
                })
                .toList();
    }

    /**
     * 规则无法定位时，让 DeepSeek 在严格 JSON 格式下补充可疑行。
     *
     * @param code    用户代码
     * @param message 错误信息
     * @return 可疑行列表；调用或解析失败返回空列表
     */
    private List<Map<String, Object>> llmSuspects(String code, String message) {
        try {
            String prompt = "代码：\n" + code + "\n错误：\n" + safe(message)
                    + "\n只返回 JSON：{\"suspectLines\":[{\"line\":1,\"reason\":\"...\"}],\"category\":\"RE\"}";
            String raw = deepSeekClient.chat(
                    "你是代码错误定位器。只输出 JSON，不要解释，不要 Markdown 围栏。",
                    prompt);
            int start = raw.indexOf('{');
            int end = raw.lastIndexOf('}');
            if (start < 0 || end <= start) {
                return List.of();
            }
            JsonNode root = objectMapper.readTree(raw.substring(start, end + 1));
            JsonNode lines = root.path("suspectLines");
            if (!lines.isArray()) {
                return List.of();
            }
            List<Map<String, Object>> result = new ArrayList<>();
            for (JsonNode node : lines) {
                int line = node.path("line").asInt(0);
                String reason = node.path("reason").asText("");
                if (line > 0 && !reason.isBlank()) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("line", line);
                    item.put("reason", reason);
                    result.add(item);
                }
                if (result.size() >= 3) {
                    break;
                }
            }
            return result;
        } catch (Exception e) {
            log.warn("LLM 错误定位辅助失败: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * 推断错误类别。
     *
     * @param result  判题结果，可为 null
     * @param message 错误文本
     * @param code    用户代码
     * @return CE / TLE / RE / WA
     */
    private String categoryOf(JudgeResultVO result, String message, String code) {
        if (result != null && result.getStatus() != null) {
            String status = result.getStatus().toUpperCase(Locale.ROOT);
            if (Set.of("CE", "TLE", "RE", "WA").contains(status)) {
                return status;
            }
            if ("MLE".equals(status)) {
                return "RE";
            }
        }
        String text = safe(message).toLowerCase(Locale.ROOT);
        if (text.contains("compile") || text.contains("error:")) {
            return "CE";
        }
        if (text.contains("timeout") || text.contains("time limit")) {
            return "TLE";
        }
        if (text.contains("exception") || text.contains("indexoutofbounds")) {
            return "RE";
        }
        if (code.contains("<=") && code.matches("(?s).*\\[[^]]+].*")) {
            return "RE";
        }
        return "WA";
    }

    /**
     * 从错误文本中提取第一个行号。
     *
     * @param message 错误文本
     * @return 行号；没有返回 0
     */
    private int errorLine(String message) {
        if (message == null || message.isBlank()) {
            return 0;
        }
        Matcher matcher = ERROR_LINE.matcher(message);
        while (matcher.find()) {
            try {
                int line = Integer.parseInt(matcher.group(1));
                if (line > 0) {
                    return line;
                }
            } catch (NumberFormatException ignored) {
                // 忽略非行号数字
            }
        }
        return 0;
    }

    /**
     * 取第一个失败用例的 stderr。
     *
     * @param result 判题结果
     * @return stderr 或 null
     */
    private String firstFailedStderr(JudgeResultVO result) {
        JudgeCaseResultVO failed = firstFailed(result);
        return failed == null ? null : failed.getStderr();
    }

    /**
     * 取第一个失败用例。
     *
     * @param result 判题结果
     * @return 失败用例；没有返回 null
     */
    private JudgeCaseResultVO firstFailed(JudgeResultVO result) {
        if (result == null || result.getCaseResults() == null) {
            return null;
        }
        return result.getCaseResults().stream()
                .filter(item -> !item.isPassed())
                .findFirst()
                .orElse(null);
    }

    /**
     * 把判题结果转成工具输出。
     *
     * @param result 判题结果
     * @param cached 是否复用已有结果
     * @return JSON
     */
    private String judgeJson(JudgeResultVO result, boolean cached) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("submitId", result.getSubmitId());
        map.put("status", result.getStatus());
        map.put("passedCount", result.getPassedCount());
        map.put("totalCount", result.getTotalCount());
        map.put("runTimeMs", result.getRunTimeMs());
        map.put("cached", cached);
        if (result.getCompileOutput() != null) {
            map.put("compileOutput", result.getCompileOutput());
        }
        if (result.getErrorMessage() != null) {
            map.put("errorMessage", result.getErrorMessage());
        }
        map.put("firstFail", firstFail(result));
        return json(map);
    }

    /**
     * 取第一个失败用例的摘要。
     *
     * @param result 判题结果
     * @return 摘要 map 或 null
     */
    private Map<String, Object> firstFail(JudgeResultVO result) {
        JudgeCaseResultVO failed = firstFailed(result);
        if (failed == null) {
            return null;
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("index", failed.getIndex());
        map.put("sample", failed.isSample());
        map.put("input", failed.getInput());
        map.put("expected", failed.getExpected());
        map.put("actual", failed.getActual());
        map.put("stderr", failed.getStderr());
        return map;
    }

    /**
     * 结束工具调用并返回输出，保证成功路径一定记录到时间线。
     *
     * @param context 当前上下文
     * @param summary 时间线摘要
     * @param output  工具输出 JSON
     * @return 原样返回 output
     */
    private String finish(AgentRunContext context, String summary, String output) {
        context.endTool(summary, output);
        return output;
    }

    /**
     * 解析工具参数中的题目 ID；模型传空或 0 时回退到本轮题目。
     *
     * @param questionId 工具传入的题目 ID
     * @param context    当前上下文
     * @return 有效题目 ID
     */
    private Long resolveQuestionId(Long questionId, AgentRunContext context) {
        return questionId == null || questionId <= 0 ? context.getQuestionId() : questionId;
    }

    /**
     * 取当前 Agent 上下文，工具脱离编排调用时直接报错。
     *
     * @return 上下文
     */
    private AgentRunContext requireContext() {
        AgentRunContext context = AgentRunContext.current();
        if (context == null) {
            throw new BizException(ErrorCode.AI_UNAVAILABLE, "Agent 上下文不存在");
        }
        return context;
    }

    /**
     * 统一语言代码。
     *
     * @param language 原始语言
     * @return CPP 或 JAVA
     */
    private String normalizeLanguage(String language) {
        String value = safe(language).toUpperCase(Locale.ROOT);
        if ("C++".equals(value) || "CPP".equals(value)) {
            return "CPP";
        }
        if ("JAVA".equals(value)) {
            return "JAVA";
        }
        return value;
    }

    /**
     * 工具次数达到上限时的固定返回。
     *
     * @return JSON
     */
    private String limitJson() {
        return json(Map.of("error", "工具调用次数已达上限（4 次），请直接依据已有信息作答"));
    }

    /**
     * 安全序列化。
     *
     * @param value 对象
     * @return JSON；失败返回 {}
     */
    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    /**
     * 返回第一个非空字符串。
     *
     * @param values 候选值
     * @return 第一个非空值或空串
     */
    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
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


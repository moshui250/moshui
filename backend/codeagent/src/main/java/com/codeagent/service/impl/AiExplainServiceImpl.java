package com.codeagent.service.impl;

import com.codeagent.ai.explain.DeepSeekClient;
import com.codeagent.ai.rag.ErrorManual;
import com.codeagent.ai.rag.RagChunk;
import com.codeagent.ai.rag.RagChunker;
import com.codeagent.ai.rag.RagSourceDocument;
import com.codeagent.common.ErrorCode;
import com.codeagent.common.UserContext;
import com.codeagent.dto.AiExplainRequest;
import com.codeagent.dto.QuestionDetailRow;
import com.codeagent.entity.ChatMessage;
import com.codeagent.entity.ChatSession;
import com.codeagent.entity.KnowledgePoint;
import com.codeagent.entity.Question;
import com.codeagent.exception.BizException;
import com.codeagent.exception.LlmTimeoutException;
import com.codeagent.mapper.ChatMessageMapper;
import com.codeagent.mapper.ChatSessionMapper;
import com.codeagent.mapper.KnowledgePointMapper;
import com.codeagent.mapper.QuestionMapper;
import com.codeagent.service.AiExplainService;
import com.codeagent.vo.AiExplainSourceVO;
import com.codeagent.vo.AiExplainVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI 讲解实现：先查向量库，异常时回退到 MyBatis 关键词检索。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiExplainServiceImpl implements AiExplainService {

    private static final String PERSONAL = "PERSONAL";
    private static final double MIN_SCORE = 0.35;
    private static final int VECTOR_TOP_K = 8;
    private static final int MAX_CONTEXT = 4;
    private static final int MAX_CHUNK_CHARS = 700;
    private static final int MAX_ANSWER_CHARS = 200;
    private static final String SOURCE_MARK = "📎 参考来源：";
    private static final String NO_CONTEXT_ANSWER =
            "知识库暂未覆盖，建议先看题目的输入输出说明，或把问题问得更具体一些。";
    private static final String LLM_TIMEOUT_ANSWER =
            "AI 讲解暂时超时，先按这三步自查：1. 变量是否初始化；2. 数组下标是否越界；"
                    + "3. 循环边界是否正确。也可以点击下方来源查看对应手册。";
    private static final String SYSTEM_PROMPT = """
            你是编程助教。只能依据 <context> 回答；context 没有的内容，直接说"知识库暂未覆盖，建议…"。
            不要编造 API、不要编造语法。回答用中文，≤200 字，末尾另起一行写"📎 参考来源：<title>"。
            """;

    private final QuestionMapper questionMapper;
    private final KnowledgePointMapper knowledgePointMapper;
    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final ObjectProvider<VectorStore> vectorStoreProvider;
    private final DeepSeekClient deepSeekClient;
    private final RagChunker ragChunker;
    private final ObjectMapper objectMapper;

    /**
     * 基于题目和问题检索知识库并生成讲解。
     *
     * @param userId  当前登录用户 ID
     * @param request 题目 ID 与用户问题
     * @return 回答、来源和降级标记
     */
    @Override
    public AiExplainVO explain(Long userId, AiExplainRequest request) {
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        QuestionDetailRow question = questionMapper.selectDetailRow(request.getQuestionId());
        if (question == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "题目不存在");
        }
        if (PERSONAL.equalsIgnoreCase(question.getType())
                && !userId.equals(question.getUserId())) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }

        String query = buildQuery(request.getMessage(), question);
        boolean degraded = false;
        List<ContextItem> contextItems;
        VectorStore vectorStore = vectorStoreProvider.getIfAvailable();
        if (vectorStore == null) {
            degraded = true;
            contextItems = safeKeywordSearch(request.getMessage());
        } else {
            try {
                contextItems = vectorSearch(vectorStore, query, request.getMessage());
            } catch (Exception e) {
                log.warn("向量检索失败，降级为关键词检索: {}", e.getMessage());
                degraded = true;
                contextItems = safeKeywordSearch(request.getMessage());
            }
        }

        List<AiExplainSourceVO> sources = toSources(contextItems);
        String answer;
        if (contextItems.isEmpty()) {
            answer = NO_CONTEXT_ANSWER;
        } else {
            try {
                String raw = deepSeekClient.chat(SYSTEM_PROMPT, buildUserPrompt(contextItems, request.getMessage()));
                answer = ensureSourceAndLimit(raw, sources);
            } catch (LlmTimeoutException e) {
                log.warn("AI 讲解超时，返回预置排查话术: {}", e.getMessage());
                degraded = true;
                answer = ensureSourceAndLimit(LLM_TIMEOUT_ANSWER, sources);
            }
        }

        saveHistory(userId, question, request.getMessage(), answer, sources);
        return new AiExplainVO(answer, sources, degraded);
    }

    /**
     * 组装检索 query：用户问题 + 题目标题 + 知识点名称。
     *
     * @param message  用户问题
     * @param question 当前题目
     * @return 用于向量检索的 query
     */
    private String buildQuery(String message, QuestionDetailRow question) {
        String title = question.getTitle() == null ? "" : question.getTitle();
        String knowledge = question.getKnowledgePointName() == null ? "" : question.getKnowledgePointName();
        return message.trim() + " " + title + " " + knowledge;
    }

    /**
     * 向量检索并按分数、问题相关性和条数过滤。
     *
     * @param vectorStore 向量库
     * @param query       检索 query（问题 + 题目标题 + 知识点）
     * @param message     用户原始问题，用于排除仅因题目上下文而命中的无关结果
     * @return 最多 4 个上下文块
     */
    private List<ContextItem> vectorSearch(VectorStore vectorStore, String query, String message) {
        List<Document> documents = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .topK(VECTOR_TOP_K)
                        .similarityThreshold(0.0)
                        .build());
        if (documents == null || documents.isEmpty()) {
            return List.of();
        }
        List<ContextItem> items = new ArrayList<>();
        for (Document document : documents) {
            double score = document.getScore() == null ? 0.0 : document.getScore();
            if (score < MIN_SCORE) {
                continue;
            }
            Map<String, Object> metadata = document.getMetadata();
            String title = stringValue(metadata.get("title"), "知识库片段");
            String sourceType = stringValue(metadata.get("source_type"), "UNKNOWN");
            ContextItem item = new ContextItem(document.getText(), title, sourceType, score);
            if (!isRelevant(message, item)) {
                continue;
            }
            items.add(item);
            if (items.size() >= MAX_CONTEXT) {
                break;
            }
        }
        return items;
    }

    /**
     * 判断检索结果是否真的包含用户问题中的核心词，避免题目上下文把无关问题拉进 prompt。
     *
     * @param message 用户问题
     * @param item    检索结果
     * @return 命中核心词或问题过于宽泛时返回 true
     */
    private boolean isRelevant(String message, ContextItem item) {
        String keyword = normalizeKeyword(message);
        if (keyword.isBlank() || keyword.length() <= 2) {
            return true;
        }
        String haystack = (item.title() + "\n" + safe(item.content())).toLowerCase(Locale.ROOT);
        for (String term : queryTerms(keyword)) {
            if (haystack.contains(term.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 生成用于相关性判断的词项：完整关键词加 2-4 字片段。
     *
     * @param keyword 清洗后的关键词
     * @return 去重后的词项
     */
    private List<String> queryTerms(String keyword) {
        List<String> terms = new ArrayList<>();
        terms.add(keyword);
        int max = Math.min(4, keyword.length());
        for (int length = 2; length <= max; length++) {
            for (int start = 0; start + length <= keyword.length(); start++) {
                String term = keyword.substring(start, start + length);
                if (!terms.contains(term)) {
                    terms.add(term);
                }
            }
        }
        return terms;
    }

    /**
     * 向量库异常时的关键词检索，查询知识点、题目标题和内置错误手册。
     *
     * @param message 用户问题
     * @return 最多 4 个上下文块
     */
    private List<ContextItem> safeKeywordSearch(String message) {
        try {
            return keywordSearch(message);
        } catch (Exception e) {
            log.warn("关键词降级检索失败: {}", e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * 使用 MyBatis LIKE 查询知识点、题目，并匹配内置错误手册。
     *
     * @param message 用户问题
     * @return 最多 4 个上下文块
     */
    private List<ContextItem> keywordSearch(String message) {
        String keyword = normalizeKeyword(message);
        if (keyword.isBlank()) {
            return List.of();
        }
        String safeKeyword = keyword.replace("%", "").replace("_", "");
        if (safeKeyword.isBlank()) {
            return List.of();
        }

        List<ContextItem> items = new ArrayList<>();
        for (KnowledgePoint knowledgePoint : knowledgePointMapper.selectByNameLike(safeKeyword)) {
            items.add(new ContextItem(
                    "知识点：" + knowledgePoint.getName() + "\n" + safe(knowledgePoint.getDescription()),
                    knowledgePoint.getName(),
                    "KNOWLEDGE",
                    0.0));
        }
        for (Question question : questionMapper.selectByTitleLike(safeKeyword)) {
            items.add(new ContextItem(
                    "题目：" + question.getTitle() + "\n" + safe(question.getDescription()),
                    question.getTitle(),
                    "QUESTION",
                    0.0));
        }

        RagSourceDocument manual = RagSourceDocument.builder()
                .sourceType("ERROR_MANUAL")
                .sourceId(0L)
                .title("刷题常见错误手册")
                .knowledgePointCode("")
                .content(ErrorManual.TEXT)
                .build();
        for (RagChunk chunk : ragChunker.split(manual)) {
            if (chunk.title().contains(safeKeyword) || chunk.text().contains(safeKeyword)) {
                items.add(new ContextItem(chunk.text(), chunk.title(), "ERROR_MANUAL", 0.0));
            }
            if (items.size() >= MAX_CONTEXT) {
                break;
            }
        }
        log.info("关键词降级检索: keyword={}, items={}", keyword, items.size());
        return items.stream().limit(MAX_CONTEXT).toList();
    }

    /**
     * 清洗用户问题，去掉常见问句前缀和后缀，保留核心检索词。
     *
     * @param message 原始问题
     * @return 清洗后的关键词
     */
    private String normalizeKeyword(String message) {
        if (message == null) {
            return "";
        }
        String keyword = message.trim()
                .replaceAll("^(请问|请|帮我|我想问|什么是|怎么|如何|为什么|讲讲|解释一下)", "")
                .replaceAll("(怎么办|怎么写|怎么做|是什么|为什么)$", "")
                .replaceAll("[\\p{Punct}\\s，。！？；：、]+", "");
        return keyword.isBlank() ? message.trim() : keyword;
    }

    /**
     * 将上下文块转换为去重后的引用来源。
     *
     * @param items 上下文块
     * @return 按相关度排序的来源列表
     */
    private List<AiExplainSourceVO> toSources(List<ContextItem> items) {
        Map<String, AiExplainSourceVO> unique = new LinkedHashMap<>();
        for (ContextItem item : items) {
            String key = item.sourceType() + "|" + item.title();
            AiExplainSourceVO existing = unique.get(key);
            if (existing == null || item.score() > existing.getScore()) {
                unique.put(key, new AiExplainSourceVO(item.title(), item.score(), item.sourceType()));
            }
        }
        return unique.values().stream().limit(MAX_CONTEXT).toList();
    }

    /**
     * 拼接 user prompt。
     *
     * @param items   上下文块
     * @param message 用户问题
     * @return 包含 <context> 的提示词
     */
    private String buildUserPrompt(List<ContextItem> items, String message) {
        String context = items.stream()
                .map(item -> truncate(item.content(), MAX_CHUNK_CHARS))
                .collect(Collectors.joining("\n---\n"));
        return "<context>" + context + "</context>\n问题：" + message.trim();
    }

    /**
     * 确保回答包含来源行，并把正文限制在 200 字以内。
     *
     * @param raw     模型原始回答
     * @param sources 引用来源
     * @return 规范化后的回答
     */
    private String ensureSourceAndLimit(String raw, List<AiExplainSourceVO> sources) {
        String body = raw == null ? "" : raw.trim();
        int markIndex = body.indexOf(SOURCE_MARK);
        if (markIndex >= 0) {
            body = body.substring(0, markIndex).trim();
        }
        String titles = sources.stream()
                .map(AiExplainSourceVO::getTitle)
                .filter(title -> title != null && !title.isBlank())
                .distinct()
                .limit(2)
                .collect(Collectors.joining("、"));
        String sourceLine = titles.isBlank() ? "" : SOURCE_MARK + titles;
        int bodyLimit = sourceLine.isBlank()
                ? MAX_ANSWER_CHARS
                : Math.max(40, MAX_ANSWER_CHARS - sourceLine.length() - 1);
        if (body.length() > bodyLimit) {
            body = body.substring(0, bodyLimit).trim() + "…";
        }
        if (body.isBlank()) {
            body = NO_CONTEXT_ANSWER;
        }
        return sourceLine.isBlank() ? body : body + "\n" + sourceLine;
    }

    /**
     * 保存会话和两条消息：用户问题与助手回答。
     *
     * @param userId   用户 ID
     * @param question 当前题目
     * @param message  用户问题
     * @param answer   助手回答
     * @param sources  引用来源
     */
    private void saveHistory(Long userId, QuestionDetailRow question, String message,
                             String answer, List<AiExplainSourceVO> sources) {
        ChatSession session = chatSessionMapper.selectLatest(userId, question.getId());
        if (session == null) {
            session = new ChatSession();
            session.setUserId(userId);
            session.setQuestionId(question.getId());
            session.setTitle(truncate(question.getTitle() + " · AI 讲解", 100));
            chatSessionMapper.insert(session);
        }
        int seq = chatMessageMapper.selectMaxSeq(session.getId());

        ChatMessage userMessage = new ChatMessage();
        userMessage.setSessionId(session.getId());
        userMessage.setUserId(userId);
        userMessage.setQuestionId(question.getId());
        userMessage.setSeq(seq + 1);
        userMessage.setRole("user");
        userMessage.setContent(message);
        userMessage.setHintLevel(1);
        chatMessageMapper.insert(userMessage);

        ChatMessage assistantMessage = new ChatMessage();
        assistantMessage.setSessionId(session.getId());
        assistantMessage.setUserId(userId);
        assistantMessage.setQuestionId(question.getId());
        assistantMessage.setSeq(seq + 2);
        assistantMessage.setRole("assistant");
        assistantMessage.setContent(answer);
        assistantMessage.setToolName("rag");
        assistantMessage.setToolOutput(toJson(sources));
        assistantMessage.setHintLevel(1);
        chatMessageMapper.insert(assistantMessage);
        chatSessionMapper.touch(session.getId());
    }

    /**
     * 把来源列表序列化为 JSON，持久化失败不影响主回答。
     *
     * @param sources 来源列表
     * @return JSON 字符串；序列化失败返回 null
     */
    private String toJson(List<AiExplainSourceVO> sources) {
        try {
            return objectMapper.writeValueAsString(sources);
        } catch (JsonProcessingException e) {
            log.warn("来源序列化失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 安全读取字符串。
     *
     * @param value        原值
     * @param defaultValue 默认值
     * @return 非空字符串
     */
    private String stringValue(Object value, String defaultValue) {
        return value == null ? defaultValue : String.valueOf(value);
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

    /**
     * 截断过长文本，避免 prompt 超长。
     *
     * @param value 原文本
     * @param limit 最大字符数
     * @return 截断后的文本
     */
    private String truncate(String value, int limit) {
        if (value == null) {
            return "";
        }
        return value.length() <= limit ? value : value.substring(0, limit);
    }

    /**
     * 单个检索上下文块。
     *
     * @param content    块正文
     * @param title      来源标题
     * @param sourceType 来源类型
     * @param score      相似度分数；关键词降级时为 0
     */
    private record ContextItem(String content, String title, String sourceType, double score) {
    }
}

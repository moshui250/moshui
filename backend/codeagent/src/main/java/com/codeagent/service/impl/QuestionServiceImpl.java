package com.codeagent.service.impl;

import com.codeagent.common.ErrorCode;
import com.codeagent.common.PageResult;
import com.codeagent.common.UserContext;
import com.codeagent.dto.QuestionDetailRow;
import com.codeagent.dto.QuestionSaveRequest;
import com.codeagent.dto.QuestionUserState;
import com.codeagent.dto.ReferenceSolutionRequest;
import com.codeagent.dto.TestCaseRequest;
import com.codeagent.entity.KnowledgePoint;
import com.codeagent.entity.Question;
import com.codeagent.entity.QuestionSolution;
import com.codeagent.entity.QuestionTestCase;
import com.codeagent.exception.BizException;
import com.codeagent.mapper.ContestMapper;
import com.codeagent.mapper.KnowledgePointMapper;
import com.codeagent.mapper.QuestionKnowledgePointMapper;
import com.codeagent.mapper.QuestionMapper;
import com.codeagent.mapper.QuestionSolutionMapper;
import com.codeagent.mapper.QuestionTestCaseMapper;
import com.codeagent.mapper.SubmitRecordMapper;
import com.codeagent.service.QuestionService;
import com.codeagent.vo.KnowledgePointBriefVO;
import com.codeagent.vo.QuestionDetailVO;
import com.codeagent.vo.QuestionListItemVO;
import com.codeagent.vo.QuestionRecommendVO;
import com.codeagent.vo.TestCaseSampleVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class QuestionServiceImpl implements QuestionService {

    private static final String PUBLIC = "PUBLIC";
    private static final String PERSONAL = "PERSONAL";
    private static final String PRIVATE = "PRIVATE";
    private static final String SHARED = "SHARED";
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;
    private static final int DEFAULT_SCORE = 25;

    private final QuestionMapper questionMapper;
    private final QuestionKnowledgePointMapper questionKnowledgePointMapper;
    private final QuestionTestCaseMapper testCaseMapper;
    private final QuestionSolutionMapper solutionMapper;
    private final KnowledgePointMapper knowledgePointMapper;
    private final SubmitRecordMapper submitRecordMapper;
    private final ContestMapper contestMapper;
    private final ObjectMapper objectMapper;

    /**
     * 分页查询题目列表。公开题所有人可见，个人题只查当前用户自己的。
     *
     * @param page             页码，小于 1 时按 1 处理
     * @param size             每页条数，小于 1 用默认 10，超过 50 按 50
     * @param knowledgePointId 知识点 ID，null 表示不限
     * @param difficulty       难度 1/2/3，null 表示不限
     * @param type             题目类型 PUBLIC/PERSONAL
     * @param keyword          标题关键字，null 或空串表示不限
     * @return 分页结果；无数据时返回空页
     */
    @Override
    public PageResult<QuestionListItemVO> list(long page, long size, Long knowledgePointId,
                                               Integer difficulty, String type, String keyword) {
        Long userId = requireUserId();
        long safePage = Math.max(page, 1L);
        long safeSize = size < 1 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);

        String normalizedType = type == null || type.isBlank()
                ? PUBLIC : type.trim().toUpperCase(Locale.ROOT);
        boolean personal;
        if (PUBLIC.equals(normalizedType)) {
            personal = false;
        } else if (PERSONAL.equals(normalizedType)) {
            personal = true;
        } else {
            throw new BizException(ErrorCode.PARAM_ERROR, "type 只能是 PUBLIC 或 PERSONAL");
        }
        if (difficulty != null && (difficulty < 1 || difficulty > 3)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "难度只能是 1/2/3");
        }
        String safeKeyword = keyword == null ? null : keyword.trim();
        if (safeKeyword != null && safeKeyword.isEmpty()) {
            safeKeyword = null;
        }

        long total = questionMapper.countList(personal, userId, knowledgePointId, difficulty, safeKeyword);
        if (total == 0) {
            return PageResult.empty(safePage, safeSize);
        }
        long offset = (safePage - 1L) * safeSize;
        if (offset < 0) {
            throw new BizException(ErrorCode.PARAM_ERROR, "页码过大");
        }
        List<QuestionListItemVO> records = questionMapper.selectList(
                personal, userId, knowledgePointId, difficulty, safeKeyword, offset, safeSize);
        return PageResult.of(records, total, safePage, safeSize);
    }

    /**
     * 查询题目详情。只查询 is_sample=1 的用例，隐藏用例不会进入响应。
     *
     * @param id 题目 ID
     * @return 题目详情，包含知识点、模板、样例和当前用户做题状态
     */
    @Override
    public QuestionDetailVO detail(Long id) {
        Long userId = requireUserId();
        QuestionDetailRow row = requireAccessible(id, userId);

        QuestionDetailVO vo = new QuestionDetailVO();
        BeanUtils.copyProperties(row, vo, "templateJson");
        vo.setTemplateJson(parseTemplate(row.getTemplateJson()));

        KnowledgePointBriefVO point = new KnowledgePointBriefVO();
        point.setId(row.getKnowledgePointId());
        point.setCode(row.getKnowledgePointCode());
        point.setName(row.getKnowledgePointName());
        vo.setKnowledgePoint(point);

        List<KnowledgePointBriefVO> points = questionKnowledgePointMapper.selectPointsByQuestionId(id);
        if (points == null || points.isEmpty()) {
            points = List.of(point);
        }
        vo.setKnowledgePoints(points);

        QuestionUserState state = submitRecordMapper.selectUserState(userId, id);
        vo.setLastSubmitId(state == null ? null : state.getLastSubmitId());
        vo.setSolved(state != null && Boolean.TRUE.equals(state.getSolved()));

        List<TestCaseSampleVO> samples = testCaseMapper.selectSamples(id);
        vo.setSamples(samples == null ? List.of() : samples);
        return vo;
    }

    /**
     * 创建个人题。题目、用例和参考代码在同一事务内写入。
     *
     * @param request 题目基本信息、用例和可选参考代码
     * @return 新建题目 ID
     */
    @Override
    @Transactional
    public Long create(QuestionSaveRequest request) {
        Long userId = requireUserId();
        List<Long> pointIds = resolveKnowledgePointIds(request, null);

        List<QuestionTestCase> cases = buildCases(request, true);
        List<QuestionSolution> solutions = buildSolutions(request);

        Question question = new Question();
        fillQuestion(question, request, userId);
        question.setKnowledgePointId(pointIds.get(0));
        questionMapper.insert(question);

        Long questionId = question.getId();
        syncKnowledgePoints(questionId, pointIds);
        attachQuestionId(cases, questionId);
        testCaseMapper.insertBatch(cases);
        if (!solutions.isEmpty()) {
            solutions.forEach(solution -> solution.setQuestionId(questionId));
            solutionMapper.insertBatch(solutions);
        }
        return questionId;
    }

    /**
     * 修改当前用户创建的个人题。用例/参考代码只在请求中显式提供时替换。
     *
     * @param id      题目 ID
     * @param request 新的题目信息
     */
    @Override
    @Transactional
    public void update(Long id, QuestionSaveRequest request) {
        Long userId = requireUserId();
        QuestionDetailRow existing = requireOwnedPersonal(id, userId);
        List<Long> pointIds = hasKnowledgePointSelection(request)
                ? resolveKnowledgePointIds(request, existing.getKnowledgePointId())
                : existingKnowledgePointIds(id, existing.getKnowledgePointId());

        boolean casesProvided = request.getTestCases() != null || request.getSamples() != null;
        boolean solutionsProvided = request.getSolutions() != null || request.getReferenceCode() != null;

        Question question = new Question();
        fillQuestion(question, request, userId);
        question.setKnowledgePointId(pointIds.get(0));
        if (request.getVisibility() == null || request.getVisibility().isBlank()) {
            question.setVisibility(existing.getVisibility());
        }
        question.setId(id);
        int updated = questionMapper.updateOwned(question);
        if (updated == 0) {
            throw new BizException(ErrorCode.NOT_FOUND, "题目不存在");
        }
        syncKnowledgePoints(id, pointIds);

        if (casesProvided) {
            List<QuestionTestCase> cases = buildCases(request, true);
            attachQuestionId(cases, id);
            testCaseMapper.deleteByQuestionId(id);
            testCaseMapper.insertBatch(cases);
        }
        if (solutionsProvided) {
            List<QuestionSolution> solutions = buildSolutions(request);
            if (!solutions.isEmpty()) {
                solutions.forEach(solution -> solution.setQuestionId(id));
                solutionMapper.deleteByQuestionId(id);
                solutionMapper.insertBatch(solutions);
            }
        }
    }

    /**
     * 逻辑删除当前用户创建的个人题。
     *
     * @param id 题目 ID
     */
    @Override
    @Transactional
    public void delete(Long id) {
        Long userId = requireUserId();
        requireOwnedPersonal(id, userId);
        int deleted = questionMapper.logicalDeleteOwned(id, userId);
        if (deleted == 0) {
            throw new BizException(ErrorCode.NOT_FOUND, "题目不存在");
        }
    }

    /**
     * 推荐公开题：同知识点优先，不足时用相近难度的其他知识点补齐。
     *
     * @param questionId 当前题 ID，用于取知识点/难度并排除自身
     * @param limit      推荐数量，null 默认 3，限制在 1~50
     * @return 推荐列表，可能为空
     */
    @Override
    public List<QuestionRecommendVO> recommend(Long questionId, Integer limit) {
        Long userId = requireUserId();
        QuestionDetailRow target = requireAccessible(questionId, userId);
        int safeLimit = limit == null ? 3 : Math.max(1, Math.min(limit, MAX_PAGE_SIZE));

        List<QuestionRecommendVO> records = questionMapper.selectRecommend(
                userId, target.getKnowledgePointId(), target.getDifficulty(), questionId, safeLimit);
        if (records == null || records.isEmpty()) {
            return List.of();
        }
        for (QuestionRecommendVO record : records) {
            boolean sameKnowledgePoint = target.getKnowledgePointId()
                    .equals(record.getKnowledgePointId());
            if (sameKnowledgePoint) {
                record.setReason(Boolean.FALSE.equals(record.getSolved())
                        ? "同知识点 · 还没做过" : "同知识点 · 难度接近");
            } else {
                record.setReason(Boolean.FALSE.equals(record.getSolved())
                        ? "相近难度 · 换个知识点" : "相近难度 · 已做过");
            }
        }
        return records;
    }

    /**
     * 判断请求是否显式选择了知识点。
     *
     * @param request 题目保存请求
     * @return true 表示传了 knowledgePointIds 或 knowledgePointId
     */
    private boolean hasKnowledgePointSelection(QuestionSaveRequest request) {
        return (request.getKnowledgePointIds() != null && !request.getKnowledgePointIds().isEmpty())
                || request.getKnowledgePointId() != null;
    }

    /**
     * 合并主知识点与多知识点列表，去重并按请求顺序返回。
     *
     * @param request  题目保存请求
     * @param fallback 请求没有选择时使用的知识点 ID
     * @return 去重后的知识点 ID 列表，首项作为主知识点
     */
    private List<Long> resolveKnowledgePointIds(QuestionSaveRequest request, Long fallback) {
        java.util.LinkedHashSet<Long> ids = new java.util.LinkedHashSet<>();
        if (request.getKnowledgePointIds() != null) {
            request.getKnowledgePointIds().stream()
                    .filter(java.util.Objects::nonNull)
                    .forEach(ids::add);
        }
        if (ids.isEmpty() && request.getKnowledgePointId() != null) {
            ids.add(request.getKnowledgePointId());
        }
        if (ids.isEmpty() && fallback != null) {
            ids.add(fallback);
        }
        if (ids.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "知识点不能为空");
        }
        if (ids.size() > 8) {
            throw new BizException(ErrorCode.PARAM_ERROR, "一题最多选择 8 个知识点");
        }
        for (Long pointId : ids) {
            requireKnowledgePoint(pointId);
        }
        return new ArrayList<>(ids);
    }

    /**
     * 读取题目现有知识点；旧数据没有关联记录时回退到主知识点。
     *
     * @param questionId 题目 ID
     * @param primaryId  题目主知识点 ID
     * @return 知识点 ID 列表
     */
    private List<Long> existingKnowledgePointIds(Long questionId, Long primaryId) {
        List<Long> ids = questionKnowledgePointMapper.selectIdsByQuestionId(questionId);
        if (ids == null || ids.isEmpty()) {
            return primaryId == null ? List.of() : List.of(primaryId);
        }
        return ids;
    }

    /**
     * 重写题目的知识点关联。
     *
     * @param questionId 题目 ID
     * @param pointIds   知识点 ID 列表，首项为主知识点
     */
    private void syncKnowledgePoints(Long questionId, List<Long> pointIds) {
        questionKnowledgePointMapper.deleteByQuestionId(questionId);
        if (pointIds != null && !pointIds.isEmpty()) {
            questionKnowledgePointMapper.insertBatch(questionId, pointIds);
        }
    }

    /**
     * 把请求中的可编辑字段复制到题目实体，并固定为当前用户的 PERSONAL 题。
     *
     * @param question 待填充的题目实体
     * @param request  前端提交的题目信息
     * @param userId   当前登录用户 ID，作为题目作者
     */
    private void fillQuestion(Question question, QuestionSaveRequest request, Long userId) {
        question.setTitle(request.getTitle().trim());
        question.setDescription(request.getDescription());
        question.setInputDesc(request.getInputDesc());
        question.setOutputDesc(request.getOutputDesc());
        question.setDifficulty(request.getDifficulty());
        question.setKnowledgePointId(request.getKnowledgePointId());
        question.setTemplateJson(normalizeTemplate(request.getTemplateJson()));
        question.setType(PERSONAL);
        question.setVisibility(normalizeVisibility(request.getVisibility()));
        question.setUserId(userId);
        question.setSourceImageUrl(request.getSourceImageUrl());
    }

    /**
     * 个人题允许 PRIVATE/SHARED；管理员额外允许 PUBLIC。
     *
     * @param visibility 前端传入的可见性
     * @return 规范化后的可见性
     */
    private String normalizeVisibility(String visibility) {
        if (visibility == null || visibility.isBlank()) {
            return PRIVATE;
        }
        String value = visibility.trim().toUpperCase(Locale.ROOT);
        if (PUBLIC.equals(value)) {
            if (!isAdmin()) {
                throw new BizException(ErrorCode.FORBIDDEN, "仅管理员可以发布公共题");
            }
            return PUBLIC;
        }
        if (!PRIVATE.equals(value) && !SHARED.equals(value)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "可见性只能是 PRIVATE、SHARED 或 PUBLIC");
        }
        return value;
    }

    /**
     * 当前登录用户是否管理员。
     *
     * @return true 表示 role=ADMIN
     */
    private boolean isAdmin() {
        UserContext.UserInfo user = UserContext.get();
        return user != null && "ADMIN".equalsIgnoreCase(user.getRole());
    }

    /**
     * 合并 testCases 与 samples，并校验至少有一条样例用例。
     *
     * @param request  题目保存请求，samples 中的用例会被强制标记为样例
     * @param required 是否要求至少提交一条用例；false 时允许空列表
     * @return 归一化后的用例实体列表
     */
    private List<QuestionTestCase> buildCases(QuestionSaveRequest request, boolean required) {
        List<QuestionTestCase> result = new ArrayList<>();
        int index = 0;
        if (request.getTestCases() != null) {
            for (TestCaseRequest item : request.getTestCases()) {
                result.add(toTestCase(item, item.sampleFlag(), ++index));
            }
        }
        // samples 是简化写法，进入这里的记录强制标记为样例。
        if (request.getSamples() != null) {
            for (TestCaseRequest item : request.getSamples()) {
                result.add(toTestCase(item, true, ++index));
            }
        }
        if (result.isEmpty()) {
            if (required) {
                throw new BizException(ErrorCode.PARAM_ERROR, "至少提交 1 条用例");
            }
            return result;
        }
        boolean hasSample = result.stream().anyMatch(item -> item.getIsSample() == 1);
        if (!hasSample) {
            throw new BizException(ErrorCode.PARAM_ERROR, "至少提交 1 条样例用例");
        }
        return result;
    }

    /**
     * 把单条请求转换为用例实体，补齐默认分值和排序号。
     *
     * @param request 单条用例请求
     * @param sample  是否强制标记为样例
     * @param index   当前用例在列表中的序号，作为默认 sortNo
     * @return 待入库的用例实体
     */
    private QuestionTestCase toTestCase(TestCaseRequest request, boolean sample, int index) {
        if (request.getInputData() == null || request.getInputData().isBlank()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "用例输入不能为空");
        }
        if (request.getExpectedOutput() == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "用例期望输出不能为空");
        }
        QuestionTestCase testCase = new QuestionTestCase();
        testCase.setInputData(request.getInputData());
        testCase.setExpectedOutput(request.getExpectedOutput());
        testCase.setIsSample(sample ? 1 : 0);
        testCase.setScore(request.getScore() == null ? DEFAULT_SCORE : Math.max(request.getScore(), 0));
        testCase.setSortNo(request.getSortNo() == null ? index : request.getSortNo());
        return testCase;
    }

    /**
     * 合并 solutions 与 referenceCode，按语言去重后生成待入库的参考代码。
     *
     * @param request 题目保存请求
     * @return 参考代码实体列表，未提供时为空列表
     */
    private List<QuestionSolution> buildSolutions(QuestionSaveRequest request) {
        Map<String, ReferenceSolutionRequest> merged = new LinkedHashMap<>();
        if (request.getSolutions() != null) {
            for (ReferenceSolutionRequest solution : request.getSolutions()) {
                addSolution(merged, solution.getLanguage(), solution.getContent(), solution.getExplanation());
            }
        }

        JsonNode reference = request.getReferenceCode();
        if (reference != null && !reference.isNull()) {
            if (reference.isTextual()) {
                addSolution(merged, "CPP", reference.asText(), null);
            } else if (reference.isObject()) {
                if (reference.hasNonNull("content") || reference.hasNonNull("code")) {
                    String content = reference.hasNonNull("content")
                            ? reference.get("content").asText() : reference.get("code").asText();
                    String language = reference.hasNonNull("language")
                            ? reference.get("language").asText() : "CPP";
                    String explanation = reference.hasNonNull("explanation")
                            ? reference.get("explanation").asText() : null;
                    addSolution(merged, language, content, explanation);
                } else {
                    // 也兼容 {"cpp":"...", "java":"..."} 的简单映射。
                    reference.properties().forEach(entry ->
                            addSolution(merged, entry.getKey(), entry.getValue().asText(), null));
                }
            } else {
                throw new BizException(ErrorCode.PARAM_ERROR, "referenceCode 格式不正确");
            }
        }

        List<QuestionSolution> result = new ArrayList<>();
        for (ReferenceSolutionRequest item : merged.values()) {
            QuestionSolution solution = new QuestionSolution();
            solution.setLanguage(item.getLanguage());
            solution.setContent(item.getContent());
            solution.setExplanation(item.getExplanation());
            result.add(solution);
        }
        return result;
    }

    /**
     * 校验并规范化一条参考代码，按语言放入 map；同语言后写覆盖前写。
     *
     * @param target      按语言聚合的参考代码 map
     * @param language    原始语言名，空值按 CPP 处理
     * @param content     参考代码内容，不能为空
     * @param explanation 可选讲解
     */
    private void addSolution(Map<String, ReferenceSolutionRequest> target, String language,
                             String content, String explanation) {
        if (content == null || content.isBlank()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "参考代码内容不能为空");
        }
        String normalized = normalizeLanguage(language);
        ReferenceSolutionRequest solution = new ReferenceSolutionRequest();
        solution.setLanguage(normalized);
        solution.setContent(content);
        solution.setExplanation(explanation);
        target.put(normalized, solution);
    }

    /**
     * 把语言名规范为 CPP 或 JAVA。
     *
     * @param language 原始语言名，null/空串默认 CPP
     * @return 规范化后的语言名
     */
    private String normalizeLanguage(String language) {
        if (language == null || language.isBlank()) {
            return "CPP";
        }
        String normalized = language.trim().toUpperCase(Locale.ROOT);
        if ("C++".equals(normalized) || "CPP".equals(normalized)) {
            return "CPP";
        }
        if ("JAVA".equals(normalized)) {
            return "JAVA";
        }
        throw new BizException(ErrorCode.PARAM_ERROR, "参考代码语言只能是 CPP 或 JAVA");
    }

    /**
     * 给一批用例回填所属题目 ID。
     *
     * @param cases      用例实体列表
     * @param questionId 所属题目 ID
     */
    private void attachQuestionId(List<QuestionTestCase> cases, Long questionId) {
        cases.forEach(item -> item.setQuestionId(questionId));
    }

    /**
     * 校验知识点存在。
     *
     * @param knowledgePointId 知识点 ID
     */
    private void requireKnowledgePoint(Long knowledgePointId) {
        KnowledgePoint point = knowledgePointMapper.selectById(knowledgePointId);
        if (point == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "知识点不存在");
        }
    }

    /**
     * 查询题目并校验访问权限：PUBLIC 所有人可读，SHARED 所有人可读，PRIVATE 仅作者可读。
     *
     * @param id     题目 ID
     * @param userId 当前登录用户 ID
     * @return 题目详情行
     */
    private QuestionDetailRow requireAccessible(Long id, Long userId) {
        QuestionDetailRow row = questionMapper.selectDetailRow(id);
        if (row == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "题目不存在");
        }
        if (PUBLIC.equalsIgnoreCase(row.getVisibility()) || SHARED.equalsIgnoreCase(row.getVisibility())) {
            return row;
        }
        if (PERSONAL.equals(row.getType()) && !userId.equals(row.getUserId())
                && contestMapper.countRegisteredQuestion(id, userId) == 0) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        return row;
    }

    /**
     * 要求题目是当前用户创建的 PERSONAL 题，用于修改和删除。
     *
     * @param id     题目 ID
     * @param userId 当前登录用户 ID
     * @return 已通过权限校验的题目详情行
     */
    private QuestionDetailRow requireOwnedPersonal(Long id, Long userId) {
        QuestionDetailRow row = requireAccessible(id, userId);
        if (!PERSONAL.equals(row.getType()) || !userId.equals(row.getUserId())) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        return row;
    }

    /**
     * 获取当前登录用户 ID，未登录时抛 40100。
     *
     * @return 当前用户 ID
     */
    private Long requireUserId() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return userId;
    }

    /**
     * 把 templateJson 规范为字符串：文本去空白，对象序列化，空值返回 null。
     *
     * @param node 前端提交的模板 JSON
     * @return 可入库的模板字符串，空值为 null
     */
    private String normalizeTemplate(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isTextual()) {
            String text = node.asText().trim();
            return text.isEmpty() ? null : text;
        }
        try {
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            throw new BizException(ErrorCode.PARAM_ERROR, "templateJson 格式不正确");
        }
    }

    /**
     * 把数据库里的模板字符串解析成 JSON 节点，数据损坏时抛 50000。
     *
     * @param raw 数据库中的模板字符串，可能为 null
     * @return JSON 节点；空值返回 null
     */
    private JsonNode parseTemplate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(raw);
        } catch (JsonProcessingException e) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "题目模板数据损坏");
        }
    }
}

package com.codeagent.service.impl;

import com.codeagent.ai.PistonCircuitBreaker;
import com.codeagent.ai.PistonClient;
import com.codeagent.ai.PistonExecuteResponse;
import com.codeagent.common.ErrorCode;
import com.codeagent.common.PageResult;
import com.codeagent.dto.JudgeCaseResult;
import com.codeagent.dto.JudgeCustomCaseRequest;
import com.codeagent.dto.QuestionDetailRow;
import com.codeagent.entity.QuestionTestCase;
import com.codeagent.entity.SubmitRecord;
import com.codeagent.exception.BizException;
import com.codeagent.mapper.QuestionMapper;
import com.codeagent.mapper.QuestionTestCaseMapper;
import com.codeagent.mapper.SubmitRecordMapper;
import com.codeagent.service.ContestService;
import com.codeagent.service.JudgeRateLimiter;
import com.codeagent.service.JudgeService;
import com.codeagent.service.ProfileStatService;
import com.codeagent.service.RateLimitService;
import com.codeagent.service.WrongBookService;
import com.codeagent.vo.JudgeCaseResultVO;
import com.codeagent.vo.JudgeHistoryVO;
import com.codeagent.vo.JudgeResultVO;
import com.codeagent.vo.JudgeRunSampleVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 异步判题核心实现：落库、线程池、逐用例调用 Piston、状态归并和错题记录。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JudgeServiceImpl implements JudgeService {

    private static final String PENDING = "PENDING";
    private static final String JUDGING = "JUDGING";
    private static final String AC = "AC";
    private static final String WA = "WA";
    private static final String TLE = "TLE";
    private static final String MLE = "MLE";
    private static final String RE = "RE";
    private static final String CE = "CE";
    private static final String SE = "SE";

    private static final int MAX_CODE_LENGTH = 100_000;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;
    private static final int MAX_COMPILE_OUTPUT = 20_000;

    private final JudgeRateLimiter rateLimiter;
    private final RateLimitService rateLimitService;
    private final QuestionMapper questionMapper;
    private final QuestionTestCaseMapper testCaseMapper;
    private final SubmitRecordMapper submitRecordMapper;
    private final PistonClient pistonClient;
    private final PistonCircuitBreaker pistonCircuitBreaker;
    private final WrongBookService wrongBookService;
    private final ProfileStatService profileStatService;
    private final ContestService contestService;
    private final ObjectMapper objectMapper;

    @Resource(name = "judgeExecutor")
    private ThreadPoolTaskExecutor judgeExecutor;

    /**
     * 校验参数和题目权限，插入 PENDING 记录后投递异步任务。
     *
     * @param userId     当前登录用户 ID
     * @param questionId 题目 ID
     * @param contestId  竞赛 ID；普通练习为 null
     * @param language   语言代码 CPP / JAVA
     * @param code       完整源代码
     * @return 新建的提交记录 ID
     */
    @Override
    public Long submit(Long userId, Long questionId, Long contestId, String language, String code) {
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        rateLimiter.check(userId);
        pistonCircuitBreaker.ensureAvailable();

        if (questionId == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "题目 ID 不能为空");
        }
        String normalizedLanguage = normalizeLanguage(language);
        if (code == null || code.isBlank()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "代码不能为空");
        }
        if (code.length() > MAX_CODE_LENGTH) {
            throw new BizException(ErrorCode.PARAM_ERROR, "代码长度不能超过 100000 个字符");
        }
        requireAccessible(questionId, userId);
        contestService.validateSubmission(contestId, questionId, userId);

        List<QuestionTestCase> cases = testCaseMapper.selectAllByQuestionId(questionId);
        if (cases == null || cases.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "题目没有测试用例，暂时无法判题");
        }

        SubmitRecord record = new SubmitRecord();
        record.setUserId(userId);
        record.setQuestionId(questionId);
        record.setContestId(contestId);
        record.setLanguage(normalizedLanguage);
        record.setCode(code);
        record.setStatus(PENDING);
        record.setPassedCount(0);
        record.setTotalCount(cases.size());
        record.setRunTimeMs(0L);
        submitRecordMapper.insert(record);
        Long submitId = record.getId();
        if (submitId == null) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "提交记录创建失败");
        }

        try {
            judgeExecutor.execute(() -> judgeAsync(submitId));
        } catch (TaskRejectedException e) {
            submitRecordMapper.markFailed(submitId, SE, "判题繁忙，请稍后");
            throw new BizException(ErrorCode.JUDGE_QUEUE_FULL, "判题繁忙，请稍后");
        }
        return submitId;
    }

    /**
     * 只执行公开样例：不落 submit_record，不记错题，也不让样例结果污染统计。
     *
     * @param userId     当前登录用户 ID
     * @param questionId 题目 ID
     * @param language   语言代码 CPP / JAVA
     * @param code       完整源代码
     * @return 样例运行结果
     */
    @Override
    public JudgeRunSampleVO runSample(Long userId, Long questionId, String language, String code,
                                      List<JudgeCustomCaseRequest> customCases) {
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        rateLimitService.check("judge:run-sample:" + userId, 10, 60_000L,
                "运行样例过于频繁，请稍后再试");
        pistonCircuitBreaker.ensureAvailable();

        if (questionId == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "题目 ID 不能为空");
        }
        String normalizedLanguage = normalizeLanguage(language);
        if (code == null || code.isBlank()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "代码不能为空");
        }
        if (code.length() > MAX_CODE_LENGTH) {
            throw new BizException(ErrorCode.PARAM_ERROR, "代码长度不能超过 100000 个字符");
        }
        requireAccessible(questionId, userId);

        List<QuestionTestCase> allCases = testCaseMapper.selectAllByQuestionId(questionId);
        List<QuestionTestCase> samples = allCases == null ? List.of() : allCases.stream()
                .filter(item -> Integer.valueOf(1).equals(item.getIsSample()))
                .toList();

        // 官方样例在前，用户临时添加的样例在后；两者都只用于本次运行。
        List<QuestionTestCase> cases = new ArrayList<>(samples);
        if (customCases != null) {
            for (JudgeCustomCaseRequest customCase : customCases) {
                if (customCase == null) {
                    continue;
                }
                QuestionTestCase item = new QuestionTestCase();
                item.setQuestionId(questionId);
                item.setInputData(customCase.getInput());
                item.setExpectedOutput(customCase.getExpectedOutput());
                item.setIsSample(1);
                item.setScore(0);
                item.setSortNo(cases.size() + 1);
                cases.add(item);
            }
        }
        if (cases.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "题目没有样例，暂时无法运行");
        }

        SubmitRecord record = new SubmitRecord();
        record.setUserId(userId);
        record.setQuestionId(questionId);
        record.setLanguage(normalizedLanguage);
        record.setCode(code);
        record.setStatus(PENDING);
        record.setPassedCount(0);
        record.setTotalCount(cases.size());
        record.setRunTimeMs(0L);

        SubmitRecord result = executeCases(record, cases);
        JudgeRunSampleVO vo = new JudgeRunSampleVO();
        vo.setStatus(result.getStatus());
        vo.setPassedCount(result.getPassedCount() == null ? 0 : result.getPassedCount());
        vo.setTotalCount(result.getTotalCount() == null ? cases.size() : result.getTotalCount());
        vo.setRunTimeMs(result.getRunTimeMs() == null ? 0L : result.getRunTimeMs());
        vo.setCompileOutput(result.getCompileOutput());
        vo.setErrorMessage(result.getErrorMessage());
        List<JudgeCaseResultVO> caseResults = parseCaseResults(result.getCaseResultJson());
        for (int i = samples.size(); i < caseResults.size(); i++) {
            caseResults.get(i).setCustom(true);
        }
        vo.setCaseResults(caseResults);
        log.info("[monitor] type=run-sample userId={} questionId={} status={} passed={}/{} durationMs={}",
                userId, questionId, vo.getStatus(), vo.getPassedCount(), vo.getTotalCount(), vo.getRunTimeMs());
        return vo;
    }

    /**
     * 查询判题结果；非本人提交统一按不存在处理，避免泄露。
     *
     * @param submitId 提交记录 ID
     * @param userId   当前登录用户 ID
     * @return 判题结果
     */
    @Override
    public JudgeResultVO result(Long submitId, Long userId) {
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        SubmitRecord record = submitRecordMapper.selectById(submitId);
        if (record == null || !userId.equals(record.getUserId())) {
            throw new BizException(ErrorCode.NOT_FOUND, "提交记录不存在");
        }

        JudgeResultVO vo = new JudgeResultVO();
        vo.setSubmitId(record.getId());
        vo.setStatus(record.getStatus());
        vo.setPassedCount(record.getPassedCount() == null ? 0 : record.getPassedCount());
        vo.setTotalCount(record.getTotalCount() == null ? 0 : record.getTotalCount());
        vo.setRunTimeMs(record.getRunTimeMs() == null ? 0L : record.getRunTimeMs());
        vo.setCompileOutput(record.getCompileOutput());
        vo.setErrorMessage(record.getErrorMessage());
        vo.setCaseResults(parseCaseResults(record.getCaseResultJson()));
        vo.setAgentSuggest(!"AC".equals(record.getStatus())
                && !PENDING.equals(record.getStatus())
                && !JUDGING.equals(record.getStatus()));
        return vo;
    }

    /**
     * 分页查询当前用户历史，页码和每页条数会被规范化。
     *
     * @param userId     当前登录用户 ID
     * @param questionId 题目 ID，null 表示不限
     * @param page       页码，从 1 开始
     * @param size       每页条数，服务端限制为 1~50
     * @return 分页提交历史
     */
    @Override
    public PageResult<JudgeHistoryVO> history(Long userId, Long questionId, long page, long size) {
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        long safePage = Math.max(page, 1L);
        long safeSize = size < 1 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        long total = submitRecordMapper.countHistory(userId, questionId);
        if (total == 0) {
            return PageResult.empty(safePage, safeSize);
        }
        long offset = (safePage - 1L) * safeSize;
        if (offset < 0) {
            throw new BizException(ErrorCode.PARAM_ERROR, "页码过大");
        }
        List<JudgeHistoryVO> records = submitRecordMapper.selectHistory(userId, questionId, offset, safeSize);
        return PageResult.of(records == null ? List.of() : records, total, safePage, safeSize);
    }

    /**
     * 异步任务入口：标记 JUDGING、执行用例、写回结果并按需记错题。
     *
     * @param submitId 提交记录 ID
     */
    private void judgeAsync(Long submitId) {
        long startedAt = System.currentTimeMillis();
        try {
            if (submitRecordMapper.markJudging(submitId) == 0) {
                return;
            }
            SubmitRecord record = submitRecordMapper.selectById(submitId);
            if (record == null) {
                return;
            }
            List<QuestionTestCase> cases = testCaseMapper.selectAllByQuestionId(record.getQuestionId());
            if (cases == null || cases.isEmpty()) {
                submitRecordMapper.markFailed(submitId, SE, "题目没有测试用例");
                return;
            }
            SubmitRecord result = executeCases(record, cases);
            submitRecordMapper.updateResult(result);
            profileStatService.invalidate(result.getUserId());
            log.info("[monitor] type=judge submitId={} userId={} status={} passed={}/{} durationMs={}",
                    submitId, result.getUserId(), result.getStatus(),
                    result.getPassedCount(), result.getTotalCount(),
                    System.currentTimeMillis() - startedAt);

            if (result.getContestId() == null
                    && !AC.equals(result.getStatus()) && !SE.equals(result.getStatus())) {
                try {
                    wrongBookService.recordWrong(result.getUserId(), result.getQuestionId(), submitId);
                } catch (Exception e) {
                    log.warn("记录错题失败, submitId={}: {}", submitId, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("判题任务异常, submitId={}", submitId, e);
            try {
                submitRecordMapper.markFailed(submitId, SE, "判题服务异常，请稍后重试");
            } catch (Exception ignored) {
                log.error("回写判题异常状态失败, submitId={}", submitId, ignored);
            }
        }
    }

    /**
     * 串行执行全部用例。编译失败立即结束；出现运行超时/内存超限/运行错误时提前结束，
     * 避免死循环用例把 4 次 3 秒超时都跑满。
     *
     * @param record 当前提交记录
     * @param cases  按 sort_no 排序的全部用例
     * @return 已填充判题字段的提交记录
     */
    private SubmitRecord executeCases(SubmitRecord record, List<QuestionTestCase> cases) {
        List<JudgeCaseResult> caseResults = new ArrayList<>();
        int passedCount = 0;
        long maxRunTime = 0L;
        String overallStatus = AC;
        String compileOutput = null;
        String errorMessage = null;

        for (int i = 0; i < cases.size(); i++) {
            QuestionTestCase testCase = cases.get(i);
            boolean sample = Integer.valueOf(1).equals(testCase.getIsSample());
            long startedAt = System.currentTimeMillis();

            PistonExecuteResponse response;
            try {
                response = pistonClient.execute(record.getLanguage(), record.getCode(), testCase.getInputData());
            } catch (BizException e) {
                record.setStatus(SE);
                record.setPassedCount(0);
                record.setTotalCount(cases.size());
                record.setRunTimeMs(maxRunTime);
                record.setErrorMessage(e.getMessage());
                return record;
            }
            long elapsed = Math.max(System.currentTimeMillis() - startedAt, 0L);

            PistonExecuteResponse.Stage compile = response == null ? null : response.compile();
            if (compile != null && compile.code() != null && compile.code() != 0) {
                String compileText = stageText(compile);
                JudgeCaseResult item = baseCase(i + 1, testCase, sample);
                item.setPassed(false);
                item.setStderr(compileText);
                item.setTimeMs(elapsed);
                if (!sample) {
                    // 编译失败时不应连带泄露隐藏用例的输入和期望输出。
                    item.setInput(null);
                    item.setExpected(null);
                }
                caseResults.add(item);
                overallStatus = CE;
                compileOutput = compileText;
                errorMessage = "编译错误";
                maxRunTime = Math.max(maxRunTime, elapsed);
                break;
            }

            PistonExecuteResponse.Stage run = response == null ? null : response.run();
            String stdout = run == null ? "" : nullToEmpty(run.stdout());
            String stderr = run == null ? "" : nullToEmpty(run.stderr());
            String expected = normalizeOutput(testCase.getExpectedOutput());
            String actual = normalizeOutput(stdout);
            boolean passed = run != null && actual.equals(expected);
            int exitCode = run == null || run.code() == null ? -1 : run.code();
            String signal = run == null ? null : run.signal();
            String caseStatus = classify(exitCode, signal, passed);

            if (AC.equals(caseStatus)) {
                passedCount++;
            }
            JudgeCaseResult item = baseCase(i + 1, testCase, sample);
            item.setPassed(AC.equals(caseStatus));
            item.setStderr(stderr);
            item.setExpected(expected);
            item.setActual(actual);
            item.setTimeMs(elapsed);
            if (sample) {
                item.setInput(testCase.getInputData());
                item.setExpected(expected);
            } else {
                // 隐藏用例只在失败时返回输入/期望/实际；通过的隐藏用例不向响应泄露内容。
                item.setStderr(null);
                if (AC.equals(caseStatus)) {
                    item.setInput(null);
                    item.setExpected(null);
                    item.setActual(null);
                }
            }
            caseResults.add(item);

            maxRunTime = Math.max(maxRunTime, elapsed);
            overallStatus = worse(overallStatus, caseStatus);
            if (!AC.equals(caseStatus) && errorMessage == null) {
                errorMessage = errorText(caseStatus, i + 1);
            }
            if (TLE.equals(caseStatus) || MLE.equals(caseStatus) || RE.equals(caseStatus)) {
                break;
            }
        }

        record.setStatus(overallStatus);
        record.setPassedCount(passedCount);
        record.setTotalCount(cases.size());
        record.setRunTimeMs(maxRunTime);
        record.setCompileOutput(compileOutput);
        record.setErrorMessage(errorMessage);
        record.setCaseResultJson(serializeCases(caseResults));
        return record;
    }

    /**
     * 构造单条用例结果的基础字段。
     *
     * @param index    用例序号，从 1 开始
     * @param testCase 用例实体
     * @param sample   是否为公开样例
     * @return 待填充的用例结果
     */
    private JudgeCaseResult baseCase(int index, QuestionTestCase testCase, boolean sample) {
        JudgeCaseResult item = new JudgeCaseResult();
        item.setIndex(index);
        item.setSample(sample);
        item.setHidden(!sample);
        item.setInput(testCase.getInputData());
        item.setExpected(testCase.getExpectedOutput());
        return item;
    }

    /**
     * 根据退出码、信号和输出是否匹配判定单个用例状态。
     *
     * @param exitCode 进程退出码，-1 表示没有运行结果
     * @param signal   终止信号，正常结束为 null
     * @param passed   标准输出是否与期望一致
     * @return AC/WA/TLE/MLE/RE 之一
     */
    private String classify(int exitCode, String signal, boolean passed) {
        // Piston 在运行超时时返回 124；不能用整个请求耗时判断，
        // 否则编译较慢但运行正常的用例会被误判为 TLE。
        if (exitCode == 124) {
            return TLE;
        }
        if (exitCode == 137 || "SIGKILL".equalsIgnoreCase(signal)) {
            return MLE;
        }
        if (exitCode != 0 || (signal != null && !signal.isBlank())) {
            return RE;
        }
        return passed ? AC : WA;
    }

    /**
     * 按 CE > RE > TLE > MLE > WA > AC 的优先级合并状态。
     *
     * @param current   当前累计状态
     * @param candidate 新用例状态
     * @return 优先级更高的状态
     */
    private String worse(String current, String candidate) {
        return priority(candidate) > priority(current) ? candidate : current;
    }

    /**
     * 状态优先级，数字越大越优先。
     *
     * @param status 判题状态
     * @return 优先级数值
     */
    private int priority(String status) {
        return switch (status) {
            case CE -> 6;
            case RE -> 5;
            case TLE -> 4;
            case MLE -> 3;
            case WA -> 2;
            case AC -> 1;
            default -> 0;
        };
    }

    /**
     * 生成不包含隐藏用例内容的错误摘要。
     *
     * @param status 用例状态
     * @param index  用例序号
     * @return 面向用户的摘要
     */
    private String errorText(String status, int index) {
        return switch (status) {
            case CE -> "编译错误";
            case TLE -> "第 " + index + " 个用例运行超时";
            case MLE -> "第 " + index + " 个用例内存超限";
            case RE -> "第 " + index + " 个用例运行错误";
            case WA -> "第 " + index + " 个用例输出不符";
            default -> null;
        };
    }

    /**
     * 输出归一化：统一换行、逐行去尾部空白、去掉末尾空行，但保留行内和行首空格。
     *
     * @param value 原始输出
     * @return 归一化后的输出，null 按空串处理
     */
    private String normalizeOutput(String value) {
        if (value == null) {
            return "";
        }
        String text = value.replace("\r\n", "\n").replace('\r', '\n');
        String[] lines = text.split("\n", -1);
        int end = lines.length;
        while (end > 0 && rstrip(lines[end - 1]).isEmpty()) {
            end--;
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < end; i++) {
            if (i > 0) {
                builder.append('\n');
            }
            builder.append(rstrip(lines[i]));
        }
        return builder.toString();
    }

    /**
     * 去掉一行末尾的空白字符，不影响行首和行内空格。
     *
     * @param line 原始行
     * @return 去掉尾部空白后的行
     */
    private String rstrip(String line) {
        int end = line.length();
        while (end > 0 && Character.isWhitespace(line.charAt(end - 1))) {
            end--;
        }
        return line.substring(0, end);
    }

    /**
     * 合并编译阶段的 stdout/stderr，并限制长度。
     *
     * @param stage 编译阶段结果
     * @return 可直接写入 compile_output 的文本
     */
    private String stageText(PistonExecuteResponse.Stage stage) {
        String text = nullToEmpty(stage.stdout()) + nullToEmpty(stage.stderr());
        if (text.isBlank()) {
            text = "编译失败（Piston 未返回错误输出）";
        }
        return text.length() > MAX_COMPILE_OUTPUT ? text.substring(0, MAX_COMPILE_OUTPUT) : text;
    }

    /**
     * 序列化逐用例结果；序列化失败时只记录日志，不改变判题状态。
     *
     * @param caseResults 用例结果列表
     * @return JSON 字符串；失败时为 null
     */
    private String serializeCases(List<JudgeCaseResult> caseResults) {
        try {
            return objectMapper.writeValueAsString(caseResults);
        } catch (JsonProcessingException e) {
            log.error("序列化用例结果失败", e);
            return null;
        }
    }

    /**
     * 解析数据库中的逐用例 JSON 并转换为响应对象。
     *
     * @param json 提交记录中的 case_result_json
     * @return 用例结果列表；为空或解析失败时返回空列表
     */
    private List<JudgeCaseResultVO> parseCaseResults(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            List<JudgeCaseResult> items = objectMapper.readValue(json,
                    new TypeReference<List<JudgeCaseResult>>() {
                    });
            List<JudgeCaseResultVO> result = new ArrayList<>();
            for (JudgeCaseResult item : items) {
                JudgeCaseResultVO vo = new JudgeCaseResultVO();
                vo.setIndex(item.getIndex());
                vo.setSample(item.isSample());
                vo.setPassed(item.isPassed());
                vo.setInput(item.getInput());
                vo.setExpected(item.getExpected());
                vo.setActual(item.getActual());
                vo.setStderr(item.getStderr());
                vo.setTimeMs(item.getTimeMs());
                result.add(vo);
            }
            return result;
        } catch (JsonProcessingException e) {
            log.warn("解析用例结果失败: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * 校验题目存在且当前用户有权访问：公共题与共享题所有人可读，私有个人题仅作者可读。
     *
     * @param questionId 题目 ID
     * @param userId     当前登录用户 ID
     * @return 题目详情行
     */
    private QuestionDetailRow requireAccessible(Long questionId, Long userId) {
        QuestionDetailRow question = questionMapper.selectDetailRow(questionId);
        if (question == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "题目不存在");
        }
        if ("PERSONAL".equals(question.getType())
                && !"SHARED".equalsIgnoreCase(question.getVisibility())
                && !userId.equals(question.getUserId())) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        return question;
    }

    /**
     * 把语言名规范为 CPP 或 JAVA。
     *
     * @param language 原始语言名
     * @return 规范化后的语言名
     */
    private String normalizeLanguage(String language) {
        if (language == null || language.isBlank()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "语言不能为空");
        }
        String normalized = language.trim().toUpperCase(Locale.ROOT);
        if ("C++".equals(normalized) || "CPP".equals(normalized)) {
            return "CPP";
        }
        if ("JAVA".equals(normalized)) {
            return "JAVA";
        }
        throw new BizException(ErrorCode.PARAM_ERROR, "语言只能是 CPP 或 JAVA");
    }

    /**
     * null 转空串。
     *
     * @param value 原始值
     * @return 非 null 字符串
     */
    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}

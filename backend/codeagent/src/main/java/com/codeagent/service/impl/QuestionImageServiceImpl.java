package com.codeagent.service.impl;

import com.codeagent.common.ErrorCode;
import com.codeagent.dto.ImageQuestionDraft;
import com.codeagent.dto.ImportParsedRequest;
import com.codeagent.dto.QuestionSaveRequest;
import com.codeagent.dto.TestCaseRequest;
import com.codeagent.entity.KnowledgePoint;
import com.codeagent.exception.BizException;
import com.codeagent.mapper.KnowledgePointMapper;
import com.codeagent.service.DashScopeVisionClient;
import com.codeagent.service.DraftJsonParser;
import com.codeagent.service.FileStorageService;
import com.codeagent.service.ImageProcessor;
import com.codeagent.service.ParsedDraftCache;
import com.codeagent.service.QuestionImageService;
import com.codeagent.service.QuestionService;
import com.codeagent.vo.ImportParsedVO;
import com.codeagent.vo.ParseImageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

/**
 * 图片识别与草稿导入实现。
 */
@Service
@RequiredArgsConstructor
public class QuestionImageServiceImpl implements QuestionImageService {

    private final ImageProcessor imageProcessor;
    private final FileStorageService fileStorageService;
    private final DashScopeVisionClient visionClient;
    private final DraftJsonParser draftJsonParser;
    private final ParsedDraftCache parsedDraftCache;
    private final KnowledgePointMapper knowledgePointMapper;
    private final QuestionService questionService;

    /**
     * 压缩图片、调用视觉模型、容错解析并缓存草稿。任何一步失败都不会写题目表。
     *
     * @param file     上传图片
     * @param imageUrl 已上传图片地址，可为空
     * @param userId   当前用户 ID
     * @return 识别结果
     */
    @Override
    public ParseImageVO parse(MultipartFile file, String imageUrl, Long userId) {
        ImageProcessor.PreparedImage prepared = imageProcessor.prepare(file);

        String finalImageUrl = imageUrl;
        if (finalImageUrl == null || finalImageUrl.isBlank()) {
            finalImageUrl = fileStorageService.storeJpeg(prepared.bytes()).url();
        }

        String raw = visionClient.extract(prepared.bytes());
        DraftJsonParser.ParsedDraft parsed = draftJsonParser.parse(raw);
        ImageQuestionDraft draft = parsed.draft();
        String parseId = parsedDraftCache.put(userId, draft, finalImageUrl);

        ParseImageVO vo = new ParseImageVO();
        vo.setParseId(parseId);
        vo.setDraft(draft);
        vo.setMissingFields(parsed.missingFields());
        vo.setWarn(parsed.warn());
        vo.setImageUrl(finalImageUrl);
        return vo;
    }

    /**
     * 校验 parseId 归属、知识点 code 和样例，再复用现有个人题创建逻辑。
     *
     * @param request 用户确认后的草稿
     * @param userId  当前用户 ID
     * @return 新题 ID 及隐藏用例提示
     */
    @Override
    public ImportParsedVO importParsed(ImportParsedRequest request, Long userId) {
        ParsedDraftCache.CachedDraft cached = parsedDraftCache.get(request.getParseId(), userId);
        if (cached == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "识别结果已过期，请重新上传");
        }

        String code = request.getKnowledgePointCode() == null ? "" : request.getKnowledgePointCode().trim();
        KnowledgePoint point = knowledgePointMapper.selectByCode(code);
        if (point == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "知识点不存在");
        }

        List<TestCaseRequest> hidden = request.getTestCases();
        boolean needManualConfirm = hidden == null || hidden.isEmpty();
        if (needManualConfirm) {
            hidden = copySamplesAsHidden(request.getSamples());
        } else {
            int sampleCount = request.getSamples().size();
            for (int i = 0; i < hidden.size(); i++) {
                TestCaseRequest item = hidden.get(i);
                item.setIsSample(false);
                item.setScore(25);
                item.setSortNo(sampleCount + i + 1);
            }
        }

        QuestionSaveRequest save = new QuestionSaveRequest();
        save.setTitle(request.getTitle());
        save.setDescription(request.getDescription());
        save.setInputDesc(request.getInputDesc());
        save.setOutputDesc(request.getOutputDesc());
        save.setDifficulty(request.getDifficulty());
        save.setKnowledgePointId(point.getId());
        // 样例统一排在前、隐藏用例排在后，避免前端/数据库排序混乱。
        for (int i = 0; i < request.getSamples().size(); i++) {
            TestCaseRequest sample = request.getSamples().get(i);
            sample.setIsSample(true);
            sample.setScore(25);
            sample.setSortNo(i + 1);
        }
        save.setSamples(request.getSamples());
        save.setTestCases(hidden);
        save.setSourceImageUrl(firstNonBlank(request.getSourceImageUrl(), cached.imageUrl()));

        Long questionId = questionService.create(save);
        parsedDraftCache.remove(request.getParseId());
        return new ImportParsedVO(
                questionId,
                needManualConfirm,
                needManualConfirm
                        ? "已用样例复制 2 条隐藏用例，请人工确认后使用"
                        : "题目已创建"
        );
    }

    /**
     * 用户没有填写隐藏用例时，用样例循环复制两条隐藏用例。
     *
     * @param samples 用户确认的样例
     * @return 两条隐藏用例
     */
    private List<TestCaseRequest> copySamplesAsHidden(List<TestCaseRequest> samples) {
        if (samples == null || samples.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "至少提交 1 条样例用例");
        }
        List<TestCaseRequest> hidden = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            TestCaseRequest source = samples.get(i % samples.size());
            TestCaseRequest copy = new TestCaseRequest();
            copy.setInputData(source.getInputData());
            copy.setExpectedOutput(source.getExpectedOutput());
            copy.setIsSample(false);
            copy.setScore(25);
            copy.setSortNo(samples.size() + i + 1);
            hidden.add(copy);
        }
        return hidden;
    }

    /**
     * 返回第一个非空白字符串。
     *
     * @param first  首选值
     * @param second 备选值
     * @return 非空白值，二者都为空时返回 null
     */
    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second;
    }
}

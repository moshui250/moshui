package com.codeagent.service;

import com.codeagent.common.ErrorCode;
import com.codeagent.dto.ImageDraftSample;
import com.codeagent.dto.ImageQuestionDraft;
import com.codeagent.exception.BizException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 视觉模型 JSON 容错解析器。依次尝试原文、去围栏、截取首尾大括号。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DraftJsonParser {

    private final ObjectMapper objectMapper;

    /**
     * 解析模型输出，并计算缺失字段。
     *
     * @param raw 模型原始文本
     * @return 草稿、缺失字段和提示
     */
    public ParsedDraft parse(String raw) {
        String content = raw == null ? "" : raw.trim();
        List<String> candidates = List.of(content, stripFences(content), betweenBraces(content));

        for (String candidate : candidates) {
            if (candidate == null || candidate.isBlank()) {
                continue;
            }
            try {
                JsonNode node = objectMapper.readTree(candidate);
                if (node != null && node.isObject()) {
                    normalizeShape(node);
                    ImageQuestionDraft draft = objectMapper.treeToValue(node, ImageQuestionDraft.class);
                    normalize(draft);
                    if (isEmpty(draft)) {
                        continue;
                    }
                    List<String> missing = missingFields(draft);
                    draft.setMissingFields(missing);
                    return new ParsedDraft(draft, missing, warnText(missing));
                }
            } catch (Exception error) {
                log.warn("草稿 JSON 解析失败: {}，原文前缀={}", error.getMessage(), preview(content));
            }
        }
        throw new BizException(ErrorCode.IMAGE_PARSE_FAILED, "识别失败，请手动录入");
    }

    /**
     * 去掉 Markdown 代码围栏。
     *
     * @param raw 原始文本
     * @return 去围栏后的文本
     */
    private String stripFences(String raw) {
        String text = raw.trim();
        if (!text.startsWith("```")) {
            return text;
        }
        int firstLineEnd = text.indexOf('\n');
        if (firstLineEnd < 0) {
            return text.replaceFirst("^```[a-zA-Z]*", "").replaceFirst("```$", "").trim();
        }
        String withoutStart = text.substring(firstLineEnd + 1);
        int fenceEnd = withoutStart.lastIndexOf("```");
        if (fenceEnd >= 0) {
            withoutStart = withoutStart.substring(0, fenceEnd);
        }
        return withoutStart.trim();
    }

    /**
     * 截取第一个 { 到最后一个 }，容忍模型在 JSON 前后加说明。
     *
     * @param raw 原始文本
     * @return 截取后的文本，找不到大括号时返回原文
     */
    private String betweenBraces(String raw) {
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return raw.substring(start, end + 1);
        }
        return raw;
    }

    /**
     * 兼容模型把 knowledgePointCode 返回成数组、把 samples 返回成单个对象的情况。
     *
     * @param node 原始 JSON 对象
     */
    private void normalizeShape(JsonNode node) {
        if (!(node instanceof ObjectNode object)) {
            return;
        }
        for (String field : List.of("title", "description", "inputDesc", "outputDesc",
                "difficulty", "knowledgePointCode", "knowledgePointName", "confidence")) {
            flattenFirst(object, field);
        }

        JsonNode samples = object.get("samples");
        if (samples != null && samples.isObject()) {
            var array = object.arrayNode();
            array.add(samples);
            object.set("samples", array);
            samples = array;
        }
        if (samples != null && samples.isArray()) {
            for (JsonNode sample : samples) {
                if (sample instanceof ObjectNode sampleObject) {
                    flattenFirst(sampleObject, "input");
                    flattenFirst(sampleObject, "output");
                }
            }
        }
    }

    /**
     * 模型偶尔把单个字符串字段返回成数组，取第一个元素并保留原始类型。
     *
     * @param object JSON 对象
     * @param field  字段名
     */
    private void flattenFirst(ObjectNode object, String field) {
        JsonNode value = object.get(field);
        if (value == null || !value.isArray()) {
            return;
        }
        if (value.isEmpty()) {
            object.putNull(field);
        } else {
            object.set(field, value.get(0));
        }
    }

    /**
     * 归一化可选字段，避免模型给出越界值。
     *
     * @param draft 待归一化草稿
     */
    private void normalize(ImageQuestionDraft draft) {
        if (draft.getDifficulty() == null || draft.getDifficulty() < 1) {
            draft.setDifficulty(1);
        } else if (draft.getDifficulty() > 3) {
            draft.setDifficulty(3);
        }
        if (draft.getConfidence() == null) {
            draft.setConfidence(0.0);
        } else {
            draft.setConfidence(Math.max(0, Math.min(1, draft.getConfidence())));
        }
        if (draft.getKnowledgePointCode() != null) {
            draft.setKnowledgePointCode(draft.getKnowledgePointCode().trim());
        }
        if (draft.getSamples() == null) {
            draft.setSamples(new ArrayList<>());
        }
        draft.getSamples().removeIf(sample -> sample == null
                || (isBlank(sample.getInput()) && isBlank(sample.getOutput())));
        for (ImageDraftSample sample : draft.getSamples()) {
            sample.setInput(sample.getInput() == null ? "" : sample.getInput().trim());
            sample.setOutput(sample.getOutput() == null ? "" : sample.getOutput().trim());
        }
    }

    /**
     * 判断模型是否实际上没有识别出任何内容。
     *
     * @param draft 草稿
     * @return true 表示空草稿
     */
    private boolean isEmpty(ImageQuestionDraft draft) {
        return isBlank(draft.getTitle())
                && isBlank(draft.getDescription())
                && isBlank(draft.getInputDesc())
                && isBlank(draft.getOutputDesc())
                && (draft.getSamples() == null || draft.getSamples().isEmpty());
    }

    /**
     * 计算缺失字段。输入/输出格式是验收要求的重点。
     *
     * @param draft 草稿
     * @return 缺失字段列表
     */
    private List<String> missingFields(ImageQuestionDraft draft) {
        List<String> missing = new ArrayList<>();
        if (isBlank(draft.getTitle())) {
            missing.add("题目标题");
        }
        if (isBlank(draft.getDescription())) {
            missing.add("题目描述");
        }
        if (isBlank(draft.getInputDesc())) {
            missing.add("输入格式");
        }
        if (isBlank(draft.getOutputDesc())) {
            missing.add("输出格式");
        }
        if (draft.getSamples() == null || draft.getSamples().isEmpty()) {
            missing.add("样例用例");
        }
        if (isBlank(draft.getKnowledgePointCode())) {
            missing.add("知识点");
        }
        return missing;
    }

    /**
     * 生成面向用户的缺失提示。
     *
     * @param missing 缺失字段
     * @return 提示文本；无缺失时为 null
     */
    private String warnText(List<String> missing) {
        return missing.isEmpty() ? null : "未识别到" + String.join("、", missing) + "，请手动补全";
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /**
     * 截取日志可读的原文前缀。
     *
     * @param raw 模型原始文本
     * @return 最多 300 字符的前缀
     */
    private String preview(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.length() <= 300 ? raw : raw.substring(0, 300);
    }

    /**
     * 解析结果。
     *
     * @param draft         草稿
     * @param missingFields 缺失字段
     * @param warn          提示文本
     */
    public record ParsedDraft(ImageQuestionDraft draft, List<String> missingFields, String warn) {
    }
}

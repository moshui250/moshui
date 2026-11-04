package com.codeagent.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 视觉模型识别出的题目草稿，不直接入库。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ImageQuestionDraft {

    /**
     * 题目标题。
     */
    private String title;

    /**
     * Markdown 题面。
     */
    private String description;

    /**
     * 输入格式说明。
     */
    private String inputDesc;

    /**
     * 输出格式说明。
     */
    private String outputDesc;

    /**
     * 难度：1 入门、2 进阶、3 挑战。
     */
    private Integer difficulty;

    /**
     * 知识点编码，例如 array/string/linked_list/recursion/sort。
     */
    private String knowledgePointCode;

    /**
     * 知识点名称，仅用于展示。
     */
    private String knowledgePointName;

    /**
     * 识别出的样例。
     */
    private List<ImageDraftSample> samples = new ArrayList<>();

    /**
     * 模型自评置信度 0~1。
     */
    private Double confidence;

    /**
     * 服务端计算的缺失字段。
     */
    private List<String> missingFields = new ArrayList<>();
}

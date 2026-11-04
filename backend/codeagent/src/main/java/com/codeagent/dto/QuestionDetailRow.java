package com.codeagent.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 详情 SQL 的扁平结果，service 再组装成 QuestionDetailVO。
 */
@Data
public class QuestionDetailRow {

    private Long id;
    private String title;
    private String description;
    private String inputDesc;
    private String outputDesc;
    private Integer difficulty;
    private Long knowledgePointId;
    private String templateJson;
    private String type;
    private Long userId;
    private String sourceImageUrl;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String knowledgePointCode;
    private String knowledgePointName;
}

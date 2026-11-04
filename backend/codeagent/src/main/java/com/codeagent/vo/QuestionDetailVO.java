package com.codeagent.vo;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 题目详情。
 * 注意：这里刻意没有 testCases 字段，隐藏用例只能留在 question_test_case 表中。
 */
@Data
public class QuestionDetailVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String title;
    private String description;
    private String inputDesc;
    private String outputDesc;
    private Integer difficulty;
    private Long knowledgePointId;
    private KnowledgePointBriefVO knowledgePoint;
    private JsonNode templateJson;
    private String type;
    private Long userId;
    private String sourceImageUrl;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Boolean solved;
    private Long lastSubmitId;
    private List<TestCaseSampleVO> samples;
}

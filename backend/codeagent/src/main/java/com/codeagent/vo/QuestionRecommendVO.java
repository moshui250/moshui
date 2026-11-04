package com.codeagent.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 同类题推荐项。
 */
@Data
public class QuestionRecommendVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String title;
    private Integer difficulty;
    private Long knowledgePointId;
    private String knowledgePointName;
    private Boolean solved;
    private String reason;
}

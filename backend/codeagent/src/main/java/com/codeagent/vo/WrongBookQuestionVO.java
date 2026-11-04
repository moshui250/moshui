package com.codeagent.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 错题项内嵌的题目摘要。
 */
@Data
public class WrongBookQuestionVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 题目 ID。 */
    private Long id;

    /** 题目标题。 */
    private String title;

    /** 难度 1/2/3。 */
    private Integer difficulty;

    /** 题目所属知识点。 */
    private KnowledgePointBriefVO knowledgePoint;
}

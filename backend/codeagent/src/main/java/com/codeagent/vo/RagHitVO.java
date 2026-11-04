package com.codeagent.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * RAG 检索命中项。
 */
@Data
@AllArgsConstructor
public class RagHitVO {

    /** 命中块标题。 */
    private String title;

    /** 向量相似度分数。 */
    private Double score;

    /** 来源类型。 */
    private String sourceType;

    /** 来源主键。 */
    private Long sourceId;
}

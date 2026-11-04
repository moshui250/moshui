package com.codeagent.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * AI 讲解引用来源。
 */
@Data
@AllArgsConstructor
public class AiExplainSourceVO {

    /** 来源标题。 */
    private String title;

    /** 相似度分数；关键词降级时为 0。 */
    private Double score;

    /** 来源类型：SOLUTION / KNOWLEDGE / ERROR_MANUAL / STATEMENT。 */
    private String sourceType;
}

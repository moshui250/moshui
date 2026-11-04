package com.codeagent.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * AI 讲解响应。
 */
@Data
@AllArgsConstructor
public class AiExplainVO {

    /** 模型回答，末尾包含参考来源。 */
    private String answer;

    /** 引用来源列表，按相关度从高到低。 */
    private List<AiExplainSourceVO> sources;

    /** 是否因向量库异常降级为关键词检索。 */
    private boolean degraded;
}

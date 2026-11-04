package com.codeagent.ai.rag;

import lombok.Builder;
import lombok.Data;

/**
 * 待入库的一篇逻辑文档（题解、知识点、错误手册段落或题面）。
 */
@Data
@Builder
public class RagSourceDocument {

    /** 来源类型：SOLUTION / KNOWLEDGE / ERROR_MANUAL / STATEMENT。 */
    private String sourceType;

    /** 来源主键；ERROR_MANUAL 固定为 0。 */
    private Long sourceId;

    /** 文档标题，用于展示与切块回退标题。 */
    private String title;

    /** 关联知识点 ID，没有关联时为 null。 */
    private Long knowledgePointId;

    /** 关联知识点编码，没有关联时为空串。 */
    private String knowledgePointCode;

    /** 原始 Markdown 内容。 */
    private String content;
}

package com.codeagent.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 看板中单个知识点的提交与错误率。
 */
@Data
public class KnowledgeStatVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 知识点 ID。 */
    private Long knowledgePointId;

    /** 知识点名称。 */
    private String name;

    /** 该知识点下的提交数。 */
    private long submitCount;

    /** 该知识点下的 AC 提交数。 */
    private long acCount;

    /** 错误率，取值 0~1；无提交时为 0，前端依据 hasData 显示“—”。 */
    private double errorRate;

    /** 是否有提交数据；false 时图表留空而不是画 0。 */
    private boolean hasData;
}

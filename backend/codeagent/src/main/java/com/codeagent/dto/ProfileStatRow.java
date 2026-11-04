package com.codeagent.dto;

import lombok.Data;

/**
 * 看板统计的扁平查询行：一行对应一个知识点，同时携带该用户的整体错题数。
 */
@Data
public class ProfileStatRow {

    /** 知识点 ID。 */
    private Long knowledgePointId;

    /** 知识点名称。 */
    private String knowledgePointName;

    /** 该知识点下的提交数。 */
    private Long submitCount;

    /** 该知识点下的 AC 提交数。 */
    private Long acCount;

    /** 该知识点下有过 AC 的不同题目数。 */
    private Long solvedCount;

    /** 当前用户未掌握的错题数，同一查询的每一行都会重复返回。 */
    private Long wrongCount;
}

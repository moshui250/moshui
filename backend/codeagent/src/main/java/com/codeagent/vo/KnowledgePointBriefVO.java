package com.codeagent.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 题目详情内嵌的知识点摘要。
 */
@Data
public class KnowledgePointBriefVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String code;
    private String name;
}

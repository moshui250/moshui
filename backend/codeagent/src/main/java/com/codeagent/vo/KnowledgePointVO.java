package com.codeagent.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 知识点列表项：questionCount 只统计 PUBLIC 且未删除的题目。
 */
@Data
public class KnowledgePointVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String code;
    private String name;
    private String description;
    private Long questionCount;
}

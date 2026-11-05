package com.codeagent.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 题库列表项。solved 由 SQL 中的 EXISTS 子查询一次算出。
 */
@Data
public class QuestionListItemVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String title;
    private Integer difficulty;
    private Long knowledgePointId;
    private String knowledgePointName;
    private String type;
    private String visibility;
    private Long userId;
    private LocalDateTime createTime;
    private Boolean solved;
}

package com.codeagent.entity;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class KnowledgePoint implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String code;

    private String name;

    private String description;

    private Integer sort;

    private LocalDateTime createTime;
}

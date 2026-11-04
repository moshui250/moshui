package com.codeagent.entity;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class Question implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String title;

    private String description;

    private String inputDesc;

    private String outputDesc;

    private Integer difficulty;

    private Long knowledgePointId;

    private String templateJson;

    private String type;

    private Long userId;

    private String sourceImageUrl;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer deleted;
}

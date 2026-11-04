package com.codeagent.entity;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class RagDocument implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String sourceType;

    private Long sourceId;

    private String title;

    private Long knowledgePointId;

    private Integer chunkCount;

    private String vectorIds;

    private String contentHash;

    private String status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}

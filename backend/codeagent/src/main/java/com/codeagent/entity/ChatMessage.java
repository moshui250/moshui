package com.codeagent.entity;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class ChatMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long sessionId;

    private Long userId;

    private Long questionId;

    private Integer seq;

    private String role;

    private String content;

    private String toolName;

    private String toolInput;

    private String toolOutput;

    private Integer hintLevel;

    private LocalDateTime createTime;
}

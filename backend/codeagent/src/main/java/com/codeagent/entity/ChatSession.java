package com.codeagent.entity;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class ChatSession implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long userId;

    private Long questionId;

    private String title;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}

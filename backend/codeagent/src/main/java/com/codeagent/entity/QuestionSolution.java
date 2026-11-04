package com.codeagent.entity;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class QuestionSolution implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long questionId;

    private String language;

    private String content;

    private String explanation;

    private LocalDateTime createTime;
}

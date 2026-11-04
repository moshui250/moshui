package com.codeagent.entity;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class UserWrongQuestion implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long userId;

    private Long questionId;

    private Integer wrongCount;

    private Long lastSubmitId;

    private Integer mastered;

    private LocalDateTime masteredTime;

    private LocalDateTime firstWrongTime;

    private LocalDateTime lastWrongTime;
}

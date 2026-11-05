package com.codeagent.entity;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class SubmitRecord implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long userId;

    private Long questionId;

    /** 竞赛 ID；普通练习提交为 null。 */
    private Long contestId;

    private String language;

    private String code;

    private String status;

    private Integer passedCount;

    private Integer totalCount;

    private Long runTimeMs;

    private String compileOutput;

    private String errorMessage;

    private String caseResultJson;

    private LocalDateTime createTime;
}

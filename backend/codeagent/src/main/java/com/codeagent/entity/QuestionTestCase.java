package com.codeagent.entity;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class QuestionTestCase implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long questionId;

    private String inputData;

    private String expectedOutput;

    private Integer isSample;

    private Integer score;

    private Integer sortNo;
}

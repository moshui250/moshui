package com.codeagent.dto;

import lombok.Data;

/**
 * 当前用户在某题上的状态：最近一次提交 + 是否 AC。
 */
@Data
public class QuestionUserState {

    private Long lastSubmitId;
    private Boolean solved;
}

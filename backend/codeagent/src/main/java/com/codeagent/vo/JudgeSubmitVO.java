package com.codeagent.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 提交判题后的即时响应。
 */
@Data
@AllArgsConstructor
public class JudgeSubmitVO {

    /** 新建的提交记录 ID。 */
    private Long submitId;

    /** 当前状态，刚提交时为 PENDING。 */
    private String status;
}

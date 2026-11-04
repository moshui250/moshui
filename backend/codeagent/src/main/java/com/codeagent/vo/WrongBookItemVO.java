package com.codeagent.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 错题本列表项。
 */
@Data
public class WrongBookItemVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 错题记录 ID。 */
    private Long id;

    /** 题目摘要。 */
    private WrongBookQuestionVO question;

    /** 累计答错次数。 */
    private Integer wrongCount;

    /** 是否已掌握。 */
    private Boolean mastered;

    /** 当前用户是否已经 AC 过该题，用于前端展示“已通过”标识。 */
    private Boolean passed;

    /** 最近一次答错时间。 */
    private LocalDateTime lastWrongTime;

    /** 最近一次错误提交；历史数据缺失时可能为 null。 */
    private WrongBookLastSubmitVO lastSubmit;
}

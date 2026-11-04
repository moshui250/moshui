package com.codeagent.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 看板“最近 5 次提交”列表项。
 */
@Data
public class RecentSubmitVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 提交记录 ID。 */
    private Long id;

    /** 题目 ID。 */
    private Long questionId;

    /** 题目标题。 */
    private String questionTitle;

    /** 提交语言：CPP / JAVA。 */
    private String language;

    /** 判题状态。 */
    private String status;

    /** 运行耗时，单位毫秒。 */
    private Integer runTimeMs;

    /** 提交时间。 */
    private LocalDateTime createTime;
}

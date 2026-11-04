package com.codeagent.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 提交历史列表中的一条记录。
 */
@Data
public class JudgeHistoryVO {

    /** 提交记录 ID。 */
    private Long id;

    /** 提交语言 CPP/JAVA。 */
    private String language;

    /** 判题状态。 */
    private String status;

    /** 最大单用例耗时，单位毫秒。 */
    private Long runTime;

    /** 提交时间。 */
    private LocalDateTime createTime;
}

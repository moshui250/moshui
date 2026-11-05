package com.codeagent.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 首页周统计的数据库行。
 */
@Data
public class HomeDailyRow {

    /** 统计日期。 */
    private LocalDate day;

    /** 当日提交数。 */
    private Long submitCount;

    /** 当日通过数。 */
    private Long acCount;
}

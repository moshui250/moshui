package com.codeagent.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 首页近七天中某一天的提交统计。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HomeDailyVO {

    /** 日期，格式 MM-dd。 */
    private String date;

    /** 周几，例如 周一。 */
    private String label;

    /** 提交数。 */
    private long submitCount;

    /** 通过数。 */
    private long acCount;
}

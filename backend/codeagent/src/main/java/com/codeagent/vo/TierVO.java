package com.codeagent.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 单个段位的展示信息。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TierVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 段位序号，0 为未初始化。 */
    private int code;

    /** 段位名称。 */
    private String name;

    /** 段位主题色。 */
    private String color;

    /** 该段位最低 AC 题数。 */
    private long minAc;

    /** 该段位最高 AC 题数，最高段位为 Long.MAX_VALUE。 */
    private long maxAc;

    /** 区间文案，例如 70 - 129 题。 */
    private String rangeText;
}

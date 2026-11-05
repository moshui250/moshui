package com.codeagent.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 当前段位与下一段位的进度。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TierProgressVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 当前段位。 */
    private TierVO tier;

    /** 当前唯一 AC 题数。 */
    private long currentAc;

    /** 下一段位门槛；最高段位为 null。 */
    private Long targetAc;

    /** 下一段位名称；最高段位为 null。 */
    private String nextTierName;

    /** 是否已达最高段位。 */
    private boolean maxTier;

    /** 当前段位到下一段位的完成比例，0 - 1。 */
    private double percent;
}

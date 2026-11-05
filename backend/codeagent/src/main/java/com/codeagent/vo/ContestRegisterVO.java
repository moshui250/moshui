package com.codeagent.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 竞赛报名结果。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContestRegisterVO {

    private Boolean registered;

    private Integer participantCount;
}

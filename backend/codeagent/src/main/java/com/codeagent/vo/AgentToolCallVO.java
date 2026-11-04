package com.codeagent.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 一次 Agent 工具调用，供前端展示调试时间线。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentToolCallVO {

    /** 工具名，例如 locate_error。 */
    private String name;

    /** 工具调用摘要。 */
    private String summary;

    /** 耗时，毫秒。 */
    private Long latencyMs;

    /** 是否调用成功（有输出且无异常）。 */
    private Boolean success;
}

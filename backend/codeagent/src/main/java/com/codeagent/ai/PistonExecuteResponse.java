package com.codeagent.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Piston /execute 的响应，编译和运行两个阶段都可能存在。
 *
 * @param compile 编译阶段结果，无编译步骤时可能为 null
 * @param run     运行阶段结果，编译失败时通常为 null
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PistonExecuteResponse(Stage compile, Stage run) {

    /**
     * 编译或运行阶段的标准输出、错误输出和退出信息。
     *
     * @param stdout 标准输出
     * @param stderr 标准错误
     * @param output Piston 合并后的输出
     * @param code   进程退出码
     * @param signal 终止信号，正常结束时为 null
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Stage(String stdout, String stderr, String output, Integer code, String signal) {
    }
}

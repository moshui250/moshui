package com.codeagent.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 适配后的 go-judge 结果。go-judge 只有一次进程执行，这里把编译失败拆成 compile 阶段，
 * 其余情况拆成 run 阶段，便于判题服务复用原来的状态归并逻辑。
 *
 * @param compile 编译阶段；编译成功时为 null
 * @param run     运行阶段；编译失败时为 null
 * @param status  go-judge 原始状态
 * @param timeMs  本次进程 CPU 时间，单位毫秒
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GoJudgeExecuteResponse(Stage compile, Stage run, String status, long timeMs) {

    /**
     * 编译或运行阶段的输出和退出信息。
     *
     * @param stdout 标准输出
     * @param stderr 标准错误
     * @param output 合并后的输出
     * @param code   退出码
     * @param signal 终止信号，正常结束时为 null
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Stage(String stdout, String stderr, String output, Integer code, String signal) {
    }
}

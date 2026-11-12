package com.codeagent.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

/**
 * go-judge /run 返回的单条执行结果。接口实际返回数组，
 * 一次请求只提交一个 cmd，因此客户端取数组第一条。
 *
 * @param status      go-judge 状态，例如 Accepted / Time Limit Exceeded
 * @param exitStatus  退出码
 * @param time        CPU 时间，单位纳秒
 * @param memory      峰值内存，单位字节
 * @param runTime     墙钟时间，单位纳秒
 * @param files       copyOut 收集到的文件
 * @param error       沙箱内部错误信息，正常时为空
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GoJudgeResponse(String status,
                              Integer exitStatus,
                              Long time,
                              Long memory,
                              Long runTime,
                              Map<String, String> files,
                              String error) {
}

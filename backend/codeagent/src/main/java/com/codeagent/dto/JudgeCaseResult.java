package com.codeagent.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

/**
 * 判题过程中单个用例的结果，内部保存并序列化到 submit_record.case_result_json。
 */
@Data
public class JudgeCaseResult {

    /** 用例序号，从 1 开始。 */
    private int index;

    /** 是否为公开样例。 */
    private boolean sample;

    /** 是否通过。 */
    private boolean passed;

    /** 是否为隐藏用例；仅内部使用，不写入 JSON。 */
    @JsonIgnore
    private boolean hidden;

    /** 用例输入；隐藏用例通过时为空。 */
    private String input;

    /** 期望输出；隐藏用例通过时为空。 */
    private String expected;

    /** 实际输出；隐藏用例通过时为空。 */
    private String actual;

    /** 标准错误；隐藏用例不返回。 */
    private String stderr;

    /** 该用例运行耗时，单位毫秒。 */
    private long timeMs;
}

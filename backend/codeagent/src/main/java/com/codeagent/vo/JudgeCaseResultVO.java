package com.codeagent.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * 返回给前端的单个用例结果。隐藏用例通过时，输入/期望/实际字段为 null 并被省略。
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JudgeCaseResultVO {

    /** 用例序号，从 1 开始。 */
    private int index;

    /** 是否为公开样例。 */
    private boolean sample;

    /** 是否通过。 */
    private boolean passed;

    /** 是否为用户临时添加的样例。 */
    private Boolean custom;

    /** 用例输入；隐藏用例通过时为 null。 */
    private String input;

    /** 期望输出；隐藏用例通过时为 null。 */
    private String expected;

    /** 实际输出；隐藏用例通过时为 null。 */
    private String actual;

    /** 标准错误；隐藏用例为 null。 */
    private String stderr;

    /** 该用例运行耗时，单位毫秒。 */
    private Long timeMs;
}

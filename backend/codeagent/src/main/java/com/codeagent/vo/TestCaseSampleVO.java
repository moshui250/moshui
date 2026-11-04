package com.codeagent.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 可下发的样例用例。只允许 is_sample=1 的记录进入该 VO。
 */
@Data
public class TestCaseSampleVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String inputData;
    private String expectedOutput;
    private Integer sortNo;
}

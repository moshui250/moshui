package com.codeagent.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 错题项内嵌的最近一次错误提交摘要。
 */
@Data
public class WrongBookLastSubmitVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 提交记录 ID。 */
    private Long id;

    /** 判题状态，例如 WA / CE / TLE。 */
    private String status;

    /** 代码片段，最多 200 个字符。 */
    private String codeSnippet;
}

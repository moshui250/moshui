package com.codeagent.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * 图片识别草稿中的一条样例。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ImageDraftSample {

    /**
     * 样例输入。
     */
    @JsonAlias("inputData")
    private String input;

    /**
     * 样例输出。
     */
    @JsonAlias("expectedOutput")
    private String output;
}

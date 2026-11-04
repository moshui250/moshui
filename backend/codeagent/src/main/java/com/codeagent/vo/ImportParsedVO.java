package com.codeagent.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 图片草稿入库结果。
 */
@Data
@AllArgsConstructor
public class ImportParsedVO {

    /**
     * 新建的个人题 ID。
     */
    private Long questionId;

    /**
     * 是否用样例复制了隐藏用例，需要用户后续人工确认。
     */
    private boolean needManualConfirm;

    /**
     * 面向用户的说明。
     */
    private String message;
}

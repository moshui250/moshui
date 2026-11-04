package com.codeagent.vo;

import com.codeagent.dto.ImageQuestionDraft;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 图片识别结果。草稿只存在内存缓存中，正式题目尚未入库。
 */
@Data
public class ParseImageVO {

    /**
     * 后续 import-parsed 使用的缓存 ID。
     */
    private String parseId;

    /**
     * 识别出的题目草稿。
     */
    private ImageQuestionDraft draft;

    /**
     * 缺失字段，例如“输入格式”“输出格式”。
     */
    private List<String> missingFields = new ArrayList<>();

    /**
     * 面向用户的补全提示，没有缺失时为 null。
     */
    private String warn;

    /**
     * 压缩后图片的访问地址。
     */
    private String imageUrl;
}

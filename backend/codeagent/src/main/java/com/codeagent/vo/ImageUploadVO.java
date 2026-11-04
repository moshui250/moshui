package com.codeagent.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 图片上传结果。
 */
@Data
@AllArgsConstructor
public class ImageUploadVO {

    /**
     * 压缩后图片的访问地址。
     */
    private String url;
}

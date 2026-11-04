package com.codeagent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 图片上传目录配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "file")
public class FileStorageProperties {

    /**
     * 图片存储根目录，默认 /data/upload；生产环境可通过 FILE_UPLOAD_DIR 覆盖。
     */
    private String uploadDir = "/data/upload";
}

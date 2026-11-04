package com.codeagent.entity;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class FileObject implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long userId;

    private String url;

    private String path;

    private String mime;

    private Long sizeBytes;

    private LocalDateTime createTime;
}

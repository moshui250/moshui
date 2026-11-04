package com.codeagent.controller;

import com.codeagent.common.R;
import com.codeagent.service.FileStorageService;
import com.codeagent.service.ImageProcessor;
import com.codeagent.vo.ImageUploadVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 图片上传接口。
 */
@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class FileController {

    private final ImageProcessor imageProcessor;
    private final FileStorageService fileStorageService;

    /**
     * 校验并压缩图片，返回可访问的图片 URL。
     *
     * @param file multipart 图片文件，最大 5MB
     * @return 压缩后的图片地址
     */
    @PostMapping("/image")
    public R<ImageUploadVO> upload(@RequestParam("file") MultipartFile file) {
        ImageProcessor.PreparedImage prepared = imageProcessor.prepare(file);
        FileStorageService.StoredImage stored = fileStorageService.storeJpeg(prepared.bytes());
        return R.ok(new ImageUploadVO(stored.url()));
    }
}

package com.codeagent.service;

import com.codeagent.common.ErrorCode;
import com.codeagent.config.FileStorageProperties;
import com.codeagent.exception.BizException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.UUID;

/**
 * 本地图片存储。文件按年/月分目录，返回可直接访问的 /uploads URL。
 */
@Service
public class FileStorageService {

    private final Path root;

    public FileStorageService(FileStorageProperties properties) {
        this.root = Path.of(properties.getUploadDir()).toAbsolutePath().normalize();
    }

    /**
     * 保存一张已经压缩好的 JPEG。
     *
     * @param jpeg 压缩后的 JPEG 字节
     * @return 可访问 URL 与磁盘路径
     */
    public StoredImage storeJpeg(byte[] jpeg) {
        LocalDate today = LocalDate.now();
        String year = String.valueOf(today.getYear());
        String month = String.format("%02d", today.getMonthValue());
        String filename = UUID.randomUUID().toString().replace("-", "") + ".jpg";
        Path directory = root.resolve(year).resolve(month);
        Path target = directory.resolve(filename).normalize();
        if (!target.startsWith(root)) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "图片存储路径非法");
        }
        try {
            Files.createDirectories(directory);
            Files.write(target, jpeg, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        } catch (IOException e) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "图片保存失败");
        }
        return new StoredImage("/uploads/" + year + "/" + month + "/" + filename, target);
    }

    /**
     * 返回上传根目录，供静态资源映射使用。
     *
     * @return 规范化后的绝对目录
     */
    public Path getRoot() {
        return root;
    }

    /**
     * 已保存图片的信息。
     *
     * @param url  浏览器访问路径
     * @param path 磁盘绝对路径
     */
    public record StoredImage(String url, Path path) {
    }
}

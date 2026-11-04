package com.codeagent.service;

import com.codeagent.common.ErrorCode;
import com.codeagent.exception.BizException;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * 图片校验与压缩。扩展名不可信，先校验文件头，再交给 ImageIO/Thumbnailator。
 */
@Service
public class ImageProcessor {

    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final int MAX_EDGE = 1600;

    /**
     * 校验并压缩图片，统一输出 JPEG（质量 0.85，长边不超过 1600px）。
     *
     * @param file 上传的原始图片
     * @return 压缩后的 JPEG 字节及尺寸
     */
    public PreparedImage prepare(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "请选择图片");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BizException(ErrorCode.PARAM_ERROR, "图片不能超过 5MB");
        }

        byte[] original;
        try {
            original = file.getBytes();
        } catch (IOException e) {
            throw new BizException(ErrorCode.PARAM_ERROR, "图片读取失败");
        }
        if (!isSupported(original)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "仅支持 JPG、PNG、WebP 图片");
        }

        BufferedImage source;
        try {
            source = ImageIO.read(new ByteArrayInputStream(original));
        } catch (IOException e) {
            throw new BizException(ErrorCode.PARAM_ERROR, "图片格式无法解析");
        }
        if (source == null || source.getWidth() <= 0 || source.getHeight() <= 0) {
            throw new BizException(ErrorCode.PARAM_ERROR, "图片格式无法解析");
        }

        BufferedImage rgb = flatten(source);
        double scale = Math.min(1.0, (double) MAX_EDGE / Math.max(rgb.getWidth(), rgb.getHeight()));
        int width = Math.max(1, (int) Math.round(rgb.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(rgb.getHeight() * scale));

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Thumbnails.of(rgb)
                    .scale(scale)
                    .outputFormat("jpg")
                    .outputQuality(0.85)
                    .toOutputStream(out);
            return new PreparedImage(out.toByteArray(), width, height);
        } catch (IOException e) {
            throw new BizException(ErrorCode.PARAM_ERROR, "图片压缩失败");
        }
    }

    /**
     * 把带透明通道的图片铺到白底上，避免转 JPEG 后透明区域变黑。
     *
     * @param source 原始图片
     * @return RGB 图片
     */
    private BufferedImage flatten(BufferedImage source) {
        BufferedImage rgb = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = rgb.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, rgb.getWidth(), rgb.getHeight());
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.drawImage(source, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return rgb;
    }

    /**
     * 通过 magic bytes 判断真实图片类型，忽略文件扩展名。
     *
     * @param bytes 文件字节
     * @return 是否属于允许的 JPG/PNG/WebP
     */
    private boolean isSupported(byte[] bytes) {
        if (bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xFF
                && (bytes[1] & 0xFF) == 0xD8
                && (bytes[2] & 0xFF) == 0xFF) {
            return true;
        }
        if (bytes.length >= 8
                && (bytes[0] & 0xFF) == 0x89
                && bytes[1] == 0x50
                && bytes[2] == 0x4E
                && bytes[3] == 0x47) {
            return true;
        }
        return bytes.length >= 12
                && bytes[0] == 'R'
                && bytes[1] == 'I'
                && bytes[2] == 'F'
                && bytes[3] == 'F'
                && bytes[8] == 'W'
                && bytes[9] == 'E'
                && bytes[10] == 'B'
                && bytes[11] == 'P';
    }

    /**
     * 压缩后的图片数据。
     *
     * @param bytes  JPEG 字节
     * @param width  压缩后的宽度
     * @param height 压缩后的高度
     */
    public record PreparedImage(byte[] bytes, int width, int height) {
    }
}

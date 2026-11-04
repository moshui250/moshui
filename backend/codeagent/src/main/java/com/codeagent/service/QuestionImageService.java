package com.codeagent.service;

import com.codeagent.dto.ImportParsedRequest;
import com.codeagent.vo.ImportParsedVO;
import com.codeagent.vo.ParseImageVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 图片录题的两段式服务：先识别草稿，再由用户确认入库。
 */
public interface QuestionImageService {

    /**
     * 识别图片并写入 10 分钟内存缓存，不落库。
     *
     * @param file     上传的图片
     * @param imageUrl 已上传图片的 URL，可为空
     * @param userId   当前用户 ID
     * @return 草稿、parseId、缺失字段和提示
     */
    ParseImageVO parse(MultipartFile file, String imageUrl, Long userId);

    /**
     * 用户确认草稿后创建 PERSONAL 题目。
     *
     * @param request 用户修改后的字段和 parseId
     * @param userId  当前用户 ID
     * @return 新题 ID 及是否需要人工确认隐藏用例
     */
    ImportParsedVO importParsed(ImportParsedRequest request, Long userId);
}

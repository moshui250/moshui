package com.codeagent.controller;

import com.codeagent.common.R;
import com.codeagent.common.UserContext;
import com.codeagent.dto.ImportParsedRequest;
import com.codeagent.service.QuestionImageService;
import com.codeagent.vo.ImportParsedVO;
import com.codeagent.vo.ParseImageVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 图片录题的识别与确认入库接口。
 */
@RestController
@RequestMapping("/api/question")
@RequiredArgsConstructor
public class QuestionImageController {

    private final QuestionImageService questionImageService;

    /**
     * 识别题目图片，返回草稿和 parseId；结果只在内存缓存中保留 10 分钟。
     *
     * @param file     图片文件，最大 5MB
     * @param imageUrl 已通过 /api/file/image 上传的图片地址，可为空
     * @return 草稿、缺失字段、提示和图片地址
     */
    @PostMapping("/parse-image")
    public R<ParseImageVO> parseImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "imageUrl", required = false) String imageUrl) {
        return R.ok(questionImageService.parse(file, imageUrl, UserContext.getUserId()));
    }

    /**
     * 用户核对/修改草稿后创建个人题目。
     *
     * @param request 草稿字段和 parseId
     * @return 新题 ID、隐藏用例提示
     */
    @PostMapping("/import-parsed")
    public R<ImportParsedVO> importParsed(@Valid @RequestBody ImportParsedRequest request) {
        return R.ok(questionImageService.importParsed(request, UserContext.getUserId()));
    }
}

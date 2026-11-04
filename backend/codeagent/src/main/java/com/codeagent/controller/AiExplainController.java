package com.codeagent.controller;

import com.codeagent.common.R;
import com.codeagent.common.UserContext;
import com.codeagent.dto.AiExplainRequest;
import com.codeagent.service.AiExplainService;
import com.codeagent.vo.AiExplainVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 讲解接口。
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiExplainController {

    private final AiExplainService aiExplainService;

    /**
     * 基于题库与错误手册回答用户问题，并保存对话记录。
     *
     * @param request 题目 ID 和用户问题
     * @return 回答、引用来源与降级标记
     */
    @PostMapping("/explain")
    public R<AiExplainVO> explain(@Valid @RequestBody AiExplainRequest request) {
        return R.ok(aiExplainService.explain(UserContext.getUserId(), request));
    }
}

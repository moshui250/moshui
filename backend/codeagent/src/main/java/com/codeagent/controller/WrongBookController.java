package com.codeagent.controller;

import com.codeagent.common.PageResult;
import com.codeagent.common.R;
import com.codeagent.dto.MasteredRequest;
import com.codeagent.service.WrongBookService;
import com.codeagent.vo.WrongBookItemVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 错题本相关接口。
 */
@RestController
@RequestMapping("/api/wrong")
@RequiredArgsConstructor
public class WrongBookController {

    private final WrongBookService wrongBookService;

    /**
     * 分页查询当前用户的错题本，默认只查未掌握的记录。
     *
     * @param page             页码，从 1 开始
     * @param size             每页条数，服务端限制为 1~50
     * @param mastered         是否已掌握；null 或 false 查未掌握，true 查已掌握
     * @param knowledgePointId 知识点 ID，null 表示不限
     * @return 分页后的错题列表
     */
    @GetMapping("/list")
    public R<PageResult<WrongBookItemVO>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Boolean mastered,
            @RequestParam(required = false) Long knowledgePointId) {
        return R.ok(wrongBookService.list(page, size, mastered, knowledgePointId));
    }

    /**
     * 标记或取消标记一道错题。
     *
     * @param questionId 题目 ID，和当前用户组成错题记录的唯一键
     * @param request    目标掌握状态
     * @return 空响应；错题记录不存在时返回 40400
     */
    @PutMapping("/{questionId}/mastered")
    public R<Void> mastered(@PathVariable Long questionId,
                            @Valid @RequestBody MasteredRequest request) {
        wrongBookService.updateMastered(questionId, request.getMastered());
        return R.ok();
    }
}

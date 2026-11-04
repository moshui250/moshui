package com.codeagent.controller;

import com.codeagent.common.PageResult;
import com.codeagent.common.R;
import com.codeagent.dto.QuestionSaveRequest;
import com.codeagent.service.QuestionService;
import com.codeagent.vo.QuestionDetailVO;
import com.codeagent.vo.QuestionListItemVO;
import com.codeagent.vo.QuestionRecommendVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/question")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    /**
     * 分页查询题目列表。PUBLIC 返回公开题，PERSONAL 只返回当前登录用户创建的个人题。
     *
     * @param page             页码，从 1 开始
     * @param size             每页条数，服务端限制为 1~50
     * @param knowledgePointId 知识点 ID，null 表示不限
     * @param difficulty       难度 1/2/3，null 表示不限
     * @param type             题目类型 PUBLIC/PERSONAL，默认 PUBLIC
     * @param keyword          标题关键字，null 或空串表示不限
     * @return 分页后的题目列表
     */
    @GetMapping("/list")
    public R<PageResult<QuestionListItemVO>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Long knowledgePointId,
            @RequestParam(required = false) Integer difficulty,
            @RequestParam(defaultValue = "PUBLIC") String type,
            @RequestParam(required = false) String keyword) {
        return R.ok(questionService.list(page, size, knowledgePointId, difficulty, type, keyword));
    }

    /**
     * 推荐与当前题同知识点或难度接近的公开题。
     *
     * @param questionId 当前题 ID，用于定位知识点和难度并排除自身
     * @param limit      推荐数量，默认 3，服务端限制为 1~50
     * @return 推荐题目列表
     */
    @GetMapping("/recommend")
    public R<List<QuestionRecommendVO>> recommend(
            @RequestParam Long questionId,
            @RequestParam(defaultValue = "3") Integer limit) {
        return R.ok(questionService.recommend(questionId, limit));
    }

    /**
     * 查询题目详情，只返回公开样例用例，不返回隐藏用例。
     *
     * @param id 题目 ID
     * @return 题目详情
     */
    @GetMapping("/{id}")
    public R<QuestionDetailVO> detail(@PathVariable Long id) {
        return R.ok(questionService.detail(id));
    }

    /**
     * 创建个人题，题目自动归属当前登录用户。
     *
     * @param request 题目基本信息、用例和可选参考代码
     * @return 新建题目 ID
     */
    @PostMapping
    public R<Long> create(@Valid @RequestBody QuestionSaveRequest request) {
        return R.ok(questionService.create(request));
    }

    /**
     * 修改当前用户创建的个人题；公开题或他人题目返回 40300。
     *
     * @param id      题目 ID
     * @param request 新的题目信息、用例和参考代码；用例/代码不传则保持原值
     * @return 成功时无数据
     */
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody QuestionSaveRequest request) {
        questionService.update(id, request);
        return R.ok();
    }

    /**
     * 逻辑删除当前用户创建的个人题；公开题或他人题目返回 40300。
     *
     * @param id 题目 ID
     * @return 成功时无数据
     */
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        questionService.delete(id);
        return R.ok();
    }
}

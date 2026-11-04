package com.codeagent.controller;

import com.codeagent.common.ErrorCode;
import com.codeagent.common.PageResult;
import com.codeagent.common.R;
import com.codeagent.common.UserContext;
import com.codeagent.dto.JudgeSubmitRequest;
import com.codeagent.exception.BizException;
import com.codeagent.service.JudgeService;
import com.codeagent.vo.JudgeHistoryVO;
import com.codeagent.vo.JudgeResultVO;
import com.codeagent.vo.JudgeRunSampleVO;
import com.codeagent.vo.JudgeSubmitVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 判题相关接口。
 */
@RestController
@RequestMapping("/api/judge")
@RequiredArgsConstructor
public class JudgeController {

    private final JudgeService judgeService;

    /**
     * 提交代码，立即返回提交记录 ID，判题在后台线程池中执行。
     *
     * @param request 题目 ID、语言和完整代码
     * @return 提交记录 ID 和初始状态 PENDING
     */
    @PostMapping("/submit")
    public R<JudgeSubmitVO> submit(@Valid @RequestBody JudgeSubmitRequest request) {
        Long submitId = judgeService.submit(
                requireUserId(), request.getQuestionId(), request.getLanguage(), request.getCode());
        return R.ok(new JudgeSubmitVO(submitId, "PENDING"));
    }

    /**
     * 只运行公开样例，不创建提交记录，用于答题页的“运行样例”按钮。
     *
     * @param request 题目 ID、语言和完整代码
     * @return 样例运行结果
     */
    @PostMapping("/run-sample")
    public R<JudgeRunSampleVO> runSample(@Valid @RequestBody JudgeSubmitRequest request) {
        return R.ok(judgeService.runSample(
                requireUserId(), request.getQuestionId(), request.getLanguage(), request.getCode()));
    }

    /**
     * 查询判题结果，前端通过轮询此接口获取最终状态。
     *
     * @param submitId 提交记录 ID
     * @return 判题状态和逐用例结果
     */
    @GetMapping("/result/{submitId}")
    public R<JudgeResultVO> result(@PathVariable Long submitId) {
        return R.ok(judgeService.result(submitId, requireUserId()));
    }

    /**
     * 分页查询当前用户的提交历史，可按题目过滤。
     *
     * @param questionId 题目 ID，null 表示全部题目
     * @param page       页码，从 1 开始
     * @param size       每页条数，服务端限制为 1~50
     * @return 分页提交历史
     */
    @GetMapping("/history")
    public R<PageResult<JudgeHistoryVO>> history(
            @RequestParam(required = false) Long questionId,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return R.ok(judgeService.history(requireUserId(), questionId, page, size));
    }

    /**
     * 从 ThreadLocal 取当前登录用户 ID，未登录时抛 40100。
     *
     * @return 当前用户 ID
     */
    private Long requireUserId() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return userId;
    }
}

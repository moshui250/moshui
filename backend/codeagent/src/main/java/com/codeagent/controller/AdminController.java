package com.codeagent.controller;

import com.codeagent.common.ErrorCode;
import com.codeagent.common.R;
import com.codeagent.common.UserContext;
import com.codeagent.dto.AdminContestRequest;
import com.codeagent.exception.BizException;
import com.codeagent.service.ContestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端接口。
 * 通过 sys_user.role=ADMIN 判定管理员。
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final ContestService contestService;

    /**
     * 创建竞赛，可混合已有题目和竞赛专属新题。
     *
     * @param request 竞赛信息、已有题目 ID 和竞赛专属新题
     * @return 新建竞赛 ID
     */
    @PostMapping("/contest")
    public R<Long> createContest(@Valid @RequestBody AdminContestRequest request) {
        requireAdmin();
        return R.ok(contestService.createContest(request));
    }

    /**
     * 要求当前登录用户是管理员。
     */
    private void requireAdmin() {
        UserContext.UserInfo user = UserContext.get();
        if (user == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
            throw new BizException(ErrorCode.FORBIDDEN, "仅管理员可以创建竞赛");
        }
    }
}

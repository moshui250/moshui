package com.codeagent.controller;

import com.codeagent.common.R;
import com.codeagent.common.UserContext;
import com.codeagent.service.ProfileStatService;
import com.codeagent.vo.ProfileStatVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 学情看板接口。
 */
@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileStatService profileStatService;

    /**
     * 查询当前用户的看板统计，口径见手册 §2.5。
     *
     * @return 总提交、正确率、错题数、知识点错误率和最近 5 次提交
     */
    @GetMapping("/stat")
    public R<ProfileStatVO> stat() {
        return R.ok(profileStatService.get(UserContext.getUserId()));
    }
}

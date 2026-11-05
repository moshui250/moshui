package com.codeagent.controller;

import com.codeagent.common.R;
import com.codeagent.service.HomeSummaryService;
import com.codeagent.vo.HomeSummaryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 首页公开统计接口。
 */
@RestController
@RequestMapping("/api/home")
@RequiredArgsConstructor
public class HomeController {

    private final HomeSummaryService homeSummaryService;

    /**
     * 返回首页总览与本周提交曲线。该接口不要求登录。
     *
     * @return 全站通过率、题目数和本周统计
     */
    @GetMapping("/summary")
    public R<HomeSummaryVO> summary() {
        return R.ok(homeSummaryService.getSummary());
    }
}

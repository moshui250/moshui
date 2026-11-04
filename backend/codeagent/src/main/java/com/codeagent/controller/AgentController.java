package com.codeagent.controller;

import com.codeagent.common.R;
import com.codeagent.common.UserContext;
import com.codeagent.dto.AgentRunRequest;
import com.codeagent.service.AgentService;
import com.codeagent.vo.AgentRunVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 调试 Agent 接口。
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;

    /**
     * 执行一轮带工具调用的 AI 调试。
     *
     * @param request 题目、问题、可选语言和代码
     * @return 回答、工具调用时间线、降级标记和会话 ID
     */
    @PostMapping("/agent")
    public R<AgentRunVO> agent(@Valid @RequestBody AgentRunRequest request) {
        return R.ok(agentService.run(UserContext.getUserId(), request));
    }
}

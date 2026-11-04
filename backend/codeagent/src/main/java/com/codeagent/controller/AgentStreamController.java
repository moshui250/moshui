package com.codeagent.controller;

import com.codeagent.common.R;
import com.codeagent.common.UserContext;
import com.codeagent.dto.AgentStreamRequest;
import com.codeagent.service.AgentStreamService;
import com.codeagent.vo.AgentHistoryVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 流式 Agent 调试接口。
 */
@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class AgentStreamController {

    private final AgentStreamService agentStreamService;

    /**
     * 建立流式调试连接。
     *
     * @param request 调试请求
     * @return SSE 事件流
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@Valid @RequestBody AgentStreamRequest request) {
        return agentStreamService.stream(UserContext.getUserId(), request);
    }

    /**
     * 查询某道题的历史对话，供前端刷新后回放。
     *
     * @param questionId 题目 ID
     * @return 按会话分组的历史
     */
    @GetMapping("/history")
    public R<AgentHistoryVO> history(@RequestParam("questionId") Long questionId) {
        return R.ok(agentStreamService.history(UserContext.getUserId(), questionId));
    }
}

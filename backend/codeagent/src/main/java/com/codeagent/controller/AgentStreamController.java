package com.codeagent.controller;

import com.codeagent.common.ErrorCode;
import com.codeagent.common.R;
import com.codeagent.common.UserContext;
import com.codeagent.dto.AgentSessionRequest;
import com.codeagent.dto.AgentStreamRequest;
import com.codeagent.exception.BizException;
import com.codeagent.service.AgentSessionService;
import com.codeagent.service.AgentStreamService;
import com.codeagent.service.RateLimitService;
import com.codeagent.vo.AgentHistoryVO;
import com.codeagent.vo.AgentSessionVO;
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
    private final AgentSessionService agentSessionService;
    private final RateLimitService rateLimitService;

    /**
     * 创建或复用某道题的 AI 调试会话。
     *
     * @param request 题目 ID
     * @return 会话 ID 和标题
     */
    @PostMapping("/session")
    public R<AgentSessionVO> session(@Valid @RequestBody AgentSessionRequest request) {
        return R.ok(agentSessionService.createOrReuse(requireUserId(), request.getQuestionId()));
    }

    /**
     * 建立流式调试连接。
     *
     * @param request 调试请求
     * @return SSE 事件流
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@Valid @RequestBody AgentStreamRequest request) {
        Long userId = requireUserId();
        rateLimitService.check("agent:stream:" + userId, 20, 3_600_000L,
                "Agent 调试请求过于频繁，请稍后再试");
        return agentStreamService.stream(userId, request);
    }

    /**
     * 查询某道题的历史对话，供前端刷新后回放。
     *
     * @param questionId 题目 ID
     * @return 按会话分组的历史
     */
    @GetMapping("/history")
    public R<AgentHistoryVO> history(@RequestParam("questionId") Long questionId) {
        return R.ok(agentStreamService.history(requireUserId(), questionId));
    }

    /**
     * 从 ThreadLocal 取当前登录用户 ID。
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

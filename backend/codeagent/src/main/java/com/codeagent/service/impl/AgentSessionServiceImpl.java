package com.codeagent.service.impl;

import com.codeagent.common.ErrorCode;
import com.codeagent.dto.QuestionDetailRow;
import com.codeagent.entity.ChatSession;
import com.codeagent.exception.BizException;
import com.codeagent.mapper.ChatSessionMapper;
import com.codeagent.mapper.QuestionMapper;
import com.codeagent.service.AgentSessionService;
import com.codeagent.vo.AgentSessionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AI 调试会话的创建与复用实现。
 */
@Service
@RequiredArgsConstructor
public class AgentSessionServiceImpl implements AgentSessionService {

    private static final int MAX_TITLE_LENGTH = 120;

    private final QuestionMapper questionMapper;
    private final ChatSessionMapper chatSessionMapper;

    /**
     * 校验题目访问权限；已有最近会话时复用，否则新建一条会话。
     *
     * @param userId     当前登录用户 ID
     * @param questionId 题目 ID
     * @return 会话 ID 和标题
     */
    @Override
    @Transactional
    public AgentSessionVO createOrReuse(Long userId, Long questionId) {
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        if (questionId == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "题目 ID 不能为空");
        }

        QuestionDetailRow question = questionMapper.selectDetailRow(questionId);
        if (question == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "题目不存在");
        }
        if ("PERSONAL".equals(question.getType()) && !userId.equals(question.getUserId())) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }

        ChatSession latest = chatSessionMapper.selectLatest(userId, questionId);
        if (latest != null) {
            return new AgentSessionVO(latest.getId(), latest.getTitle());
        }

        String title = buildTitle(question.getTitle());
        ChatSession session = new ChatSession();
        session.setUserId(userId);
        session.setQuestionId(questionId);
        session.setTitle(title);
        if (chatSessionMapper.insert(session) != 1 || session.getId() == null) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "调试会话创建失败");
        }
        return new AgentSessionVO(session.getId(), title);
    }

    /**
     * 生成不超长的会话标题。
     *
     * @param questionTitle 题目标题
     * @return 会话标题
     */
    private String buildTitle(String questionTitle) {
        String base = (questionTitle == null || questionTitle.isBlank() ? "题目" : questionTitle.trim())
                + " · AI 调试";
        return base.length() <= MAX_TITLE_LENGTH ? base : base.substring(0, MAX_TITLE_LENGTH);
    }
}

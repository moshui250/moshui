package com.codeagent.common;

import com.codeagent.dto.QuestionDetailRow;

import java.util.Objects;

/**
 * 题目读取权限：公开题和共享题所有人可读，私有个人题仅作者可读。
 */
public final class QuestionAccess {

    private QuestionAccess() {
    }

    /**
     * 判断用户能否使用该题目的 AI 调试能力。
     *
     * @param question 题目详情行
     * @param userId   当前登录用户 ID
     * @return 允许读取返回 true
     */
    public static boolean canUseAi(QuestionDetailRow question, Long userId) {
        if (question == null) {
            return false;
        }
        if (!"PERSONAL".equalsIgnoreCase(question.getType())) {
            return true;
        }
        if ("SHARED".equalsIgnoreCase(question.getVisibility())
                || "PUBLIC".equalsIgnoreCase(question.getVisibility())) {
            return true;
        }
        return Objects.equals(userId, question.getUserId());
    }
}

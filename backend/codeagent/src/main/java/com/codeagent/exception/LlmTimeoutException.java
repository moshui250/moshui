package com.codeagent.exception;

import com.codeagent.common.ErrorCode;

/**
 * 大模型调用超时。上层可据此返回预置排查话术，而不是直接报错。
 */
public class LlmTimeoutException extends BizException {

    /**
     * @param message 可读的超时说明
     */
    public LlmTimeoutException(String message) {
        super(ErrorCode.AI_UNAVAILABLE, message);
    }
}

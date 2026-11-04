package com.codeagent.exception;

import com.codeagent.common.ErrorCode;
import lombok.Getter;

import java.io.Serial;

/**
 * 可预期的业务异常，由 GlobalExceptionHandler 统一转换。
 */
@Getter
public class BizException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final ErrorCode errorCode;

    public BizException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BizException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}

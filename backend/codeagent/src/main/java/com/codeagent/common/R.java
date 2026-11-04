package com.codeagent.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 统一响应体。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.ALWAYS)
public class R<T> implements Serializable {

    private int code;
    private String message;
    private T data;

    public static <T> R<T> ok() {
        return new R<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), null);
    }

    public static <T> R<T> ok(T data) {
        return new R<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), data);
    }

    public static <T> R<T> fail(ErrorCode errorCode) {
        return new R<>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    public static <T> R<T> fail(ErrorCode errorCode, String message) {
        return new R<>(errorCode.getCode(), message, null);
    }

    public static <T> R<T> fail(int code, String message) {
        return new R<>(code, message, null);
    }
}

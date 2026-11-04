package com.codeagent.common;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 全局业务错误码，和前端约定保持一致。
 */
@Getter
public enum ErrorCode {

    SUCCESS(0, "成功", HttpStatus.OK),
    PARAM_ERROR(40001, "参数校验失败", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED(40100, "未登录或 token 已失效", HttpStatus.UNAUTHORIZED),
    LOGIN_FAILED(40101, "用户名或密码错误", HttpStatus.UNAUTHORIZED),
    FORBIDDEN(40300, "无权访问该资源", HttpStatus.FORBIDDEN),
    NOT_FOUND(40400, "资源不存在", HttpStatus.NOT_FOUND),
    USERNAME_EXISTS(40901, "用户名已存在", HttpStatus.CONFLICT),
    TOO_MANY_REQUESTS(42900, "请求过于频繁，请稍后重试", HttpStatus.TOO_MANY_REQUESTS),
    JUDGE_QUEUE_FULL(42901, "判题队列已满，请稍后重试", HttpStatus.TOO_MANY_REQUESTS),
    INTERNAL_ERROR(50000, "服务器内部错误", HttpStatus.INTERNAL_SERVER_ERROR),
    IMAGE_PARSE_FAILED(50201, "图片识别失败，请手动录入", HttpStatus.BAD_GATEWAY),
    JUDGE_UNAVAILABLE(50301, "判题服务暂时不可用", HttpStatus.SERVICE_UNAVAILABLE),
    AI_UNAVAILABLE(50302, "AI 服务超时或失败", HttpStatus.SERVICE_UNAVAILABLE);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(int code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}

package com.codeagent.exception;

import com.codeagent.common.ErrorCode;
import com.codeagent.common.R;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 统一异常出口，保证所有接口都返回 R 结构。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ResponseEntity<R<Void>> handleBizException(BizException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.warn("业务异常: code={}, message={}", errorCode.getCode(), e.getMessage());
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(R.fail(errorCode, e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<R<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        return badRequest(firstMessage(e.getBindingResult()));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<R<Void>> handleBindException(BindException e) {
        return badRequest(firstMessage(e.getBindingResult()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<R<Void>> handleConstraintViolation(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .findFirst()
                .map(violation -> violation.getMessage())
                .orElse(ErrorCode.PARAM_ERROR.getMessage());
        return badRequest(message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<R<Void>> handleNotReadable(HttpMessageNotReadableException e) {
        return badRequest("请求体格式错误");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<R<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return badRequest("参数 " + e.getName() + " 类型不正确");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<R<Void>> handleMaxUploadSize(MaxUploadSizeExceededException e) {
        return badRequest("上传文件过大，单个文件不能超过 5MB");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<R<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(R.fail(ErrorCode.PARAM_ERROR, "请求方法不支持"));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<R<Void>> handleNoResource(NoResourceFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(R.fail(ErrorCode.NOT_FOUND));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<R<Void>> handleException(Exception e) {
        log.error("未处理异常", e);
        return ResponseEntity.status(ErrorCode.INTERNAL_ERROR.getHttpStatus())
                .body(R.fail(ErrorCode.INTERNAL_ERROR));
    }

    private ResponseEntity<R<Void>> badRequest(String message) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(R.fail(ErrorCode.PARAM_ERROR, message));
    }

    private String firstMessage(BindingResult bindingResult) {
        return bindingResult.getAllErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage() == null ? ErrorCode.PARAM_ERROR.getMessage() : error.getDefaultMessage())
                .orElse(ErrorCode.PARAM_ERROR.getMessage());
    }
}

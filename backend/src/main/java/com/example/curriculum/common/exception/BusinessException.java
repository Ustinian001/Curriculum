package com.example.curriculum.common.exception;

import lombok.Getter;

/**
 * 业务异常，由全局异常处理器统一捕获并返回给前端
 */
@Getter
public class BusinessException extends RuntimeException {

    /** 错误码，默认 500 */
    private final int code;

    public BusinessException(String message) {
        this(500, message);
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}

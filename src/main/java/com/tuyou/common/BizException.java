package com.tuyou.common;

import lombok.Getter;

/**
 * 业务异常：Service 层校验不通过时抛出，由全局异常处理器统一捕获
 */
@Getter
public class BizException extends RuntimeException {

    private final int code;

    public BizException(String message) {
        super(message);
        this.code = ResultCode.SYSTEM_ERROR.getCode();
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BizException(ResultCode rc) {
        super(rc.getMessage());
        this.code = rc.getCode();
    }
}

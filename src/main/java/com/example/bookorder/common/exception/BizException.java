package com.example.bookorder.common.exception;

import java.io.Serial;

import com.example.bookorder.common.exception.enums.ErrorCodeEnum;

import lombok.Getter;

@Getter
public class BizException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 256913170408464382L;
    private final ErrorCodeEnum errorCodeEnum;

    public BizException(ErrorCodeEnum errorCodeEnum) {
        super(errorCodeEnum.getErrorDesc());
        this.errorCodeEnum = errorCodeEnum;
    }

    public BizException(ErrorCodeEnum errorCodeEnum, Throwable cause) {
        super(errorCodeEnum.getErrorDesc(), cause);
        this.errorCodeEnum = errorCodeEnum;
    }
}

package com.cgv.commondto.exception;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BusinessException extends RuntimeException {
    int code;
    HttpStatus status;
    Object data;

    public BusinessException(ErrorCode errorCode , String message , Object data) {
        super(message);
        this.code = errorCode.getCode();
        this.status = errorCode.getHttpStatus();
        this.data = data;
    }
    public BusinessException(ErrorCode errorCode , String message) {
        super(message);
        this.code = errorCode.getCode();
        this.status = errorCode.getHttpStatus();
    }
}

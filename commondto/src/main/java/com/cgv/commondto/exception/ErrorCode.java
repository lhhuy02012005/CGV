package com.cgv.commondto.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION(9999, HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_KEY(1001, HttpStatus.BAD_REQUEST),
    EXISTED(1002, HttpStatus.BAD_REQUEST),
    NOT_EXISTED(1005, HttpStatus.NOT_FOUND),
    UNAUTHENTICATED(1006,  HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1007, HttpStatus.FORBIDDEN),
    BAD_REQUEST(111, HttpStatus.BAD_REQUEST),
    INVALID_OPERATION(112,HttpStatus.BAD_REQUEST),
    DUPLICATE(113,HttpStatus.CONFLICT),
    TOO_MANY_REQUESTS(114, HttpStatus.TOO_MANY_REQUESTS),
    NOT_VERIFY(1000, HttpStatus.BAD_REQUEST),
    FORBIDDEN(1001, HttpStatus.FORBIDDEN),
    INTERNAL_ERROR(5555,HttpStatus.INTERNAL_SERVER_ERROR),
    OTP_REQUIRED(1008, HttpStatus.FORBIDDEN);

    private final int code;
    private final HttpStatus httpStatus;

    ErrorCode(int code, HttpStatus httpStatus) {
        this.code = code;
        this.httpStatus = httpStatus;
    }
}

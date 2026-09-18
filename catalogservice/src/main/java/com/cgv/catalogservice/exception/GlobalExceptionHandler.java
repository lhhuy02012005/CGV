package com.cgv.catalogservice.exception;

import com.cgv.commondto.exception.BusinessException;
import com.cgv.commondto.exception.ErrorCode;
import com.cgv.commondto.exception.ErrorResponse;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j(topic = "GLOBAL-EXCEPTION")
public class GlobalExceptionHandler {
    private static final String VALIDATION_FAILED_PREFIX = "Validation failed: ";

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex, HttpServletRequest request) {
        log.error("Business Exception occurred: {}", ex.getMessage());
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(new Date())
                .status(ex.getCode())
                .path(request.getRequestURI())
                .error(ex.getStatus().getReasonPhrase())
                .message(ex.getMessage())
                .data(ex.getData())
                .build();
        return new ResponseEntity<>(errorResponse, ex.getStatus());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex, HttpServletRequest request) {
        ErrorCode errorCode = ErrorCode.UNAUTHORIZED;
        log.warn("Access denied: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(new Date())
                .status(errorCode.getHttpStatus().value())
                .path(request.getRequestURI())
                .error(errorCode.getHttpStatus().getReasonPhrase())
                .message("Bạn không có quyền truy cập tài nguyên này!")
                .build();
        return new ResponseEntity<>(errorResponse, errorCode.getHttpStatus());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
        log.warn("Http Media Type Not Supported: {}", ex.getMessage());
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(new Date())
                .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value())
                .path(request.getRequestURI())
                .error(HttpStatus.UNSUPPORTED_MEDIA_TYPE.getReasonPhrase())
                .message("Định dạng dữ liệu không được hỗ trợ. Vui lòng gửi Header 'Content-Type: application/json'!")
                .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        ErrorCode errorCode = ErrorCode.INVALID_KEY;
        log.warn("Constraint violation exception occurred");

        List<String> errorMessages = ex.getConstraintViolations().stream()
                .map(violation -> violation.getMessage())
                .collect(Collectors.toList());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(new Date())
                .status(errorCode.getHttpStatus().value())
                .path(request.getRequestURI())
                .error(errorCode.getHttpStatus().getReasonPhrase())
                .message(VALIDATION_FAILED_PREFIX + String.join(", ", errorMessages))
                .details(errorMessages)
                .build();

        return new ResponseEntity<>(errorResponse, errorCode.getHttpStatus());
    }


    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleDataIntegrityViolation(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.error("Data integrity violation: ", ex);
        return ErrorResponse.builder()
                .timestamp(new Date())
                .status(HttpStatus.BAD_REQUEST.value())
                .path(request.getRequestURI())
                .error("Bad Request")
                .message("Dữ liệu không hợp lệ hoặc đã tồn tại trên hệ thống!")
                .build();
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFoundException(
            EntityNotFoundException ex,
            HttpServletRequest request
    ) {
        log.warn("Entity not found: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(new Date())
                .status(HttpStatus.NOT_FOUND.value())
                .path(request.getRequestURI())
                .error(HttpStatus.NOT_FOUND.getReasonPhrase())
                .message(ex.getMessage())
                .build();

        return new ResponseEntity<>(
                errorResponse,
                HttpStatus.NOT_FOUND
        );
    }

//    @ExceptionHandler(RuntimeException.class)
//    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex, HttpServletRequest request) {
//        log.error("Runtime exception occurred: {}", ex.getMessage());
//        ErrorResponse errorResponse = ErrorResponse.builder()
//                .timestamp(new Date())
//                .status(HttpStatus.BAD_REQUEST.value())
//                .path(request.getRequestURI())
//                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
//                .message(ex.getMessage())
//                .build();
//        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
//    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleGeneralException(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error occurred", ex);
        return ErrorResponse.builder()
                .timestamp(new Date())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .path(request.getRequestURI())
                .error("Internal Server Error")
                .message("Đã có lỗi hệ thống xảy ra. Vui lòng thử lại sau!")
                .build();
    }
}
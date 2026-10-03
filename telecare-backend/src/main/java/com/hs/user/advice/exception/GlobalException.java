package com.hs.user.advice.exception;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.hs.user.advice.base.AppException;
import com.hs.user.constant.base.ErrorCode;
import com.hs.user.dto.base.ApiResponse;
import com.hs.user.filter.RequestIdFilter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ControllerAdvice
public class GlobalException {

    @ExceptionHandler(value = AppException.class)
    public ResponseEntity<ApiResponse<Void>> handlingAppException(AppException exception, HttpServletRequest request) {
        ErrorCode errorCode = exception.getErrorCode();
        log.warn("AppException [code={}, message={}]: {}", errorCode.getCode(), errorCode.getMessage(), exception.getMessage());
        return ResponseEntity.status(errorCode.getStatusCode()).body(
                buildResponse(errorCode.getCode(), errorCode.getMessage(), null, request)
        );
    }

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handlingValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fe : exception.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fe.getField(), fe.getDefaultMessage());
        }

        String firstMessage = !fieldErrors.isEmpty()
                ? fieldErrors.values().iterator().next()
                : ErrorCode.INVALID_KEY.getMessage();

        log.warn("Validation error on path {}: {}", request != null ? request.getRequestURI() : "", fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                buildResponse(ErrorCode.INVALID_KEY.getCode(), firstMessage, fieldErrors, request)
        );
    }

    @ExceptionHandler(value = BindException.class)
    public ResponseEntity<ApiResponse<Void>> handlingBindException(BindException exception, HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fe : exception.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fe.getField(), fe.getDefaultMessage());
        }

        String firstMessage = !fieldErrors.isEmpty()
                ? fieldErrors.values().iterator().next()
                : ErrorCode.INVALID_KEY.getMessage();

        log.warn("BindException on path {}: {}", request != null ? request.getRequestURI() : "", fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                buildResponse(ErrorCode.INVALID_KEY.getCode(), firstMessage, fieldErrors, request)
        );
    }

    @ExceptionHandler(value = ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handlingConstraintViolation(ConstraintViolationException exception, HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getConstraintViolations().forEach(violation ->
                errors.put(violation.getPropertyPath().toString(), violation.getMessage())
        );

        log.warn("ConstraintViolationException on path {}: {}", request != null ? request.getRequestURI() : "", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                buildResponse(ErrorCode.INVALID_KEY.getCode(), "Constraint validation failed", errors, request)
        );
    }

    @ExceptionHandler(value = HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handlingNotReadable(HttpMessageNotReadableException exception, HttpServletRequest request) {
        log.warn("Malformed JSON or unreadable HTTP body: {}", exception.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                buildResponse(ErrorCode.INVALID_REQUEST_BODY.getCode(), "Malformed JSON request or invalid field format", null, request)
        );
    }

    @ExceptionHandler(value = MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handlingTypeMismatch(MethodArgumentTypeMismatchException exception, HttpServletRequest request) {
        String message = String.format("Invalid parameter '%s': '%s'", exception.getName(), exception.getValue());
        log.warn("Type mismatch on path {}: {}", request != null ? request.getRequestURI() : "", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                buildResponse(ErrorCode.INVALID_KEY.getCode(), message, null, request)
        );
    }

    @ExceptionHandler(value = MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handlingMissingParam(MissingServletRequestParameterException exception, HttpServletRequest request) {
        String message = String.format("Missing required parameter: %s", exception.getParameterName());
        log.warn("Missing parameter on path {}: {}", request != null ? request.getRequestURI() : "", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                buildResponse(ErrorCode.INVALID_KEY.getCode(), message, null, request)
        );
    }

    @ExceptionHandler(value = ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiResponse<Void>> handlingOptimisticLock(ObjectOptimisticLockingFailureException exception, HttpServletRequest request) {
        log.warn("Optimistic locking conflict on path {}: {}", request != null ? request.getRequestURI() : "", exception.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                buildResponse(ErrorCode.SUPPORT_REQUEST_CONFLICT.getCode(), ErrorCode.SUPPORT_REQUEST_CONFLICT.getMessage(), null, request)
        );
    }

    @ExceptionHandler(value = DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handlingDataIntegrity(DataIntegrityViolationException exception, HttpServletRequest request) {
        log.warn("Data integrity conflict on path {}: {}", request != null ? request.getRequestURI() : "", exception.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                buildResponse(ErrorCode.DATA_INTEGRITY_VIOLATION.getCode(), "Data conflict or unique constraint violation", null, request)
        );
    }

    @ExceptionHandler(value = MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handlingMaxUpload(MaxUploadSizeExceededException exception, HttpServletRequest request) {
        log.warn("Max upload size exceeded: {}", exception.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                buildResponse(ErrorCode.INVALID_FILE.getCode(), "Maximum upload file size exceeded", null, request)
        );
    }

    @ExceptionHandler(value = NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handlingNoResourceFoundException(NoResourceFoundException exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                buildResponse(ErrorCode.ROUTE_NOT_FOUND.getCode(), ErrorCode.ROUTE_NOT_FOUND.getMessage(), null, request)
        );
    }

    @ExceptionHandler(value = AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handlingAccessDeniedException(AccessDeniedException exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                buildResponse(ErrorCode.UNAUTHORIZED.getCode(), ErrorCode.UNAUTHORIZED.getMessage(), null, request)
        );
    }

    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<ApiResponse<Void>> handlingRuntimeException(Exception exception, HttpServletRequest request) {
        String traceId = MDC.get(RequestIdFilter.MDC_REQUEST_ID_KEY);
        log.error("Unhandled server exception [traceId={}]: ", traceId, exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                buildResponse(ErrorCode.UNCATEGORIZED_EXCEPTION.getCode(), ErrorCode.UNCATEGORIZED_EXCEPTION.getMessage(), null, request)
        );
    }

    private ApiResponse<Void> buildResponse(int code, String message, Object details, HttpServletRequest request) {
        return ApiResponse.<Void>builder()
                .code(code)
                .message(message)
                .details(details)
                .path(request != null ? request.getRequestURI() : null)
                .traceId(MDC.get(RequestIdFilter.MDC_REQUEST_ID_KEY))
                .timestamp(Instant.now())
                .build();
    }
}

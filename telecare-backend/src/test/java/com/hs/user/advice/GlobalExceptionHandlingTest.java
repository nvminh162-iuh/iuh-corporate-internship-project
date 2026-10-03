package com.hs.user.advice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.hs.user.advice.base.AppException;
import com.hs.user.advice.exception.GlobalException;
import com.hs.user.constant.base.ErrorCode;
import com.hs.user.dto.base.ApiResponse;

class GlobalExceptionHandlingTest {

    private GlobalException globalException;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        globalException = new GlobalException();
        request = new MockHttpServletRequest("GET", "/api/v1/test");
    }

    @Test
    @DisplayName("AppException maps to corresponding HTTP status and ErrorCode")
    void handleAppException_ReturnsCustomError() {
        AppException ex = new AppException(ErrorCode.SUPPORT_REQUEST_NOT_EXISTED);

        ResponseEntity<ApiResponse<Void>> response = globalException.handlingAppException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.SUPPORT_REQUEST_NOT_EXISTED.getCode());
        assertThat(response.getBody().getMessage()).isEqualTo(ErrorCode.SUPPORT_REQUEST_NOT_EXISTED.getMessage());
    }

    @Test
    @DisplayName("MethodArgumentNotValidException collects field errors into details map")
    void handleMethodArgumentNotValidException_CollectsFieldErrors() {
        Object target = new Object();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(target, "request");
        bindingResult.addError(new FieldError("request", "subject", "Tiêu đề không được để trống"));
        bindingResult.addError(new FieldError("request", "content", "Nội dung quá ngắn"));

        MethodParameter parameter = mock(MethodParameter.class);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);

        ResponseEntity<ApiResponse<Void>> response = globalException.handlingValidation(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        @SuppressWarnings("unchecked")
        Map<String, String> details = (Map<String, String>) response.getBody().getDetails();
        assertThat(details).containsEntry("subject", "Tiêu đề không được để trống");
        assertThat(details).containsEntry("content", "Nội dung quá ngắn");
    }

    @Test
    @DisplayName("HttpMessageNotReadableException returns 400 Bad Request")
    void handleHttpMessageNotReadableException_Returns400() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("JSON parse error", new MockHttpInputMessage(new byte[0]));

        ResponseEntity<ApiResponse<Void>> response = globalException.handlingNotReadable(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.INVALID_REQUEST_BODY.getCode());
    }

    @Test
    @DisplayName("MethodArgumentTypeMismatchException returns 400 Bad Request")
    void handleMethodArgumentTypeMismatchException_Returns400() {
        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
                "INVALID_STATUS",
                Enum.class,
                "status",
                mock(MethodParameter.class),
                new IllegalArgumentException("No enum constant")
        );

        ResponseEntity<ApiResponse<Void>> response = globalException.handlingTypeMismatch(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.INVALID_KEY.getCode());
    }

    @Test
    @DisplayName("ObjectOptimisticLockingFailureException returns 409 Conflict")
    void handleOptimisticLockingFailure_Returns409() {
        ObjectOptimisticLockingFailureException ex = new ObjectOptimisticLockingFailureException("SupportRequest", "sr-1");

        ResponseEntity<ApiResponse<Void>> response = globalException.handlingOptimisticLock(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.SUPPORT_REQUEST_CONFLICT.getCode());
    }

    @Test
    @DisplayName("DataIntegrityViolationException returns 409 Conflict")
    void handleDataIntegrityViolation_Returns409() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException("Unique constraint violation");

        ResponseEntity<ApiResponse<Void>> response = globalException.handlingDataIntegrity(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.DATA_INTEGRITY_VIOLATION.getCode());
    }

    @Test
    @DisplayName("AccessDeniedException returns 403 Forbidden")
    void handleAccessDeniedException_Returns403() {
        AccessDeniedException ex = new AccessDeniedException("Access is denied");

        ResponseEntity<ApiResponse<Void>> response = globalException.handlingAccessDeniedException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.UNAUTHORIZED.getCode());
    }

    @Test
    @DisplayName("Unhandled Exception returns 500 without leaking stack trace")
    void handleGenericException_Returns500WithoutStackTrace() {
        NullPointerException ex = new NullPointerException("Unexpected null pointer in service");

        ResponseEntity<ApiResponse<Void>> response = globalException.handlingRuntimeException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.UNCATEGORIZED_EXCEPTION.getCode());
        assertThat(response.getBody().getMessage()).doesNotContain("NullPointerException");
    }
}

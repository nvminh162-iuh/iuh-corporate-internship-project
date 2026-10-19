package com.hs.user.advice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
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
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

class GlobalExceptionHandlingTest {

    private GlobalException globalException;
    private MockHttpServletRequest request;
    private final Logger logger = (Logger) LoggerFactory.getLogger(GlobalException.class);
    private ListAppender<ILoggingEvent> events;

    @BeforeEach
    void setUp() {
        globalException = new GlobalException();
        request = new MockHttpServletRequest("GET", "/api/v1/test");
        events = new ListAppender<>();
        events.start();
        logger.addAppender(events);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(events);
        events.stop();
        MDC.clear();
    }

    @Test
    void errorLogsExcludeSensitiveMessagesValuesAndSqlButKeepDiagnostics() {
        String secret = "synthetic-sensitive-password";
        request.setRequestURI("/api/v1/" + secret);
        MDC.put("requestId", "error-correlation-id");
        globalException.handlingNotReadable(new HttpMessageNotReadableException(secret,
                new MockHttpInputMessage(new byte[0])), request);
        globalException.handlingTypeMismatch(new MethodArgumentTypeMismatchException(secret, Enum.class,
                "status", mock(MethodParameter.class), new IllegalArgumentException(secret)), request);
        globalException.handlingDataIntegrity(new DataIntegrityViolationException("SQL values=" + secret), request);
        globalException.handlingRuntimeException(new IllegalStateException(secret, new RuntimeException(secret)), request);

        assertThat(events.list).hasSize(4);
        assertThat(events.list).allSatisfy(event -> {
            assertThat(event.getFormattedMessage()).doesNotContain(secret);
            assertThat(event.getThrowableProxy()).isNull();
        });
        assertThat(events.list.getLast().getFormattedMessage())
                .contains("traceId=error-correlation-id", "errorType=IllegalStateException", "GlobalExceptionHandlingTest.java");
    }

    @Test
    void validationLogsExcludeInterpolatedMessagesAndRejectedValues() {
        String secret = "synthetic-validation-secret";
        BeanPropertyBindingResult result = new BeanPropertyBindingResult(new Object(), "request");
        result.addError(new FieldError("request", "password", secret, false, null, null, "Rejected " + secret));
        globalException.handlingValidation(new MethodArgumentNotValidException(mock(MethodParameter.class), result), request);
        assertThat(events.list.getFirst().getFormattedMessage()).contains("invalidFieldCount=1").doesNotContain(secret);
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

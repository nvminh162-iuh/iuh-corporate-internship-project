package com.hs.user.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockAsyncContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.servlet.HandlerMapping;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.ServletException;

class RequestIdFilterTest {
    private final RequestIdFilter filter = new RequestIdFilter();
    private final Logger logger = (Logger) LoggerFactory.getLogger(RequestIdFilter.class);
    private final ListAppender<ILoggingEvent> events = new ListAppender<>() {
        @Override
        protected void append(ILoggingEvent event) {
            event.prepareForDeferredProcessing();
            super.append(event);
        }
    };
    private Level originalLevel;

    @BeforeEach
    void setUp() {
        originalLevel = logger.getLevel();
        logger.setLevel(Level.INFO);
        events.start();
        logger.addAppender(events);
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(events);
        logger.setLevel(originalLevel);
        events.stop();
        MDC.clear();
        SecurityContextHolder.clearContext();
        UserContextHolder.clear();
    }

    @Test
    void correlatesResponseAndLogAndRestoresMdc() throws Exception {
        MockHttpServletRequest request = request();
        request.addHeader("X-Request-Id", " demo-request-10 ");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MDC.put("requestId", "parent-context");
        MDC.put("other", "keep-me");

        filter.doFilter(request, response, (req, res) -> {
            assertThat(MDC.get("requestId")).isEqualTo("demo-request-10");
            assertThat(req.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE)).isEqualTo("demo-request-10");
        });

        assertThat(response.getHeader("X-Request-Id")).isEqualTo("demo-request-10");
        assertThat(events.list).hasSize(1);
        assertThat(events.list.getFirst().getFormattedMessage())
                .contains("requestId=demo-request-10", "method=GET", "path=/api/v1/plans", "status=200", "durationMs=", "actorId=anonymous");
        assertThat(events.list.getFirst().getMDCPropertyMap()).containsEntry("requestId", "demo-request-10");
        assertThat(MDC.get("requestId")).isEqualTo("parent-context");
        assertThat(MDC.get("other")).isEqualTo("keep-me");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "bad\r\nforged", "space inside", "01234567890123456789012345678901234567890123456789012345678901234"})
    void replacesInvalidIncomingIds(String supplied) throws Exception {
        MockHttpServletRequest request = request();
        request.addHeader("X-Request-Id", supplied);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, (req, res) -> {});
        assertThat(response.getHeader("X-Request-Id")).matches("[a-f0-9-]{36}").isNotEqualTo(supplied);
        assertThat(MDC.get("requestId")).isNull();
    }

    @Test
    void createsDifferentIdsForIndependentRequests() throws Exception {
        MockHttpServletResponse first = new MockHttpServletResponse();
        MockHttpServletResponse second = new MockHttpServletResponse();
        filter.doFilter(request(), first, (req, res) -> {});
        filter.doFilter(request(), second, (req, res) -> {});
        assertThat(first.getHeader("X-Request-Id")).isNotEqualTo(second.getHeader("X-Request-Id"));
        assertThat(events.list).hasSize(2);
        assertThat(MDC.get("requestId")).isNull();
    }

    @ParameterizedTest
    @CsvSource({"200, INFO", "204, INFO", "302, INFO", "400, WARN", "401, WARN", "403, WARN", "404, WARN", "405, WARN", "500, ERROR", "503, ERROR"})
    void choosesSeverityFromFinalStatus(int status, String level) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request(), response, (req, res) -> response.setStatus(status));
        assertThat(events.list).hasSize(1);
        assertThat(events.list.getFirst().getLevel()).isEqualTo(Level.valueOf(level));
        assertThat(events.list.getFirst().getFormattedMessage()).contains("status=" + status);
    }

    @Test
    void logsEscapingFailureAs500AndPropagatesWithoutLeakingMessage() {
        ServletException failure = new ServletException("password=synthetic-secret");
        MockHttpServletResponse response = new MockHttpServletResponse();
        assertThatThrownBy(() -> filter.doFilter(request(), response, (req, res) -> { throw failure; }))
                .isSameAs(failure);
        assertThat(response.getStatus()).isEqualTo(200); // Container, not logging, owns the error response.
        assertThat(events.list.getFirst().getLevel()).isEqualTo(Level.ERROR);
        assertThat(events.list.getFirst().getFormattedMessage()).contains("status=500").doesNotContain("synthetic-secret");
        assertThat(MDC.get("requestId")).isNull();
    }

    @Test
    void neverReadsOrLogsBodiesHeadersQueryValuesOrDynamicPathValues() throws Exception {
        MockHttpServletRequest request = request();
        request.setRequestURI("/api/v1/users/sensitive-id");
        request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/users/{id}");
        request.setQueryString("access_token=query-secret&email=private@example.test");
        request.addHeader("Authorization", "Bearer token-secret");
        request.addHeader("Cookie", "session=cookie-secret");
        request.addHeader("X-User-Id", "forged-actor");
        request.setContent("{\"password\":\"body-secret\"}".getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> response.getWriter().write("response-secret"));

        assertThat(events.list.getFirst().getFormattedMessage())
                .contains("path=/api/v1/users/{id}", "actorId=anonymous")
                .doesNotContain("sensitive-id", "query-secret", "private@example.test", "token-secret", "cookie-secret", "body-secret", "response-secret", "forged-actor");
        assertThat(response.getContentAsString()).isEqualTo("response-secret");
        assertThat(request.getInputStream().readAllBytes()).isEqualTo(request.getContentAsByteArray());
    }

    @Test
    void unknownRoutesDoNotLeakRawPathsOrControlCharacters() throws Exception {
        MockHttpServletRequest request = request();
        request.removeAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        request.setRequestURI("/token-secret\r\nforged");
        request.setMethod("GET\r\nforged");
        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> {});
        assertThat(events.list.getFirst().getFormattedMessage())
                .contains("path=[unmapped]", "method=OTHER").doesNotContain("token-secret", "forged", "\r", "\n");
    }

    @Test
    void capturesVerifiedSubjectBeforeUserContextIsCleared() throws Exception {
        Jwt jwt = Jwt.withTokenValue("not-logged").header("alg", "none")
                .subject("user-123").claim("email", "private@example.test").build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
        MockHttpServletRequest request = request();
        request.addHeader("X-User-Id", "forged-actor");
        filter.doFilter(request, new MockHttpServletResponse(), (req, res) ->
                new UserContextFilter().doFilter(req, res, (innerReq, innerRes) ->
                        assertThat(UserContextHolder.get().getUserId()).isEqualTo("user-123")));
        assertThat(UserContextHolder.get()).isNull();
        assertThat(events.list.getFirst().getFormattedMessage()).contains("actorId=user-123")
                .doesNotContain("forged-actor", "private@example.test", "not-logged");
    }

    @Test
    void sanitizesUnexpectedSubjectInsteadOfAllowingLogInjection() throws Exception {
        MockHttpServletRequest request = request();
        filter.doFilter(request, new MockHttpServletResponse(), (req, res) ->
                RequestIdFilter.recordAuthenticatedActor(request, "bad\r\nsubject"));
        assertThat(events.list.getFirst().getFormattedMessage()).contains("actorId=unavailable").doesNotContain("bad", "\r", "\n");
    }

    @Test
    void asyncRequestLogsOnlyAtCompletionWithFinalStatusAndNoMdcLeak() throws Exception {
        MockHttpServletRequest request = request();
        request.setAsyncSupported(true);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, (req, res) -> req.startAsync(req, res));
        assertThat(events.list).isEmpty();
        assertThat(MDC.get("requestId")).isNull();
        response.setStatus(503);
        AsyncContext context = request.getAsyncContext();
        context.complete();
        assertThat(events.list).hasSize(1);
        assertThat(events.list.getFirst().getLevel()).isEqualTo(Level.ERROR);
        assertThat(events.list.getFirst().getFormattedMessage()).contains("status=503");
        assertThat(MDC.get("requestId")).isNull();
    }

    @Test
    void asyncFailureWithoutAnAssignedStatusIsNotMisreportedAsSuccess() throws Exception {
        MockHttpServletRequest request = request();
        request.setAsyncSupported(true);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, (req, res) -> req.startAsync(req, res));
        MockAsyncContext context = (MockAsyncContext) request.getAsyncContext();
        context.getListeners().getFirst().onError(new AsyncEvent(context, request, response,
                new IllegalStateException("synthetic-async-secret")));
        context.complete();
        context.complete();
        assertThat(events.list).hasSize(1);
        assertThat(events.list.getFirst().getFormattedMessage()).contains("status=500").doesNotContain("synthetic-async-secret");
        assertThat(MDC.get("requestId")).isNull();
    }

    @Test
    void completionOnAnotherThreadRetainsCorrelation() throws Exception {
        MockHttpServletRequest request = request();
        request.setAsyncSupported(true);
        request.addHeader("X-Request-Id", "async-correlation");
        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> req.startAsync(req, res));
        Thread worker = new Thread(() -> {
            MDC.put("requestId", "worker-context");
            request.getAsyncContext().complete();
            if (!"worker-context".equals(MDC.get("requestId"))) {
                throw new AssertionError("Async listener did not restore MDC");
            }
            MDC.clear();
        });
        java.util.concurrent.atomic.AtomicReference<Throwable> failure = new java.util.concurrent.atomic.AtomicReference<>();
        worker.setUncaughtExceptionHandler((thread, exception) -> failure.set(exception));
        worker.start();
        worker.join(5000);
        assertThat(worker.isAlive()).isFalse();
        assertThat(failure.get()).isNull();
        assertThat(events.list).hasSize(1);
        assertThat(events.list.getFirst().getMDCPropertyMap()).containsEntry("requestId", "async-correlation");
        assertThat(MDC.get("requestId")).isNull();
    }

    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/plans");
        request.setContextPath("/api/v1");
        request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/plans");
        return request;
    }
}

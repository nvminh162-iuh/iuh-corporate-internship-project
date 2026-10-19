package com.hs.user.filter;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String MDC_REQUEST_ID_KEY = "requestId";
    public static final String REQUEST_ID_ATTRIBUTE = RequestIdFilter.class.getName() + ".requestId";
    private static final String LOG_STATE_ATTRIBUTE = RequestIdFilter.class.getName() + ".logState";
    private static final String ACTOR_ATTRIBUTE = RequestIdFilter.class.getName() + ".actorId";
    private static final Pattern VALID_REQUEST_ID = Pattern.compile("^[a-zA-Z0-9_-]{1,64}$");
    private static final Pattern VALID_ACTOR_ID = Pattern.compile("^[a-zA-Z0-9_.:@-]{1,128}$");

    /** Capture only a verified JWT subject, not the legacy X-User-* headers. */
    public static void recordAuthenticatedActor(HttpServletRequest request, String subject) {
        request.setAttribute(ACTOR_ATTRIBUTE,
                subject != null && VALID_ACTOR_ID.matcher(subject).matches() ? subject : "unavailable");
    }

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        RequestLog state = (RequestLog) request.getAttribute(LOG_STATE_ATTRIBUTE);
        if (state == null) {
            String suppliedId = request.getHeader(REQUEST_ID_HEADER);
            String requestId = suppliedId != null && VALID_REQUEST_ID.matcher(suppliedId.trim()).matches()
                    ? suppliedId.trim() : UUID.randomUUID().toString();
            state = new RequestLog(requestId, request, response);
            request.setAttribute(LOG_STATE_ATTRIBUTE, state);
            request.setAttribute(REQUEST_ID_ATTRIBUTE, requestId);
        }

        String previousRequestId = MDC.get(MDC_REQUEST_ID_KEY);
        MDC.put(MDC_REQUEST_ID_KEY, state.requestId);
        response.setHeader(REQUEST_ID_HEADER, state.requestId);
        try {
            filterChain.doFilter(request, response);
        } catch (ServletException | IOException | RuntimeException | Error exception) {
            state.failed = true;
            throw exception;
        } finally {
            try {
                if (request.isAsyncStarted()) {
                    // Do not log a premature 200 when asynchronous processing has only begun.
                    try {
                        request.getAsyncContext().addListener(state);
                    } catch (IllegalStateException alreadyCompleted) {
                        state.complete();
                    }
                } else {
                    state.complete();
                }
            } finally {
                restoreRequestId(previousRequestId);
            }
        }
    }

    private static void restoreRequestId(String previous) {
        if (previous == null) {
            MDC.remove(MDC_REQUEST_ID_KEY);
        } else {
            MDC.put(MDC_REQUEST_ID_KEY, previous);
        }
    }

    private static final class RequestLog implements AsyncListener {
        private final String requestId;
        private final HttpServletRequest request;
        private final HttpServletResponse response;
        private final long startNanos = System.nanoTime();
        private final AtomicBoolean logged = new AtomicBoolean();
        private volatile boolean failed;

        private RequestLog(String requestId, HttpServletRequest request, HttpServletResponse response) {
            this.requestId = requestId;
            this.request = request;
            this.response = response;
        }

        private void complete() {
            if (!logged.compareAndSet(false, true)) {
                return;
            }
            String previousRequestId = MDC.get(MDC_REQUEST_ID_KEY);
            MDC.put(MDC_REQUEST_ID_KEY, requestId);
            try {
                int status = failed && response.getStatus() < 400 ? 500 : response.getStatus();
                long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
                Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
                // Templates retain endpoint visibility without logging IDs, query values or tokens.
                String path = pattern != null ? request.getContextPath() + pattern : "[unmapped]";
                Object actor = request.getAttribute(ACTOR_ATTRIBUTE);
                String method = request.getMethod();
                String message = "HTTP requestId={} method={} path={} status={} durationMs={} actorId={}";
                Object[] arguments = {requestId, method != null && method.matches("[A-Z]{1,20}") ? method : "OTHER",
                        path, status, durationMs, actor != null ? actor : "anonymous"};
                if (status >= 500) {
                    log.error(message, arguments);
                } else if (status >= 400) {
                    log.warn(message, arguments);
                } else {
                    log.info(message, arguments);
                }
            } finally {
                restoreRequestId(previousRequestId);
            }
        }

        @Override
        public void onComplete(AsyncEvent event) {
            complete();
        }

        @Override
        public void onTimeout(AsyncEvent event) {
            // Wait for completion: the application's timeout handler determines the status.
        }

        @Override
        public void onError(AsyncEvent event) {
            failed = true;
        }

        @Override
        public void onStartAsync(AsyncEvent event) {
            event.getAsyncContext().addListener(this);
        }
    }
}

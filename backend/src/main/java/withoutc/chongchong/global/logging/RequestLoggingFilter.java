package withoutc.chongchong.global.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.slf4j.spi.LoggingEventBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final String UNMATCHED_ROUTE = "UNMATCHED_ROUTE";

    @Value("${spring.application.name:unknown}")
    private String service;

    @Value("${app.environment:local}")
    private String environment;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestId = resolveRequestId(request);

        request.setAttribute(RequestLoggingContext.REQUEST_ID_ATTRIBUTE, requestId);
        request.setAttribute(RequestLoggingContext.START_TIME_ATTRIBUTE, System.nanoTime());

        response.setHeader(RequestLoggingContext.REQUEST_ID_HEADER, requestId);
        MDC.put(RequestLoggingContext.MDC_REQUEST_ID_KEY, requestId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            try {
                logRequest(request, response);
            } finally {
                MDC.remove(RequestLoggingContext.MDC_REQUEST_ID_KEY);
            }
        }
    }

    private String resolveRequestId(HttpServletRequest request) {
        String requestId = request.getHeader(RequestLoggingContext.REQUEST_ID_HEADER);

        if (requestId != null && requestId.matches("[A-Za-z0-9._:-]{1,64}")) {
            return requestId;
        }
        return UUID.randomUUID().toString();
    }

    private void logRequest(HttpServletRequest request, HttpServletResponse response) {
        long startTime = (long) request.getAttribute(RequestLoggingContext.START_TIME_ATTRIBUTE);
        long durationMs = Duration.ofNanos(System.nanoTime() - startTime).toMillis();

        int status = response.getStatus();

        String route = resolveRoute(request);

        String errorCode = (String) request.getAttribute(RequestLoggingContext.ERROR_CODE_ATTRIBUTE);

        LoggingEventBuilder eventBuilder = getEventBuilder(status, errorCode);

        eventBuilder.addKeyValue("service", service)
                .addKeyValue("environment", environment)
                .addKeyValue("method", request.getMethod())
                .addKeyValue("route", route)
                .addKeyValue("status", status)
                .addKeyValue("duration_ms", durationMs)
                .addKeyValue("error_code", errorCode);

        if (status >= 500) {
            eventBuilder.log("request failed");
            return;
        }
        eventBuilder.log("request completed");
    }

    String resolveRoute(HttpServletRequest request) {
        String matchedRoute = (String) request.getAttribute(
                HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE
        );
        if (matchedRoute != null) {
            return matchedRoute;
        }
        return UNMATCHED_ROUTE;
    }

    private LoggingEventBuilder getEventBuilder(int status, String errorCode) {
        if (status >= 500) {
            return log.atError();
        }
        if (status == 429 || "INVALID_CSRF_TOKEN".equals(errorCode)) {
            return log.atWarn();
        }
        return log.atInfo();
    }
}

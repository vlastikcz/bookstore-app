package com.example.bookstore.catalog.config;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.regex.Pattern;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RequestIdFilter extends OncePerRequestFilter {

    private static final String TRACEPARENT = "traceparent";
    private static final String REQUEST_ID = "Request-Id";
    private static final Pattern TRACEPARENT_PATTERN = Pattern.compile("^[\\da-f]{2}-[\\da-f]{32}-[\\da-f]{16}-[\\da-f]{2}$");
    private static final Pattern REQUEST_ID_PATTERN = Pattern.compile("^[\\da-f]{32}$");
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String traceparent = normalizeTraceparent(request.getHeader(TRACEPARENT));
        if (!StringUtils.hasText(traceparent)) {
            traceparent = generateTraceparent();
        }

        request.setAttribute(TRACEPARENT, traceparent);
        response.setHeader(TRACEPARENT, traceparent);

        String requestId = normalizeRequestId(request.getHeader(REQUEST_ID));
        if (!StringUtils.hasText(requestId)) {
            requestId = extractTraceId(traceparent);
        }

        if (StringUtils.hasText(requestId)) {
            request.setAttribute(REQUEST_ID, requestId);
            response.setHeader(REQUEST_ID, requestId);
        }

        filterChain.doFilter(request, response);
    }

    private String normalizeTraceparent(String headerValue) {
        if (!StringUtils.hasText(headerValue)) {
            return null;
        }

        String trimmed = headerValue.trim();
        return TRACEPARENT_PATTERN.matcher(trimmed).matches() ? trimmed : null;
    }

    private String generateTraceparent() {
        byte[] traceId = new byte[16];
        byte[] parentId = new byte[8];
        RANDOM.nextBytes(traceId);
        RANDOM.nextBytes(parentId);

        String traceIdHex = HexFormat.of().formatHex(traceId);
        String parentIdHex = HexFormat.of().formatHex(parentId);
        return "00-" + traceIdHex + "-" + parentIdHex + "-01";
    }

    private String normalizeRequestId(String headerValue) {
        if (!StringUtils.hasText(headerValue)) {
            return null;
        }

        String trimmed = headerValue.trim();
        return REQUEST_ID_PATTERN.matcher(trimmed).matches() ? trimmed : null;
    }

    private String extractTraceId(String traceparent) {
        if (!StringUtils.hasText(traceparent)) {
            return null;
        }

        String[] parts = traceparent.split("-");
        if (parts.length != 4) {
            return null;
        }
        return parts[1];
    }
}

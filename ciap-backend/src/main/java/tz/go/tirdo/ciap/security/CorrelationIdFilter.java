package tz.go.tirdo.ciap.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Carries one request id from Nginx (or generates one) into the response and
 * the log context. The id is a short token, never a credential.
 */
@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Request-Id";
    private static final Pattern SAFE = Pattern.compile("^[A-Za-z0-9._-]{8,80}$");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String incoming = request.getHeader(HEADER);
        String requestId = incoming != null && SAFE.matcher(incoming).matches()
                ? incoming
                : UUID.randomUUID().toString();
        MDC.put("requestId", requestId);
        response.setHeader(HEADER, requestId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("requestId");
        }
    }
}

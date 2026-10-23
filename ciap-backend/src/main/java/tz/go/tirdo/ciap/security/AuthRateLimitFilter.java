package tz.go.tirdo.ciap.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Limits login, token refresh, and public registration per client address.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private final AppSecurityProperties properties;
    private final AttemptLimiter limiter;

    public AuthRateLimitFilter(AppSecurityProperties properties) {
        this.properties = properties;
        this.limiter = new AttemptLimiter(
                Math.max(1, properties.getAuthRateLimit()),
                Math.max(1, properties.getAuthRateWindowSeconds()) * 1000L);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!HttpMethod.POST.matches(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        return !(path.endsWith("/auth/login")
                || path.endsWith("/auth/refresh")
                || path.endsWith("/auth/change-password")
                || path.endsWith("/clients/register"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!limiter.allow(clientKey(request))) {
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(
                    "{\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Too many attempts. Try again later.\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private String clientKey(HttpServletRequest request) {
        if (properties.isTrustProxy()) {
            String realIp = request.getHeader("X-Real-IP");
            if (realIp != null) {
                String candidate = realIp.trim();
                if (candidate.length() <= 45 && candidate.matches("[0-9a-fA-F:.]+")) {
                    return candidate;
                }
            }
        }
        return request.getRemoteAddr();
    }
}

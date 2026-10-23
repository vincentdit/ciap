package tz.go.tirdo.ciap.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.List;

@ConfigurationProperties(prefix = "ciap.security")
public class AppSecurityProperties {

    /** Comma-separated browser origins allowed to call the API with credentials. */
    private String corsAllowedOrigins = "http://localhost:4200,http://localhost:8088";

    /** Swagger and OpenAPI. Keep false on any shared or deployed profile. */
    private boolean exposeApiDocs = false;

    /**
     * When true, rate limits use X-Real-IP set by the trusted reverse proxy.
     * Enable only when the backend is not reachable except through that proxy.
     */
    private boolean trustProxy = false;

    private int authRateLimit = 20;
    private int authRateWindowSeconds = 60;

    public List<String> corsOrigins() {
        return Arrays.stream(corsAllowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();
    }

    public String getCorsAllowedOrigins() {
        return corsAllowedOrigins;
    }

    public void setCorsAllowedOrigins(String corsAllowedOrigins) {
        this.corsAllowedOrigins = corsAllowedOrigins;
    }

    public boolean isExposeApiDocs() {
        return exposeApiDocs;
    }

    public void setExposeApiDocs(boolean exposeApiDocs) {
        this.exposeApiDocs = exposeApiDocs;
    }

    public boolean isTrustProxy() {
        return trustProxy;
    }

    public void setTrustProxy(boolean trustProxy) {
        this.trustProxy = trustProxy;
    }

    public int getAuthRateLimit() {
        return authRateLimit;
    }

    public void setAuthRateLimit(int authRateLimit) {
        this.authRateLimit = authRateLimit;
    }

    public int getAuthRateWindowSeconds() {
        return authRateWindowSeconds;
    }

    public void setAuthRateWindowSeconds(int authRateWindowSeconds) {
        this.authRateWindowSeconds = authRateWindowSeconds;
    }
}

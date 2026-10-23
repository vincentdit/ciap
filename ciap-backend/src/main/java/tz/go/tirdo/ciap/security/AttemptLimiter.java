package tz.go.tirdo.ciap.security;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Fixed-window counter used to slow credential stuffing against public auth endpoints.
 * Not a distributed limiter: each application instance keeps its own windows.
 */
public final class AttemptLimiter {

    private final int maxAttempts;
    private final long windowMillis;
    private final ConcurrentHashMap<String, long[]> windows = new ConcurrentHashMap<>();

    public AttemptLimiter(int maxAttempts, long windowMillis) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be positive");
        }
        if (windowMillis < 1) {
            throw new IllegalArgumentException("windowMillis must be positive");
        }
        this.maxAttempts = maxAttempts;
        this.windowMillis = windowMillis;
    }

    public boolean allow(String key) {
        return allow(key, System.currentTimeMillis());
    }

    public boolean allow(String key, long nowMillis) {
        long[] state = windows.compute(key, (ignored, existing) -> {
            if (existing == null || nowMillis - existing[0] >= windowMillis) {
                return new long[] { nowMillis, 1 };
            }
            existing[1]++;
            return existing;
        });
        if (windows.size() > 20_000) {
            windows.clear();
        }
        return state[1] <= maxAttempts;
    }
}

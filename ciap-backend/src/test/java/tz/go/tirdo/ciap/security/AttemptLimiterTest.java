package tz.go.tirdo.ciap.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AttemptLimiterTest {

    @Test
    void blocksAfterTheWindowIsExhaustedAndResetsWhenItElapses() {
        AttemptLimiter limiter = new AttemptLimiter(2, 1_000);

        assertThat(limiter.allow("client", 0)).isTrue();
        assertThat(limiter.allow("client", 100)).isTrue();
        assertThat(limiter.allow("client", 200)).isFalse();
        assertThat(limiter.allow("client", 1_000)).isTrue();
        assertThat(limiter.allow("other", 200)).isTrue();
    }
}

package com.flowset.observability;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RequestIdFilterTest {

    @Test
    void acceptsSafeIdsAndRejectsUnsafeValues() {
        assertThat(RequestIdFilter.isSafeRequestId("abc-123_def.9")).isTrue();
        assertThat(RequestIdFilter.isSafeRequestId("short")).isFalse();
        assertThat(RequestIdFilter.isSafeRequestId("has space")).isFalse();
        assertThat(RequestIdFilter.isSafeRequestId("<script>")).isFalse();
        assertThat(RequestIdFilter.isSafeRequestId(null)).isFalse();
    }
}

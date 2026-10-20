package com.gohar.platform.seckit.unit.infrastructure;

import com.gohar.platform.seckit.infrastructure.ratelimit.RegistrationRateLimitFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("RegistrationRateLimitFilter Unit Tests")
class RegistrationRateLimitFilterTest {

    private RegistrationRateLimitFilter filter;
    private FilterChain filterChain;
    private Instant fixedInstant;

    @BeforeEach
    void setUp() {
        fixedInstant = Instant.parse("2026-10-19T12:00:00Z");
        Clock clock = Clock.fixed(fixedInstant, ZoneId.of("UTC"));
        filter = new RegistrationRateLimitFilter(5, 60, clock);
        filterChain = mock(FilterChain.class);
    }

    @Test
    @DisplayName("Allows up to 5 registration attempts per client IP within 1 minute")
    void allowsUpToFiveRegistrationAttempts() throws ServletException, IOException {
        for (int i = 1; i <= 5; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/register");
            request.setRemoteAddr("192.168.1.100");
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, filterChain);

            assertThat(response.getStatus()).isEqualTo(200);
        }
        verify(filterChain, times(5)).doFilter(any(), any());
    }

    @Test
    @DisplayName("Rejects the 6th registration attempt from the same client IP with HTTP 429 Too Many Requests")
    void rejectsSixthRegistrationAttemptWith429() throws ServletException, IOException {
        // Send 5 permitted attempts
        for (int i = 0; i < 5; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/register");
            req.setRemoteAddr("10.0.0.1");
            MockHttpServletResponse res = new MockHttpServletResponse();
            filter.doFilter(req, res, filterChain);
        }

        // 6th attempt
        MockHttpServletRequest request6 = new MockHttpServletRequest("POST", "/register");
        request6.setRemoteAddr("10.0.0.1");
        MockHttpServletResponse response6 = new MockHttpServletResponse();

        filter.doFilter(request6, response6, filterChain);

        assertThat(response6.getStatus()).isEqualTo(429);
        assertThat(response6.getHeader("Retry-After")).isEqualTo("60");
        assertThat(response6.getContentAsString()).contains("Maximum registration attempts exceeded");
        // Verify filterChain was only called 5 times, not for the 6th
        verify(filterChain, times(5)).doFilter(any(), any());
    }

    @Test
    @DisplayName("Returns RFC 9457 Problem Details when Accept header specifies application/problem+json")
    void returnsProblemDetailsWhenRequested() throws ServletException, IOException {
        for (int i = 0; i < 5; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/register");
            req.setRemoteAddr("10.0.0.2");
            filter.doFilter(req, new MockHttpServletResponse(), filterChain);
        }

        MockHttpServletRequest request6 = new MockHttpServletRequest("POST", "/register");
        request6.setRemoteAddr("10.0.0.2");
        request6.addHeader("Accept", "application/problem+json");
        MockHttpServletResponse response6 = new MockHttpServletResponse();

        filter.doFilter(request6, response6, filterChain);

        assertThat(response6.getStatus()).isEqualTo(429);
        assertThat(response6.getContentType()).isEqualTo("application/problem+json;charset=UTF-8");
        assertThat(response6.getContentAsString()).contains("\"status\":429");
    }

    @Test
    @DisplayName("Tracks client IP rate limits independently across different IP addresses")
    void tracksDifferentIpsIndependently() throws ServletException, IOException {
        // 5 requests from IP A
        for (int i = 0; i < 5; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/register");
            req.setRemoteAddr("1.1.1.1");
            filter.doFilter(req, new MockHttpServletResponse(), filterChain);
        }

        // Request from IP B should still be allowed
        MockHttpServletRequest reqB = new MockHttpServletRequest("POST", "/register");
        reqB.setRemoteAddr("2.2.2.2");
        MockHttpServletResponse resB = new MockHttpServletResponse();

        filter.doFilter(reqB, resB, filterChain);

        assertThat(resB.getStatus()).isEqualTo(200);
        verify(filterChain, times(6)).doFilter(any(), any());
    }

    @Test
    @DisplayName("Does not throttle GET requests to /register")
    void doesNotThrottleGetRequests() throws ServletException, IOException {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/register");
        req.setRemoteAddr("10.0.0.3");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, filterChain);

        assertThat(res.getStatus()).isEqualTo(200);
        verify(filterChain).doFilter(req, res);
    }
}

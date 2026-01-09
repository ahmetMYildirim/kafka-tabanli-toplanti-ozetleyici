package security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitFilter Unit Tests")
class RateLimitFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private RateLimitFilter rateLimitFilter;

    @BeforeEach
    void setUp() {
        rateLimitFilter = new RateLimitFilter();
        ReflectionTestUtils.setField(rateLimitFilter, "requestsPerMinute", 60);
        ReflectionTestUtils.setField(rateLimitFilter, "requestsPerSecond", 10);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("Rate Limiting Tests")
    class RateLimitingTests {

        @Test
        @DisplayName("Should allow request within limit")
        void doFilterInternal_WithinLimit_ShouldAllow() throws Exception {
            
            when(request.getRemoteAddr()).thenReturn("192.168.1.1");

            
            rateLimitFilter.doFilterInternal(request, response, filterChain);

            
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Should block request when limit exceeded")
        void doFilterInternal_WhenLimitExceeded_ShouldBlock() throws Exception {
            
            when(request.getRemoteAddr()).thenReturn("192.168.1.100");

            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            when(response.getWriter()).thenReturn(printWriter);

            
            for (int i = 0; i < 60; i++) {
                rateLimitFilter.doFilterInternal(request, response, filterChain);
            }

            
            rateLimitFilter.doFilterInternal(request, response, filterChain);

            
            verify(response, atLeastOnce()).setStatus(429);
        }

        @Test
        @DisplayName("Should use authenticated user as key")
        void doFilterInternal_WithAuthenticatedUser_ShouldUseUserKey() throws Exception {
            
            Authentication auth = mock(Authentication.class);
            when(auth.isAuthenticated()).thenReturn(true);
            when(auth.getPrincipal()).thenReturn("testuser");

            SecurityContext securityContext = mock(SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(auth);
            SecurityContextHolder.setContext(securityContext);

            
            rateLimitFilter.doFilterInternal(request, response, filterChain);

            
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Should use X-Forwarded-For header for IP")
        void doFilterInternal_WithXForwardedFor_ShouldUseProxyIP() throws Exception {
            
            when(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.1, 192.168.1.1");

            
            rateLimitFilter.doFilterInternal(request, response, filterChain);

            
            verify(filterChain).doFilter(request, response);
        }
    }

    @Nested
    @DisplayName("Filter Exclusion Tests")
    class FilterExclusionTests {

        @Test
        @DisplayName("Should not filter WebSocket endpoints")
        void shouldNotFilter_WebSocketPath_ShouldReturnTrue() throws Exception {
            
            when(request.getServletPath()).thenReturn("/ws/meetings");

            
            boolean shouldNotFilter = rateLimitFilter.shouldNotFilter(request);

            
            assertThat(shouldNotFilter).isTrue();
        }

        @Test
        @DisplayName("Should not filter health check endpoint")
        void shouldNotFilter_HealthCheck_ShouldReturnTrue() throws Exception {
            
            when(request.getServletPath()).thenReturn("/actuator/health");

            
            boolean shouldNotFilter = rateLimitFilter.shouldNotFilter(request);

            
            assertThat(shouldNotFilter).isTrue();
        }

        @Test
        @DisplayName("Should filter regular API endpoints")
        void shouldNotFilter_RegularApi_ShouldReturnFalse() throws Exception {
            
            when(request.getServletPath()).thenReturn("/api/v1/meetings");

            
            boolean shouldNotFilter = rateLimitFilter.shouldNotFilter(request);

            
            assertThat(shouldNotFilter).isFalse();
        }
    }
}


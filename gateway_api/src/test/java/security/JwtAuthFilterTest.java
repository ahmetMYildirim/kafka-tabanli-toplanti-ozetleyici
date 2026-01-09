package security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthFilter Unit Tests")
class JwtAuthFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthFilter jwtAuthFilter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("Filter Authentication Tests")
    class FilterAuthenticationTests {

        @Test
        @DisplayName("Should authenticate with valid token")
        void doFilterInternal_WithValidToken_ShouldAuthenticate() throws Exception {
            
            String token = "valid-jwt-token";
            when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
            when(jwtTokenProvider.validateToken(token)).thenReturn(true);
            when(jwtTokenProvider.getUsernameToken(token)).thenReturn("testuser");
            when(request.getRequestURI()).thenReturn("/api/v1/meetings");

            
            jwtAuthFilter.doFilterInternal(request, response, filterChain);

            
            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
            assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                    .isEqualTo("testuser");
        }

        @Test
        @DisplayName("Should not authenticate with invalid token")
        void doFilterInternal_WithInvalidToken_ShouldNotAuthenticate() throws Exception {
            
            String token = "invalid-token";
            when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
            when(jwtTokenProvider.validateToken(token)).thenReturn(false);

            
            jwtAuthFilter.doFilterInternal(request, response, filterChain);

            
            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("Should continue filter chain without Authorization header")
        void doFilterInternal_WithoutAuthHeader_ShouldContinue() throws Exception {
            
            when(request.getHeader("Authorization")).thenReturn(null);

            
            jwtAuthFilter.doFilterInternal(request, response, filterChain);

            
            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("Should not authenticate with non-Bearer token")
        void doFilterInternal_WithNonBearerToken_ShouldNotAuthenticate() throws Exception {
            
            when(request.getHeader("Authorization")).thenReturn("Basic sometoken");

            
            jwtAuthFilter.doFilterInternal(request, response, filterChain);

            
            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("Should handle exception gracefully")
        void doFilterInternal_WhenExceptionThrown_ShouldContinueChain() throws Exception {
            
            String token = "error-token";
            when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
            when(jwtTokenProvider.validateToken(token)).thenThrow(new RuntimeException("Token error"));

            
            jwtAuthFilter.doFilterInternal(request, response, filterChain);

            
            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("Should handle empty Bearer token")
        void doFilterInternal_WithEmptyBearerToken_ShouldNotAuthenticate() throws Exception {
            
            when(request.getHeader("Authorization")).thenReturn("Bearer ");

            
            jwtAuthFilter.doFilterInternal(request, response, filterChain);

            
            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }
    }
}


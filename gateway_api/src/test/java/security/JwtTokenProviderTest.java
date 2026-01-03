package security;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtTokenProvider Unit Tests")
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        // Test için secret key (en az 32 karakter olmalı)
        ReflectionTestUtils.setField(jwtTokenProvider, "secretKey",
                "testSecretKey12345678901234567890123456789012345678");
        ReflectionTestUtils.setField(jwtTokenProvider, "expirationMs", 3600000L);
        jwtTokenProvider.init();
    }

    @Nested
    @DisplayName("Token Generation Tests")
    class TokenGenerationTests {

        @Test
        @DisplayName("Should generate valid token for username")
        void generateToken_ShouldCreateValidToken() {
            // Given
            String username = "testuser";

            // When
            String token = jwtTokenProvider.generateToken(username);

            // Then
            assertThat(token).isNotNull();
            assertThat(token).isNotEmpty();
            assertThat(token.split("\\.")).hasSize(3); // JWT format: header.payload.signature
        }

        @Test
        @DisplayName("Should generate different tokens for same user")
        void generateToken_ShouldCreateUniqueTokens() throws InterruptedException {
            String username = "testuser";

            String token1 = jwtTokenProvider.generateToken(username);
            Thread.sleep(1000);
            String token2 = jwtTokenProvider.generateToken(username);

            assertThat(token1).isNotEqualTo(token2);
        }
    }

    @Nested
    @DisplayName("Token Validation Tests")
    class TokenValidationTests {

        @Test
        @DisplayName("Should validate correct token")
        void validateToken_WithValidToken_ShouldReturnTrue() {
            // Given
            String token = jwtTokenProvider.generateToken("testuser");

            // When
            boolean isValid = jwtTokenProvider.validateToken(token);

            // Then
            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("Should reject invalid token")
        void validateToken_WithInvalidToken_ShouldReturnFalse() {
            // Given
            String invalidToken = "invalid.token.here";

            // When
            boolean isValid = jwtTokenProvider.validateToken(invalidToken);

            // Then
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("Should reject malformed token")
        void validateToken_WithMalformedToken_ShouldReturnFalse() {
            // Given
            String malformedToken = "not-a-jwt-token";

            // When
            boolean isValid = jwtTokenProvider.validateToken(malformedToken);

            // Then
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("Should reject expired token")
        void validateToken_WithExpiredToken_ShouldReturnFalse() {
            // Given - Create provider with very short expiration
            JwtTokenProvider shortExpiryProvider = new JwtTokenProvider();
            ReflectionTestUtils.setField(shortExpiryProvider, "secretKey",
                    "testSecretKey12345678901234567890123456789012345678");
            ReflectionTestUtils.setField(shortExpiryProvider, "expirationMs", 1L); // 1ms expiration
            shortExpiryProvider.init();

            String token = shortExpiryProvider.generateToken("testuser");

            // Wait for token to expire
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // When
            boolean isValid = shortExpiryProvider.validateToken(token);

            // Then
            assertThat(isValid).isFalse();
        }
    }

    @Nested
    @DisplayName("Token Parsing Tests")
    class TokenParsingTests {

        @Test
        @DisplayName("Should extract username from token")
        void getUsernameToken_ShouldReturnCorrectUsername() {
            // Given
            String username = "testuser";
            String token = jwtTokenProvider.generateToken(username);

            // When
            String extractedUsername = jwtTokenProvider.getUsernameToken(token);

            // Then
            assertThat(extractedUsername).isEqualTo(username);
        }

        @Test
        @DisplayName("Should extract different usernames correctly")
        void getUsernameToken_WithDifferentUsers_ShouldReturnCorrectly() {
            // Given
            String token1 = jwtTokenProvider.generateToken("user1");
            String token2 = jwtTokenProvider.generateToken("user2");

            // When
            String username1 = jwtTokenProvider.getUsernameToken(token1);
            String username2 = jwtTokenProvider.getUsernameToken(token2);

            // Then
            assertThat(username1).isEqualTo("user1");
            assertThat(username2).isEqualTo("user2");
        }
    }

    @Nested
    @DisplayName("Expiration Tests")
    class ExpirationTests {

        @Test
        @DisplayName("Should return correct expiration time")
        void getTokenExpirationMillis_ShouldReturnConfiguredValue() {
            // When
            Long expiration = jwtTokenProvider.getTokenExpirationMillis();

            // Then
            assertThat(expiration).isEqualTo(3600000L);
        }
    }
}


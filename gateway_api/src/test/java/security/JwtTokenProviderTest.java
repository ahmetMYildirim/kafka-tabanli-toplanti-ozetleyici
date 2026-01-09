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
            
            String username = "testuser";

            
            String token = jwtTokenProvider.generateToken(username);

            
            assertThat(token).isNotNull();
            assertThat(token).isNotEmpty();
            assertThat(token.split("\\.")).hasSize(3); 
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
            
            String token = jwtTokenProvider.generateToken("testuser");

            
            boolean isValid = jwtTokenProvider.validateToken(token);

            
            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("Should reject invalid token")
        void validateToken_WithInvalidToken_ShouldReturnFalse() {
            
            String invalidToken = "invalid.token.here";

            
            boolean isValid = jwtTokenProvider.validateToken(invalidToken);

            
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("Should reject malformed token")
        void validateToken_WithMalformedToken_ShouldReturnFalse() {
            
            String malformedToken = "not-a-jwt-token";

            
            boolean isValid = jwtTokenProvider.validateToken(malformedToken);

            
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("Should reject expired token")
        void validateToken_WithExpiredToken_ShouldReturnFalse() {
            
            JwtTokenProvider shortExpiryProvider = new JwtTokenProvider();
            ReflectionTestUtils.setField(shortExpiryProvider, "secretKey",
                    "testSecretKey12345678901234567890123456789012345678");
            ReflectionTestUtils.setField(shortExpiryProvider, "expirationMs", 1L); 
            shortExpiryProvider.init();

            String token = shortExpiryProvider.generateToken("testuser");

            
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            
            boolean isValid = shortExpiryProvider.validateToken(token);

            
            assertThat(isValid).isFalse();
        }
    }

    @Nested
    @DisplayName("Token Parsing Tests")
    class TokenParsingTests {

        @Test
        @DisplayName("Should extract username from token")
        void getUsernameToken_ShouldReturnCorrectUsername() {
            
            String username = "testuser";
            String token = jwtTokenProvider.generateToken(username);

            
            String extractedUsername = jwtTokenProvider.getUsernameToken(token);

            
            assertThat(extractedUsername).isEqualTo(username);
        }

        @Test
        @DisplayName("Should extract different usernames correctly")
        void getUsernameToken_WithDifferentUsers_ShouldReturnCorrectly() {
            
            String token1 = jwtTokenProvider.generateToken("user1");
            String token2 = jwtTokenProvider.generateToken("user2");

            
            String username1 = jwtTokenProvider.getUsernameToken(token1);
            String username2 = jwtTokenProvider.getUsernameToken(token2);

            
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
            
            Long expiration = jwtTokenProvider.getTokenExpirationMillis();

            
            assertThat(expiration).isEqualTo(3600000L);
        }
    }
}


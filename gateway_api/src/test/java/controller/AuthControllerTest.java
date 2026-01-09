package controller;

import model.dto.ApiResponse;
import model.dto.LoginRequest;
import model.dto.LoginResponse;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import security.JwtTokenProvider;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController Unit Tests")
class AuthControllerTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthController authController;

    @Nested
    @DisplayName("Login Tests")
    class LoginTests {

        @Test
        @DisplayName("Should return token for valid credentials")
        void login_WithValidCredentials_ShouldReturnToken() {
            
            LoginRequest request = new LoginRequest();
            request.setUsername("admin");
            request.setPassword("admin123");

            when(jwtTokenProvider.generateToken("admin")).thenReturn("test-jwt-token");
            when(jwtTokenProvider.getTokenExpirationMillis()).thenReturn(3600000L);

            
            ResponseEntity<ApiResponse<LoginResponse>> response = authController.login(request);

            
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isTrue();
            assertThat(response.getBody().getData().getAccessToken()).isEqualTo("test-jwt-token");
            assertThat(response.getBody().getData().getTokenType()).isEqualTo("Bearer");
            assertThat(response.getBody().getData().getUsername()).isEqualTo("admin");
        }

        @Test
        @DisplayName("Should return error for invalid username")
        void login_WithInvalidUsername_ShouldReturnError() {
            
            LoginRequest request = new LoginRequest();
            request.setUsername("wronguser");
            request.setPassword("admin123");

            
            ResponseEntity<ApiResponse<LoginResponse>> response = authController.login(request);

            
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isFalse();
        }

        @Test
        @DisplayName("Should return error for invalid password")
        void login_WithInvalidPassword_ShouldReturnError() {
            
            LoginRequest request = new LoginRequest();
            request.setUsername("admin");
            request.setPassword("wrongpassword");

            
            ResponseEntity<ApiResponse<LoginResponse>> response = authController.login(request);

            
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isFalse();
            assertThat(response.getBody().getMessage()).contains("Gecersiz");
        }
    }

    @Nested
    @DisplayName("Token Refresh Tests")
    class RefreshTests {

        @Test
        @DisplayName("Should refresh valid token")
        void refresh_WithValidToken_ShouldReturnNewToken() {
            
            String authHeader = "Bearer valid-token";

            when(jwtTokenProvider.validateToken("valid-token")).thenReturn(true);
            when(jwtTokenProvider.getUsernameToken("valid-token")).thenReturn("admin");
            when(jwtTokenProvider.generateToken("admin")).thenReturn("new-jwt-token");
            when(jwtTokenProvider.getTokenExpirationMillis()).thenReturn(3600000L);

            
            ResponseEntity<ApiResponse<LoginResponse>> response = authController.refresh(authHeader);

            
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isTrue();
            assertThat(response.getBody().getData().getAccessToken()).isEqualTo("new-jwt-token");
        }

        @Test
        @DisplayName("Should return error for invalid token")
        void refresh_WithInvalidToken_ShouldReturnError() {
            
            String authHeader = "Bearer invalid-token";

            when(jwtTokenProvider.validateToken("invalid-token")).thenReturn(false);

            
            ResponseEntity<ApiResponse<LoginResponse>> response = authController.refresh(authHeader);

            
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isFalse();
        }
    }
}


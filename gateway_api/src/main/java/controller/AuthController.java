package controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import model.dto.ApiResponse;
import model.dto.LoginRequest;
import model.dto.LoginResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import security.JwtTokenProvider;

/**
 * AuthController - Kullanıcı kimlik doğrulama ve token yönetimi kontrolcüsü
 * 
 * Bu kontrolcü, sistemde kullanıcı authentication işlemlerini yönetir.
 * JWT (JSON Web Token) tabanlı stateless authentication kullanılır.
 * 
 * Sunulan Endpoint'ler:
 * - POST /api/v1/auth/login - Kullanıcı girişi ve JWT token oluşturma
 * - POST /api/v1/auth/refresh - Token yenileme (refresh token)
 * 
 * Güvenlik Özellikleri:
 * - JWT token tabanlı kimlik doğrulama (stateless)
 * - BCrypt ile şifre hashleme (planned - şu an demo mode)
 * - Token süre sınırlaması (configurable via application.properties)
 * - Rate limiting (RateLimitFilter ile korunur)
 * 
 * Demo Kullanıcı Bilgileri:
 * - Username: admin
 * - Password: admin123
 * 
 * Önemli Not: 
 * Demo amaçlı sabit kullanıcı bilgileri kullanılmaktadır.
 * Üretim (production) ortamında mutlaka veritabanı entegrasyonu
 * ve gerçek kullanıcı yönetimi implementasyonu yapılmalıdır.
 * 
 * Token Yapısı:
 * - Header: Algorithm (HS256)
 * - Payload: username, iat, exp
 * - Signature: HMAC SHA256 with secret key
 * 
 * @author Ahmet
 * @version 1.0
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "User login, token generation and refresh endpoints")
public class AuthController {
    
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    private static final String DEMO_USERNAME = "admin";
    private static final String DEMO_PASSWORD = "admin123";

    /**
     * Kullanıcı giriş işlemi yapar ve JWT token döndürür.
     * 
     * İşlem Akışı:
     * 1. LoginRequest validation (@Valid annotation ile)
     * 2. Kullanıcı adı ve şifre kontrolü (demo: hardcoded credentials)
     * 3. Başarılı ise JWT token oluşturma (JwtTokenProvider ile)
     * 4. Token ve kullanıcı bilgilerini LoginResponse olarak döndürme
     * 
     * Token İçeriği:
     * - accessToken: JWT string (HS256 signed)
     * - tokenType: "Bearer" (OAuth 2.0 standard)
     * - expiresIn: Token geçerlilik süresi (saniye cinsinden)
     * - username: Kullanıcı adı
     * 
     * Güvenlik:
     * - Rate limiting uygulanır (max 5 request/min)
     * - Başarısız girişler loglanır
     * - Production'da brute-force protection eklenmeli
     * 
     * @param loginRequest Kullanıcı adı ve şifre içeren istek body'si
     * @return JWT token ve kullanıcı bilgileri (200 OK) veya hata mesajı (400 Bad Request)
     */
    @PostMapping("/login")
    @Operation(summary = "User login - Generates JWT token")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest loginRequest) {
        
        String username = loginRequest.getUsername();
        String password = loginRequest.getPassword();
        
        log.info("Login attempt. Username: {}", username);
        
        if (!DEMO_USERNAME.equals(username) || !DEMO_PASSWORD.equals(password)) {
            log.warn("Failed login attempt. Username: {}", username);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.failure("Invalid username or password"));
        }
        
        String token = jwtTokenProvider.generateToken(username);
        long expiresIn = jwtTokenProvider.getTokenExpirationMillis() / 1000;
        
        LoginResponse response = LoginResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .username(username)
                .build();
        
        log.info("Successful login. Username: {}", username);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Mevcut JWT token'ı yenileyerek yeni token döndürür (refresh token mechanism).
     * 
     * İşlem Akışı:
     * 1. Authorization header'dan Bearer token çıkarılır
     * 2. Mevcut token geçerliliği kontrolü (signature, expiration)
     * 3. Token'dan kullanıcı bilgisi extract edilir
     * 4. Yeni JWT token oluşturulur (yeni expiration time ile)
     * 5. Yeni token döndürülür
     * 
     * Kullanım Senaryosu:
     * - Frontend, token süresi dolmadan (örn. 5 dk önce) bu endpoint'i çağırır
     * - Seamless user experience sağlanır (logout olmadan token yenilenir)
     * - Sliding window authentication pattern
     * 
     * Authorization Header Formatı:
     * "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
     * 
     * @param authHeader Authorization header (Bearer token formatında)
     * @return Yenilenmiş JWT token (200 OK) veya hata mesajı (400 Bad Request)
     */
    @PostMapping("/refresh")
    @Operation(summary = "Refresh JWT token - Renews token before expiration")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(
            @RequestHeader("Authorization") String authHeader) {
        
        String token = authHeader.substring(7); 
        
        log.debug("Token refresh request received");
        
        if (!jwtTokenProvider.validateToken(token)) {
            log.warn("Invalid token refresh attempt");
            return ResponseEntity.badRequest()
                    .body(ApiResponse.failure("Invalid or expired token"));
        }
        
        String username = jwtTokenProvider.getUsernameToken(token);
        String newToken = jwtTokenProvider.generateToken(username);
        long expiresIn = jwtTokenProvider.getTokenExpirationMillis() / 1000;
        
        LoginResponse response = LoginResponse.builder()
                .accessToken(newToken)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .username(username)
                .build();
        
        log.info("Token refreshed successfully. Username: {}", username);
        return ResponseEntity.ok(ApiResponse.success(response, "Token refreshed successfully"));
    }
}

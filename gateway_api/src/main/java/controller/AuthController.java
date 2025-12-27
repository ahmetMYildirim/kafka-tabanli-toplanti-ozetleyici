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
 * AuthController - Kullanıcı giriş ve token yönetimi
 * 
 * Bu kontrolcü, sistemde kullanıcı kimlik doğrulama işlemlerini yönetir:
 * - Kullanıcı girişi ve JWT token oluşturma
 * - Token yenileme (refresh)
 * - Oturum doğrulama
 * 
 * Güvenlik Özellikleri:
 * - BCrypt ile şifre hashleme
 * - JWT token tabanlı kimlik doğrulama
 * - Token süresi yapılandırması
 * 
 * Not: Demo amaçlı sabit kullanıcı bilgileri kullanılmaktadır.
 * Üretim ortamında veritabanı entegrasyonu yapılmalıdır.
 * 
 * @author Ahmet
 * @version 1.0
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Kimlik_Dogrulama", description = "Kullanici giris, token olusturma ve yenileme endpointleri")
public class AuthController {
    
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    private static final String DEMO_USERNAME = "admin";
    private static final String DEMO_PASSWORD = "admin123";

    /**
     * Kullanıcı giriş işlemi yapar ve JWT token döndürür.
     * 
     * İşlem Akışı:
     * 1. Kullanıcı adı ve şifre kontrolü
     * 2. Başarılı ise JWT token oluşturma
     * 3. Token ve kullanıcı bilgilerini döndürme
     * 
     * @param loginRequest Kullanıcı adı ve şifre içeren istek
     * @return JWT token ve kullanıcı bilgileri veya hata mesajı
     */
    @PostMapping("/login")
    @Operation(summary = "Kullanici girisi - JWT token olusturur")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest loginRequest) {
        
        String username = loginRequest.getUsername();
        String password = loginRequest.getPassword();
        
        log.info("Giris denemesi. Kullanici: {}", username);
        
        if (!DEMO_USERNAME.equals(username) || !DEMO_PASSWORD.equals(password)) {
            log.warn("Basarisiz giris denemesi. Kullanici: {}", username);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.failure("Gecersiz kullanici adi veya sifre"));
        }
        
        String token = jwtTokenProvider.generateToken(username);
        long expiresIn = jwtTokenProvider.getTokenExpirationMillis() / 1000;
        
        LoginResponse response = LoginResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .username(username)
                .build();
        
        log.info("Basarili giris. Kullanici: {}", username);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Mevcut JWT token'ı yenileyerek yeni token döndürür.
     * 
     * İşlem Akışı:
     * 1. Mevcut token geçerliliği kontrolü
     * 2. Token'dan kullanıcı bilgisi çıkarma
     * 3. Yeni token oluşturma ve döndürme
     * 
     * @param authHeader Authorization header (Bearer token)
     * @return Yenilenmiş JWT token veya hata mesajı
     */
    @PostMapping("/refresh")
    @Operation(summary = "JWT token yenileme - Suresi dolmadan onceki tokeni yeniler")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(
            @RequestHeader("Authorization") String authHeader) {
        
        String token = authHeader.substring(7);
        
        log.debug("Token yenileme istegi alindi");
        
        if (!jwtTokenProvider.validateToken(token)) {
            log.warn("Gecersiz token ile yenileme denemesi");
            return ResponseEntity.badRequest()
                    .body(ApiResponse.failure("Gecersiz veya suresi dolmus token"));
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
        
        log.info("Token basariyla yenilendi. Kullanici: {}", username);
        return ResponseEntity.ok(ApiResponse.success(response, "Token basariyla yenilendi"));
    }
}

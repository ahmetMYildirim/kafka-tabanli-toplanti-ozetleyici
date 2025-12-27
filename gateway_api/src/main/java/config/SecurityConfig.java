package config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import security.JwtAuthFilter;
import security.RateLimitFilter;

/**
 * SecurityConfig - API Gateway güvenlik ayarları
 * 
 * Bu sınıf, uygulamanın güvenlik politikalarını tanımlar:
 * - JWT tabanlı kimlik doğrulama
 * - Rate limiting (istek sınırlandırma)
 * - Endpoint erişim kontrolü
 * - CORS politikaları
 * 
 * Açık Endpointler (JWT gerektirmez):
 * - /api/v1/auth/** : Giriş ve kayıt işlemleri
 * - /ws/** : WebSocket bağlantıları
 * - /actuator/health : Sağlık kontrolü
 * - /swagger-ui/** : API dokümantasyonu
 * 
 * @author Ahmet
 * @version 1.0
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {
    
    private final JwtAuthFilter jwtAuthFilter;
    private final RateLimitFilter rateLimitFilter;
    
    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/v1/auth/**",
            "/ws/**",
            "/actuator/health",
            "/swagger-ui/**",
            "/v3/api-docs/**"
    };

    /**
     * HTTP güvenlik filtre zincirini yapılandırır.
     * Tüm HTTP istekleri bu zincirden geçer.
     * 
     * Filtre Sırası:
     * 1. Rate Limit kontrolü
     * 2. JWT token doğrulama
     * 3. Endpoint yetkilendirme
     * 
     * @param http Spring Security HTTP yapılandırıcı
     * @return Yapılandırılmış güvenlik filtre zinciri
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        log.info("Guvenlik yapilandirmasi baslatiliyor...");
        
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.maximumSessions(1))
            .authorizeHttpRequests(auth -> {
                for (String endpoint : PUBLIC_ENDPOINTS) {
                    auth.requestMatchers(endpoint).permitAll();
                }
                auth.anyRequest().authenticated();
            })
            .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        
        log.info("Guvenlik yapilandirmasi tamamlandi. Acik endpoint sayisi: {}", PUBLIC_ENDPOINTS.length);
        
        return http.build();
    }

    /**
     * Şifre şifreleyici bean'ini oluşturur.
     * BCrypt algoritması ile güvenli şifre hashleme sağlar.
     * 
     * @return BCrypt tabanlı şifre şifreleyici
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        log.debug("BCrypt sifre sifreleyici olusturuldu");
        return new BCryptPasswordEncoder();
    }
}

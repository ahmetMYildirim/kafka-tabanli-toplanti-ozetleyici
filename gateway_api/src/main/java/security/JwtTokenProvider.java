package security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JwtTokenProvider - JWT token işlemlerini yöneten sınıf
 * 
 * Bu sınıf, kullanıcıların kimlik doğrulaması için JWT (JSON Web Token)
 * oluşturma, doğrulama ve çözümleme işlemlerini gerçekleştirir.
 * 
 * Güvenlik Özellikleri:
 * - HMAC-SHA256 algoritması ile imzalama
 * - Yapılandırılabilir geçerlilik süresi
 * - Otomatik token süresi kontrolü
 * 
 * Kullanım Alanları:
 * - API Gateway kimlik doğrulama
 * - Mikroservisler arası güvenli iletişim
 * - Kullanıcı oturum yönetimi
 * 
 * @author Ahmet
 * @version 1.0
 */
@Component
@Slf4j
public class JwtTokenProvider {
    
    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long expirationMs;

    private SecretKey key;

    /**
     * Uygulama başlatıldığında kriptografik anahtarı hazırlar.
     * HMAC-SHA256 algoritması için uygun formata dönüştürür.
     */
    @PostConstruct
    public void init() {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        this.key = Keys.hmacShaKeyFor(keyBytes);
        log.info("JWT kriptografik anahtar basariyla olusturuldu");
    }

    /**
     * Belirtilen kullanıcı adı için yeni bir JWT token oluşturur.
     * Token içinde kullanıcı kimliği, oluşturulma ve bitiş zamanı bulunur.
     * 
     * @param username Token'a gömülecek kullanıcı kimliği
     * @return İmzalanmış JWT token metni
     */
    public String generateToken(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        String token = Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
        
        log.debug("Yeni JWT token olusturuldu. Kullanici: {}", username);
        return token;
    }

    /**
     * JWT token içerisinden kullanıcı adını çıkarır.
     * Token imzası doğrulandıktan sonra payload'dan kullanıcı bilgisi alınır.
     * 
     * @param token Çözümlenecek JWT token
     * @return Token'daki kullanıcı adı
     */
    public String getUsernameToken(String token) {
        Claims claims = parseToken(token);
        return claims.getSubject();
    }
    
    private Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * JWT token'ın geçerliliğini kontrol eder.
     * İmza doğrulaması ve süre kontrolü yapar.
     * 
     * @param token Doğrulanacak JWT token
     * @return Token geçerli ise true, değilse false
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            log.debug("JWT token dogrulama basarili");
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT token suresi dolmus: {}", e.getMessage());
            return false;
        } catch (JwtException e) {
            log.error("JWT token dogrulama hatasi: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Token geçerlilik süresini milisaniye cinsinden döndürür.
     * Frontend tarafında token yenileme zamanları için kullanılır.
     * 
     * @return Geçerlilik süresi (milisaniye)
     */
    public Long getTokenExpirationMillis() {
        return expirationMs;
    }
}

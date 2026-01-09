package security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RateLimitFilter - İstek sınırlandırma (Rate Limiting) filtresi
 * 
 * Bu filtre, API'ye gelen istekleri sınırlandırarak aşırı kullanımı engeller.
 * Her kullanıcı/IP için ayrı bir token bucket tutulur.
 * 
 * Sınırlandırma Stratejisi:
 * - Kimlik doğrulaması yapılmış kullanıcılar: user:{username}
 * - Anonim kullanıcılar: ip:{ip_address}
 * 
 * Token Bucket Algoritması:
 * - Belirli sürede izin verilen maksimum istek sayısı
 * - Süre dolduğunda bucket yeniden dolar
 * 
 * İstisna Endpoint'leri:
 * - /ws/** : WebSocket bağlantıları
 * - /actuator/health : Sağlık kontrolü
 * 
 * @author Ahmet
 * @version 1.0
 */
@Component
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Value("${rate-limit.requests-per-minute}")
    private int requestsPerMinute;

    @Value("${rate-limit.requests-per-second}")
    private int requestsPerSecond;

    /**
     * İstek sınırlandırma kontrolü yapar.
     * 
     * @param request HTTP isteği
     * @param response HTTP yanıtı
     * @param filterChain Filtre zinciri
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
            FilterChain filterChain) throws ServletException, IOException {
        
        String key = resolveKey(request);
        Bucket bucket = buckets.computeIfAbsent(key, k -> createNewBucket());

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            log.warn("Rate limit exceeded. Key: {}", key);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Request limit exceeded. Please try again later.\"}");
        }
    }

    /**
     * Yeni bir token bucket oluşturur.
     * 
     * @return Yapılandırılmış Bucket
     */
    private Bucket createNewBucket() {
        Bandwidth limit = Bandwidth.classic(requestsPerMinute, 
                Refill.greedy(requestsPerMinute, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }

    /**
     * İstek için rate limit anahtarını belirler.
     * Kimlik doğrulaması varsa kullanıcı adı, yoksa IP adresi kullanılır.
     * 
     * @param request HTTP isteği
     * @return Rate limit anahtarı
     */
    private String resolveKey(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() != null) {
            return "user:" + auth.getPrincipal().toString();
        }

        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return "ip:" + xForwardedFor.split(",")[0].trim();
        }
        return "ip:" + request.getRemoteAddr();
    }

    /**
     * Rate limit uygulanmayacak endpoint'leri belirler.
     * 
     * @param request HTTP isteği
     * @return true ise rate limit uygulanmaz
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getServletPath();
        return path.startsWith("/ws/") || path.equals("/actuator/health");
    }
}

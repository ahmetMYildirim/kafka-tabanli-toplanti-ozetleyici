package security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JwtAuthFilter - JWT token doğrulama filtresi
 * 
 * Bu filtre, gelen HTTP isteklerindeki JWT token'ları doğrular
 * ve geçerli token'lar için Spring Security context'ini ayarlar.
 * 
 * İşlem Akışı:
 * 1. Authorization header'dan Bearer token alınır
 * 2. Token geçerliliği kontrol edilir
 * 3. Geçerli ise SecurityContext'e authentication eklenir
 * 4. Geçersiz ise istek anonim olarak devam eder
 * 
 * Uygulama: Her HTTP isteğinde bir kez çalışır (OncePerRequestFilter)
 * 
 * @author Ahmet
 * @version 1.0
 */
@Component
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    /**
     * Gelen isteği JWT token ile doğrular.
     * 
     * @param request HTTP isteği
     * @param response HTTP yanıtı
     * @param filterChain Filtre zinciri
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
            FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = extractJwtFromRequest(request);
            
            if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)) {
                String username = jwtTokenProvider.getUsernameToken(jwt);

                UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(username, null, Collections.emptyList());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("User authenticated: {} - URI: {}", username, request.getRequestURI());
            }
        } catch (Exception e) {
            log.error("User authentication error: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    /**
     * HTTP isteğinden JWT token'ı çıkarır.
     * 
     * @param request HTTP isteği
     * @return JWT token veya null
     */
    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}

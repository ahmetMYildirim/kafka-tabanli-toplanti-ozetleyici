package config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

/**
 * CORSConfig - Cross-Origin Resource Sharing yapılandırması
 * 
 * Bu sınıf, farklı origin'lerden (domain) gelen HTTP isteklerine
 * izin vermek için CORS politikalarını yapılandırır.
 * 
 * İzin Verilen:
 * - Origins: application.properties'den okunur
 * - Methods: GET, POST, PUT, DELETE, OPTIONS
 * - Headers: Tümü (*)
 * - Credentials: Cookie ve auth header gönderimi
 * 
 * Kullanım: Frontend (React, Vue vb.) uygulamalarının
 * API Gateway'e erişmesi için gereklidir.
 * 
 * @author Ahmet
 * @version 1.0
 */
@Configuration
public class CORSConfig {

    @Value("${cors.allowed-origins}")
    private String allowedOrigins;

    /**
     * CORS filtresi oluşturur.
     * Tüm endpoint'lere uygulanır.
     * 
     * @return Yapılandırılmış CorsFilter
     */
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}

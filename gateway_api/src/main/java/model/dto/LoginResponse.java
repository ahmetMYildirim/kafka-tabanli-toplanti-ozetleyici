package model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * LoginResponse - Kullanıcı giriş yanıtı DTO'su
 * 
 * Bu sınıf, başarılı giriş işlemi sonrasında döndürülen
 * JWT token ve kullanıcı bilgilerini içerir.
 * 
 * İçerik:
 * - accessToken: JWT token değeri
 * - tokenType: Token tipi (Bearer)
 * - expiresIn: Token geçerlilik süresi (saniye)
 * - username: Giriş yapan kullanıcı adı
 * 
 * @author Ahmet
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    
    /** JWT erişim token'ı */
    private String accessToken;
    
    /** Token tipi - her zaman "Bearer" */
    private String tokenType;
    
    /** Token geçerlilik süresi (saniye cinsinden) */
    private Long expiresIn;
    
    /** Giriş yapan kullanıcının adı */
    private String username;
}

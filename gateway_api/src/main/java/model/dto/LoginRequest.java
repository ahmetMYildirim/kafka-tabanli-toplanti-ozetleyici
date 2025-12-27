package model.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

/**
 * LoginRequest - Kullanıcı giriş isteği DTO'su
 * 
 * Bu sınıf, /api/v1/auth/login endpoint'ine gelen
 * giriş isteklerinin veri formatını tanımlar.
 * 
 * Validasyon:
 * - username: Boş olamaz
 * - password: Boş olamaz
 * 
 * @author Ahmet
 * @version 1.0
 */
@Data
public class LoginRequest {
    
    /** Kullanıcı adı - zorunlu alan */
    @NotBlank(message = "Username is required")
    private String username;

    /** Şifre - zorunlu alan */
    @NotBlank(message = "Password is required")
    private String password;
}

package model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * ApiResponse - Standart API yanıt formatı
 * 
 * Bu sınıf, tüm REST API endpoint'lerinden dönen yanıtlar için
 * tutarlı bir format sağlar.
 * 
 * Format:
 * - success: İşlem başarılı mı (true/false)
 * - message: Kullanıcıya gösterilecek mesaj
 * - data: Asıl veri (generic tip)
 * - timestamp: Yanıt zamanı
 * 
 * Kullanım: Tüm controller'lardan dönen yanıtlar bu formatta olmalıdır.
 * 
 * @author Ahmet
 * @version 1.0
 * @param <T> Yanıt verisinin tipi
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    
    /** İşlem başarı durumu */
    private boolean success;
    
    /** Kullanıcıya gösterilecek mesaj */
    private String message;
    
    /** Yanıt verisi */
    private T data;
    
    /** Yanıt oluşturulma zamanı */
    private Instant timestamp;

    /**
     * Başarılı yanıt oluşturur (sadece veri ile).
     * 
     * @param data Döndürülecek veri
     * @return Başarılı ApiResponse
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message("Success")
                .data(data)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Başarılı yanıt oluşturur (veri ve mesaj ile).
     * 
     * @param data Döndürülecek veri
     * @param message Özel mesaj
     * @return Başarılı ApiResponse
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Başarısız yanıt oluşturur.
     * 
     * @param message Hata mesajı
     * @return Başarısız ApiResponse
     */
    public static <T> ApiResponse<T> failure(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .timestamp(Instant.now())
                .build();
    }
}

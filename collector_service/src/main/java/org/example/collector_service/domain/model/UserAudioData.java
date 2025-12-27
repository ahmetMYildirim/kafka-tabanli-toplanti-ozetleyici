package org.example.collector_service.domain.model;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * UserAudioData - Kullanıcı ses verisi yönetim sınıfı
 * 
 * Discord ses kanalından gelen her kullanıcının ses verilerini
 * geçici PCM dosyalarına yazar. Stream halinde gelen ses verisini
 * bufferlayarak daha sonra MP3'e dönüştürülmek üzere saklar.
 * 
 * Kullanım:
 * - Gerçek zamanlı ses akışı yakalama
 * - Kullanıcı bazlı ses ayrıştırma
 * - Geçici dosya yönetimi
 * 
 * Dosya Formatı: PCM (44100 Hz, 16-bit, stereo)
 * Klasör: audio_storage/temp_{userId}.pcm
 * 
 * @author Ahmet
 * @version 1.0
 */
@Slf4j
@Getter
public class UserAudioData {
    
    private final String userId;
    private final String userName;
    private final File tempPcmData;
    private FileOutputStream outputStream;
    private volatile boolean closed = false;

    /**
     * Constructor - Kullanıcı için geçici ses dosyası oluşturur.
     *
     * @param userId   Discord veya Zoom kullanıcı ID'si
     * @param userName Kullanıcının görünen adı
     */
    public UserAudioData(String userId, String userName) {
        this.userId = userId;
        this.userName = userName;
        this.tempPcmData = new File("audio_storage/temp_" + userId + ".pcm");

        try {
            if (tempPcmData.exists()) {
                if (!tempPcmData.delete()) {
                    log.warn("Eski PCM dosyası silinemedi: {}", tempPcmData.getPath());
                }
            }
            this.outputStream = new FileOutputStream(tempPcmData, true);
        } catch (Exception e) {
            throw new RuntimeException("Kullanıcı için output stream oluşturulamadı: " + userName, e);
        }
    }

    /**
     * Output stream'i güvenli şekilde kapatır.
     * İdempotent: Birden fazla çağrı güvenlidir.
     *
     * @throws IOException Dosya kapatma hatası
     */
    public synchronized void closeOutputStream() throws IOException {
        if (outputStream != null && !closed) {
            closed = true;
            outputStream.flush();
            outputStream.close();
            log.debug("Output stream kapatıldı: {}", userId);
        }
    }

    /**
     * Ses verilerini dosyaya yazar.
     * Thread-safe: Synchronized metod.
     *
     * @param data PCM ses byte array
     * @throws IOException Dosya yazma hatası veya stream kapalı hatası
     */
    public synchronized void writeAudioData(byte[] data) throws IOException {
        if (closed) {
            throw new IOException("Output stream zaten kapatılmış: " + userId);
        }
        if (outputStream != null) {
            outputStream.write(data);
            outputStream.flush();
        }
    }
}


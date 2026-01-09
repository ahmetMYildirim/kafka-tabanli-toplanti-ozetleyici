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
 * Kullanım Senaryoları:
 * - Gerçek zamanlı ses akışı yakalama
 * - Kullanıcı bazlı ses ayrıştırma (her kullanıcı için ayrı dosya)
 * - Geçici dosya yönetimi (PCM temp files)
 * 
 * Teknik Detaylar:
 * - Dosya Formatı: PCM (48000 Hz, 16-bit, stereo, big-endian)
 * - Klasör: audio_storage/temp_{userId}.pcm
 * - Thread-Safe: writeAudioData ve closeOutputStream synchronized
 * 
 * İş Akışı:
 * 1. Constructor ile geçici PCM dosyası oluşturulur
 * 2. writeAudioData() ile stream halinde veri yazılır
 * 3. closeOutputStream() ile stream kapatılır
 * 4. FFmpeg ile PCM -> MP3 dönüşümü yapılır
 * 5. Geçici PCM dosyası silinir
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
     * Eski PCM dosyası varsa silinir ve yeni bir FileOutputStream açılır.
     * Append mode kullanılarak stream halinde veri yazılabilir.
     *
     * @param userId   Discord veya Zoom kullanıcı ID'si (snowflake ID)
     * @param userName Kullanıcının görünen adı
     * @throws RuntimeException FileOutputStream oluşturulamazsa
     */
    public UserAudioData(String userId, String userName) {
        this.userId = userId;
        this.userName = userName;
        this.tempPcmData = new File("audio_storage/temp_" + userId + ".pcm");

        try {
            if (tempPcmData.exists()) {
                if (!tempPcmData.delete()) {
                    log.warn("Could not delete old PCM file: {}", tempPcmData.getPath());
                }
            }
            this.outputStream = new FileOutputStream(tempPcmData, true);
        } catch (Exception e) {
            throw new RuntimeException("Could not create output stream for user: " + userName, e);
        }
    }

    /**
     * Output stream'i güvenli şekilde kapatır.
     * 
     * İdempotent: Birden fazla çağrı güvenlidir, closed flag kontrol edilir.
     * Thread-Safe: synchronized metod, race condition engellenmiştir.
     * 
     * Kapatma öncesi flush() çağrılarak buffer'daki tüm veri dosyaya yazılır.
     *
     * @throws IOException Dosya kapatma hatası veya flush hatası
     */
    public synchronized void closeOutputStream() throws IOException {
        if (outputStream != null && !closed) {
            closed = true;
            outputStream.flush();
            outputStream.close();
            log.debug("Output stream closed: userId={}", userId);
        }
    }

    /**
     * Ses verilerini dosyaya yazar ve flush eder.
     * 
     * Thread-Safe: synchronized metod, aynı anda birden fazla thread
     * çağırsa bile data corruption olmaz.
     * 
     * Stream kapalıysa IOException fırlatılır. Bu durum, Discord'dan
     * gelen audio packet'lerin stream kapatıldıktan sonra geldiğini gösterir.
     *
     * @param data PCM ses byte array (Discord'dan gelen 20ms'lik ses chunk'ı)
     * @throws IOException Dosya yazma hatası veya stream kapalı hatası
     */
    public synchronized void writeAudioData(byte[] data) throws IOException {
        if (closed) {
            throw new IOException("Output stream already closed: " + userId);
        }
        if (outputStream != null) {
            outputStream.write(data);
            outputStream.flush();
        }
    }
}


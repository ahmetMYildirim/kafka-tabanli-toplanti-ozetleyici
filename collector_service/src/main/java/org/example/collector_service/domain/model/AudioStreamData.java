package org.example.collector_service.domain.model;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * AudioStreamData - Ses akışı buffer yönetim sınıfı
 * 
 * WebSocket üzerinden gelen ses verilerini bellekte bufferlar.
 * Belirli bir boyuta ulaştığında chunk'lar halinde işlenmek üzere
 * verileri döndürür.
 * 
 * Özellikler:
 * - Dinamik buffer yönetimi (ByteArrayOutputStream ile otomatik genişleme)
 * - Chunk sayacı (ses segmentlerini takip eder)
 * - Oturum zaman damgası (dosya isimlendirme için)
 * 
 * Teknik Detaylar:
 * - Chunk Boyutu Eşiği (Threshold): 40,960 byte (4096 * 10)
 * - Initial Buffer Kapasitesi: 81,920 byte (CHUNK_SIZE * 2)
 * - Kullanım Alanı: WebSocket ses streaming
 * 
 * İş Akışı:
 * 1. Constructor ile meeting ID ve session time belirlenir
 * 2. appendData() ile gelen ses verileri eklenir
 * 3. getBufferSize() ile threshold kontrolü yapılır
 * 4. Threshold aşıldığında getAndClearBuffer() ile veri alınır ve buffer temizlenir
 * 5. incrementChunkCounter() ile chunk sayısı artırılır
 * 
 * @author Ahmet
 * @version 1.0
 */
@Slf4j
@Getter
public class AudioStreamData {
    
    private static final int CHUNK_SIZE = 4096 * 10;
    
    private final String meetingId;
    private final String sessionStartTime;
    private final ByteArrayOutputStream buffer;
    private int chunkCounter;

    /**
     * Constructor - Belirtilen toplantı için buffer oluşturur.
     * 
     * Session başlangıç zamanı yyyyMMdd_HHmmss formatında kaydedilir.
     * Buffer, CHUNK_SIZE'ın 2 katı kapasite ile initialize edilir (81,920 byte).
     *
     * @param meetingId Toplantı ID'si (Discord: channel ID, Zoom: meeting ID)
     */
    public AudioStreamData(String meetingId) {
        this.meetingId = meetingId;
        this.sessionStartTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        this.buffer = new ByteArrayOutputStream(CHUNK_SIZE * 2);
        this.chunkCounter = 0;
    }

    /**
     * Buffer'daki mevcut veri boyutunu döndürür.
     * 
     * Threshold kontrolü için kullanılır. Boyut CHUNK_SIZE'ı aştığında
     * getAndClearBuffer() çağrılmalıdır.
     *
     * @return Byte cinsinden buffer boyutu
     */
    public int getBufferSize() {
        return buffer.size();
    }

    /**
     * Chunk threshold değerini döndürür (static).
     * 
     * Bu değer, buffer'ın ne zaman işleneceğini belirler.
     * Buffer boyutu bu değeri aştığında chunk olarak kaydedilmelidir.
     *
     * @return Chunk boyutu eşiği (40,960 byte)
     */
    public static int getChunkSizeThreshold() {
        return CHUNK_SIZE;
    }

    /**
     * Chunk sayacını 1 artırır.
     * 
     * Her chunk kaydedildiğinde çağrılır. Dosya isimlendirmede
     * kullanılır (örn: audio_chunk_1.mp3, audio_chunk_2.mp3).
     */
    public void incrementChunkCounter() {
        this.chunkCounter++;
    }

    /**
     * Buffer'a yeni ses verisi ekler.
     * 
     * ByteArrayOutputStream normalde IOException fırlatmaz (memory-based),
     * ancak defensive programming için try-catch kullanılmıştır.
     *
     * @param data Eklenecek byte array (WebSocket'ten gelen ses chunk'ı)
     */
    public void appendData(byte[] data) {
        try {
            buffer.write(data);
        } catch (IOException e) {
            log.error("Failed to write data to buffer: {}", e.getMessage(), e);
        }
    }

    /**
     * Buffer'daki tüm veriyi döndürür ve buffer'ı temizler.
     * 
     * toByteArray() ile buffer kopyalanır (yeni array oluşturulur),
     * ardından reset() ile buffer sıfırlanır. Bu sayede bir sonraki
     * chunk için hazır hale gelir.
     * 
     * Not: Bu metod buffer'ı temizler, dolayısıyla veri kaybı olmaması için
     * dönen array mutlaka işlenmelidir.
     * 
     * @return Buffer'daki tüm byte array (ses chunk'ı)
     */
    public byte[] getAndClearBuffer() {
        byte[] data = buffer.toByteArray();
        buffer.reset();
        return data;
    }
}


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
 * WebSocket üzerinden gelen ses verilerini bellekte buffer'lar.
 * Belirli bir boyuta ulaştığında chunk'lar halinde işlenmek üzere
 * verileri döndürür.
 * 
 * Özellikler:
 * - Dinamik buffer yönetimi (ByteArrayOutputStream)
 * - Chunk sayacı (ses segmentlerini takip)
 * - Oturum zaman damgası
 * 
 * Chunk Boyutu: 40,960 byte (4096 * 10)
 * Kullanım: WebSocket ses streaming
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
     * Constructor - Belirtilen meeting için buffer oluşturur.
     *
     * @param meetingId Toplantı ID'si
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
     * @return Byte cinsinden buffer boyutu
     */
    public int getBufferSize() {
        return buffer.size();
    }

    /**
     * Chunk threshold değerini döndürür (static).
     *
     * @return Chunk boyutu threshold (40960 byte)
     */
    public static int getChunkSizeThreshold() {
        return CHUNK_SIZE;
    }

    /**
     * Chunk sayacını 1 artırır.
     */
    public void incrementChunkCounter() {
        this.chunkCounter++;
    }

    /**
     * Buffer'a yeni ses verisi ekler.
     *
     * @param data Eklenecek byte array
     */
    public void appendData(byte[] data) {
        try {
            buffer.write(data);
        } catch (IOException e) {
            log.error("Buffer'a veri yazma hatası: {}", e.getMessage(), e);
        }
    }

    /**
     * Buffer'daki tüm veriyi döndürür ve buffer'ı temizler.
     * 
     * @return Buffer'daki tüm byte array
     */
    public byte[] getAndClearBuffer() {
        byte[] data = buffer.toByteArray();
        buffer.reset();
        return data;
    }
}


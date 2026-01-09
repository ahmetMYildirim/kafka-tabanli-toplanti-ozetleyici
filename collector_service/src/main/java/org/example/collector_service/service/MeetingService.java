package org.example.collector_service.service;

import org.example.collector_service.domain.model.Meeting;
import org.example.collector_service.exception.MediaAssetNotFoundException;
import org.example.collector_service.outbox.OutboxEventPublisher;
import org.example.collector_service.repository.MeetingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * MeetingService - Toplantı yönetim servisi
 * 
 * Discord ve Zoom toplantılarının yaşam döngüsünü yönetir.
 * Her toplantı işlemi için Outbox pattern kullanarak güvenilir event oluşturur.
 * 
 * Sorumluluklar:
 * - Toplantı başlatma ve sonlandırma işlemleri
 * - Veritabanına persistance (kalıcı hale getirme)
 * - Outbox event yayınlama (OutboxEventPublisher aracılığıyla Kafka'ya)
 * 
 * Mimari Pattern: Transactional Service + Outbox Pattern
 * Bu pattern sayesinde veritabanı işlemi ve Kafka event'i atomik olarak gerçekleşir.
 * 
 * Kullanım Senaryosu:
 * 1. Discord/Zoom'da toplantı başladığında startMeeting() çağrılır
 * 2. Meeting entity oluşturulur ve startTime kaydedilir
 * 3. MeetingStarted event'i outbox tablosuna yazılır
 * 4. OutboxEventRelayer bu event'i Kafka'ya gönderir
 * 
 * @author Ahmet
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MeetingService {
    
    private final MeetingRepository meetingRepository;
    private final OutboxEventPublisher outboxEventPublisher;

    /**
     * Yeni bir toplantı başlatır ve MeetingStarted event'i oluşturur.
     * 
     * Bu metod transactional olarak çalışır; toplantı kaydı ve event oluşturma
     * işlemleri aynı transaction içinde gerçekleşir. Herhangi bir hata durumunda
     * her iki işlem de geri alınır (rollback).
     * 
     * Başlatma zamanı (startTime) otomatik olarak şimdiki zaman olarak set edilir.
     * Platform bilgisi (DISCORD veya ZOOM) meeting entity içinde olmalıdır.
     *
     * @param meeting Başlatılacak toplantı entity'si (platform, channelId, channelName gibi bilgilerle dolu)
     * @throws org.springframework.dao.DataAccessException Veritabanı hatası durumunda
     */
    @Transactional
    public void startMeeting(Meeting meeting) {
        meeting.setStartTime(LocalDateTime.now());
        Meeting savedMeeting = meetingRepository.save(meeting);
        
        outboxEventPublisher.publishStarted(
                savedMeeting,
                savedMeeting.getId().toString(),
                "Meeting"
        );
        
        log.info("Meeting started: id={}, platform={}", savedMeeting.getId(), savedMeeting.getPlatform());
    }

    /**
     * Mevcut bir toplantıyı sonlandırır ve MeetingEnded event'i oluşturur.
     * 
     * Toplantı bitiş zamanı (endTime) otomatik olarak şimdiki zaman olarak set edilir.
     * Daha önce başlatılmış bir toplantı için çağrılmalıdır; aksi halde 
     * MediaAssetNotFoundException fırlatılır.
     * 
     * Bitiş event'i Kafka üzerinden downstream servislere (AI, Gateway) iletilir.
     * Bu sayede toplantı analizi ve özet oluşturma süreçleri tetiklenir.
     *
     * @param meetingId Sonlandırılacak toplantının benzersiz ID'si
     * @throws MediaAssetNotFoundException Belirtilen ID ile toplantı bulunamazsa
     * @throws org.springframework.dao.DataAccessException Veritabanı hatası durumunda
     */
    @Transactional
    public void endMeeting(Long meetingId) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new MediaAssetNotFoundException("Meeting not found with id: " + meetingId));

        meeting.setEndTime(LocalDateTime.now());
        Meeting updatedMeeting = meetingRepository.save(meeting);

        outboxEventPublisher.publishEnded(
                updatedMeeting,
                updatedMeeting.getId().toString(),
                "Meeting"
        );

        log.info("Meeting ended: id={}", meetingId);
    }
}

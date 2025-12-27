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
 * Her toplantı işlemi için Outbox pattern kullanarak event oluşturur.
 * 
 * Sorumluluklar:
 * - Toplantı başlatma/sonlandırma
 * - Veritabanı persistance
 * - Outbox event yayınlama (OutboxEventPublisher aracılığıyla)
 * 
 * Pattern: Transactional Service + Outbox Pattern
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
     * Transaction içinde hem meeting hem de outbox event kaydedilir.
     *
     * @param meeting Başlatılacak toplantı entity
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
        
        log.info("Toplantı başlatıldı: id={}, platform={}", savedMeeting.getId(), savedMeeting.getPlatform());
    }

    /**
     * Mevcut bir toplantıyı sonlandırır.
     * Bitiş zamanını set eder ve MeetingEnded event'i oluşturur.
     *
     * @param meetingId Sonlandırılacak toplantının ID'si
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

        log.info("Toplantı sonlandırıldı: id={}", meetingId);
    }
}

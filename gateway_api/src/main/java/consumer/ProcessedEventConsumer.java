package consumer;

import cache.dataStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import model.event.ProcessedSummary;
import model.event.ProcessedTranscription;
import model.event.ProcessedActionItem;
import org.springframework.stereotype.Component;
import service.NotificationService;
import org.springframework.kafka.annotation.KafkaListener;

/**
 * ProcessedEventConsumer - AI servisinden gelen verileri dinleyen Kafka consumer
 * 
 * Bu sınıf, AI servisi tarafından işlenen toplantı verilerini Kafka'dan alır
 * ve aşağıdaki işlemleri gerçekleştirir:
 * 
 * 1. Veriyi bellek deposuna (dataStore) kaydeder
 * 2. WebSocket üzerinden bağlı istemcilere bildirim gönderir
 * 
 * Dinlenen Topic'ler:
 * - processed-summaries: Toplantı özetleri
 * - processed-transcripts: Ses-metin dönüşümleri
 * - processed-action-items: Toplantıdan çıkarılan görevler
 * 
 * Mimari: Event-Driven Architecture - Consumer rolü
 * 
 * @author Ahmet
 * @version 1.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProcessedEventConsumer {
    
    private final dataStore dataStore;
    private final NotificationService notificationService;

    /**
     * AI servisinden gelen toplantı özetlerini işler.
     * Özet, toplantının ana konularını, kararları ve katılımcıları içerir.
     * 
     * İşlem Akışı:
     * 1. Kafka'dan mesaj alınır
     * 2. Bellek deposuna kaydedilir
     * 3. WebSocket istemcilerine bildirim gönderilir
     * 
     * @param summary AI tarafından oluşturulan toplantı özeti
     */
    @KafkaListener(
            topics = "${kafka.topics.processed-summaries}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeSummary(ProcessedSummary summary) {
        String meetingId = summary.getMeetingId();
        log.info("Yeni toplanti ozeti alindi. Toplanti ID: {}, Platform: {}", 
                meetingId, summary.getPlatform());
        
        dataStore.saveSummary(summary);
        notificationService.notifyNewSummary(summary);
        
        log.debug("Toplanti ozeti basariyla islendi. ID: {}", meetingId);
    }

    /**
     * AI servisinden gelen ses-metin dönüşüm sonuçlarını işler.
     * Transkript, toplantıdaki tüm konuşmaların metin haline getirilmiş şeklidir.
     * 
     * @param transcription AI tarafından oluşturulan transkript
     */
    @KafkaListener(
            topics = "${kafka.topics.processed-transcripts}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeTranscription(ProcessedTranscription transcription) {
        String meetingId = transcription.getMeetingId();
        log.info("Yeni transkript alindi. Toplanti ID: {}", meetingId);
        
        dataStore.saveTranscription(transcription);
        notificationService.notifyNewTranscript(transcription);
        
        log.debug("Transkript basariyla islendi. ID: {}", meetingId);
    }

    /**
     * AI servisinden gelen görev listelerini işler.
     * Görevler, toplantı içeriğinden otomatik olarak çıkarılan yapılacak işlerdir.
     * 
     * @param actionItem AI tarafından belirlenen görev listesi
     */
    @KafkaListener(
            topics = "${kafka.topics.processed-action-items}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeActionItems(ProcessedActionItem actionItem) {
        String meetingId = actionItem.getMeetingId();
        log.info("Yeni gorev listesi alindi. Toplanti ID: {}", meetingId);
        
        dataStore.saveActionItems(actionItem);
        notificationService.notifyNewActionItems(actionItem);
        
        log.debug("Gorev listesi basariyla islendi. ID: {}", meetingId);
    }
}

package consumer;

import cache.dataStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import model.event.ProcessedSummary;
import model.event.ProcessedTranscription;
import model.event.ProcessedActionItem;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import service.NotificationService;

import java.util.List;
import java.util.Map;

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
    private final ObjectMapper objectMapper;

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
    public void consumeSummary(ConsumerRecord<String, Object> record) {
        try {
            Object payload = record.value();
            ProcessedSummary summary;
            
            if (payload instanceof Map) {
                summary = objectMapper.convertValue(payload, ProcessedSummary.class);
            } else if (payload instanceof ProcessedSummary) {
                summary = (ProcessedSummary) payload;
            } else {
                log.error("Unexpected payload type: {}", payload.getClass());
                return;
            }
            
            String meetingId = summary.getMeetingId();
            log.info("New meeting summary received. Meeting ID: {}, Platform: {}", 
                    meetingId, summary.getPlatform());
            
            dataStore.saveSummary(summary);
            notificationService.notifyNewSummary(summary);
            
            log.debug("Meeting summary processed successfully. ID: {}", meetingId);
        } catch (Exception e) {
            log.error("Error processing summary: {}", record.value(), e);
        }
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
        log.info("New transcription received. Meeting ID: {}", meetingId);
        
        dataStore.saveTranscription(transcription);
        notificationService.notifyNewTranscript(transcription);
        
        log.debug("Transcription processed successfully. ID: {}", meetingId);
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
    public void consumeActionItems(ConsumerRecord<String, Object> record) {
        try {
            Object payload = record.value();
            ProcessedActionItem actionItem;
            
            if (payload instanceof Map) {
                Map<String, Object> map = (Map<String, Object>) payload;
                
                if (map.containsKey("taskItems")) {
                    Object taskItemsObj = map.get("taskItems");
                    List<String> actionItemsList = new java.util.ArrayList<>();
                    
                    if (taskItemsObj instanceof List) {
                        List<?> taskItems = (List<?>) taskItemsObj;
                        for (Object item : taskItems) {
                            if (item instanceof Map) {
                                Map<String, Object> taskMap = (Map<String, Object>) item;
                                String title = taskMap.get("title") != null ? taskMap.get("title").toString() : "";
                                String description = taskMap.get("description") != null ? taskMap.get("description").toString() : "";
                                if (!title.isEmpty() || !description.isEmpty()) {
                                    actionItemsList.add(title.isEmpty() ? description : title + ": " + description);
                                }
                            }
                        }
                    }
                    
                    map.put("actionItems", actionItemsList);
                }
                
                actionItem = objectMapper.convertValue(map, ProcessedActionItem.class);
            } else if (payload instanceof ProcessedActionItem) {
                actionItem = (ProcessedActionItem) payload;
            } else {
                log.error("Unexpected payload type: {}", payload.getClass());
                return;
            }
            
            String meetingId = actionItem.getMeetingId();
            log.info("New action items list received. Meeting ID: {}", meetingId);
            
            dataStore.saveActionItems(actionItem);
            notificationService.notifyNewActionItems(actionItem);
            
            log.debug("Action items list processed successfully. ID: {}", meetingId);
        } catch (Exception e) {
            log.error("Error processing action items: {}", record.value(), e);
        }
    }
}

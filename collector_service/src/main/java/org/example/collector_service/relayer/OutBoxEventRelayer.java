package org.example.collector_service.relayer;

import jakarta.transaction.Transactional;
import org.example.collector_service.domain.model.OutBoxEvent;
import org.example.collector_service.repository.OutBoxEventRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * OutBoxEventRelayer - Transactional Outbox Pattern relay job
 * 
 * Veritabanındaki işlenmemiş outbox event'lerini scheduled olarak
 * Kafka'ya gönderir. At-least-once delivery garantisi sağlar.
 * 
 * Çalışma Sıklığı: 10 saniyede bir
 * Batch Boyutu: 100 event
 * 
 * Pattern: Polling Publisher
 * 
 * @author Ahmet
 * @version 1.0
 */
@Component
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true", matchIfMissing = false)
public class OutBoxEventRelayer {
    private final OutBoxEventRepository outBoxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    /**
     * OutBoxEventRelayer constructor.
     *
     * @param outBoxEventRepository Outbox event repository
     * @param kafkaTemplate         Kafka template instance
     */
    public OutBoxEventRelayer(OutBoxEventRepository outBoxEventRepository, KafkaTemplate<String, String> kafkaTemplate) {
        this.outBoxEventRepository = outBoxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * İşlenmemiş outbox event'lerini Kafka'ya gönderir.
     * 10 saniyede bir çalışır ve maksimum 100 event işler.
     * Başarılı gönderimden sonra event'i işlenmiş olarak işaretler.
     */
    @Scheduled(fixedDelay = 10000)
    @Transactional
    public void relayEvents(){
        List<OutBoxEvent> outBoxEvents = outBoxEventRepository.find100Unprocessed(PageRequest.of(0,100));
        for(OutBoxEvent event : outBoxEvents){
            try{
                kafkaTemplate.send("team-messages", event.getPayload());
                event.setProcessed(true);
                outBoxEventRepository.save(event);
            }catch (Exception e){
                throw new RuntimeException("Could not relay event", e);
            }
        }
    }
}

package org.example.ai_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * AiServiceApplication - OpenAI tabanlı AI işleme servisi
 * 
 * Bu servis, OpenAI Whisper ve GPT-4 kullanarak toplantı kayıtlarını
 * metne dönüştürür, özetler ve görevleri çıkarır.
 * 
 * Kullanılan AI Modelleri:
 * - OpenAI Whisper (speech-to-text)
 * - GPT-4 Mini (summarization ve task extraction)
 * 
 * @author Ahmet
 * @version 1.0
 */
@SpringBootApplication
@EnableKafka
@EnableAsync
public class AiServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiServiceApplication.class, args);
    }
}

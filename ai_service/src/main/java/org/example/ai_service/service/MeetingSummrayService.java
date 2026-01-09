package org.example.ai_service.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ai_service.client.OpenAIClient;
import org.example.ai_service.domain.model.MeetingSummary;
import org.example.ai_service.domain.model.TranscriptionResult;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.ArrayList;

/**
 * MeetingSummaryService - Toplantı özeti oluşturma servisi
 * 
 * Transkript metninden AI kullanarak toplantı özeti oluşturur.
 * GPT-4 Mini ile structured output formatında özet üretilir.
 * 
 * Özellikler:
 * - GPT-4 Mini ile özetleme (cost-effective)
 * - Structured JSON output (title, summary, keyPoints, decisions)
 * - Katılımcı listesi çıkarma
 * - Fallback mekanizması (AI hatası durumunda)
 * 
 * Özet İçeriği:
 * - title: Toplantı başlığı
 * - summary: Genel özet (2-3 paragraf)
 * - keyPoints: Ana konular (bullet points)
 * - decisions: Alınan kararlar
 * - participants: Katılımcı listesi
 * 
 * @author Ahmet
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MeetingSummrayService {
    private final OpenAIClient openAIClient;
    private final ObjectMapper objectMapper;

    /**
     * Transkript metninden toplantı özeti oluşturur.
     * 
     * GPT-4 Mini modeli ile prompt engineering yapılarak structured
     * output elde edilir. Hata durumunda fallback summary döndürülür.
     * 
     * Prompt Template:
     * - "Analyze the following meeting transcript..."
     * - "Provide a JSON with title, summary, keyPoints, decisions..."
     * 
     * @param transcription Transkripsiyon sonucu (full text ve segments)
     * @return MeetingSummary (AI özeti) veya fallback summary (hata durumunda)
     */
    public MeetingSummary generateSummary(TranscriptionResult transcription){
        log.info("Summary generation starting: meetingId={}", transcription.getMeetingId());

        try{
            String response = openAIClient.generateSummary(transcription.getFullTranscription());

            // JSON response'u parse ederek structured MeetingSummary oluştur
            MeetingSummary summary = parseSummaryFromResponse(response, transcription);

            log.info("Summary generated successfully (structured): meetingId={}", transcription.getMeetingId());
            return summary;
        }catch (Exception e){
            log.error("Summary generation failed: meetingId={}, error={}", 
                    transcription.getMeetingId(), e.getMessage(), e);
            return createFallbackSummary(transcription);
        }
    }

    /**
     * AI hatası durumunda basit bir fallback özet oluşturur.
     * 
     * Fallback özeti minimal bilgiler içerir:
     * - Katılımcı listesi (transkriptten çıkarılır)
     * - Generic başlık ve açıklama
     * - Boş keyPoints ve decisions listeleri
     * 
     * Bu sayede downstream servisler null response almaz.
     * 
     * @param transcription Transkripsiyon sonucu
     * @return Minimal MeetingSummary (fallback)
     */
    private MeetingSummary createFallbackSummary(TranscriptionResult transcription) {
        List<String> participants = transcription.getSegments().stream()
                .map(TranscriptionResult.TranscriptionSegment::getSpeakerName)
                .distinct()
                .toList();

        return MeetingSummary.builder()
                .meetingId(transcription.getMeetingId())
                .channelId(transcription.getChannelId())
                .platform(transcription.getPlatform())
                .title("Meeting - " + transcription.getMeetingId())
                .summary("Meeting summary could not be generated due to AI processing error.")
                .keyPoints(List.of())
                .decisions(List.of())
                .participants(participants)
                .processedTime(Instant.now())
                .build();
    }

    /**
     * GPT response'unu parse ederek MeetingSummary oluşturur.
     * 
     * GPT bazen JSON'dan önce/sonra açıklama metni ekleyebilir.
     * extractJsonFromResponse() ile sadece JSON kısmı çıkarılır.
     * 
     * Parse hatası durumunda fallback summary döndürülür.
     * 
     * @param response GPT-4'ten gelen raw response
     * @param transcription Transcription metadata'sı için
     * @return Parse edilmiş MeetingSummary
     */
    private MeetingSummary parseSummaryFromResponse(String response, TranscriptionResult transcription){
        if (response == null || response.trim().isEmpty()) {
            log.warn("Empty response from AI, using fallback summary");
            return createFallbackSummary(transcription);
        }
        
        try{
            String jsonContent = extractJsonFromResponse(response);
            if (jsonContent == null || jsonContent.trim().isEmpty()) {
                log.warn("Could not extract JSON from response, using fallback summary");
                return createFallbackSummary(transcription);
            }
            
            JsonNode rootNode = objectMapper.readTree(jsonContent);

            return MeetingSummary.builder()
                    .meetingId(transcription.getMeetingId())
                    .channelId(transcription.getChannelId())
                    .platform(transcription.getPlatform())
                    .title(getTextValue(rootNode,"title"))
                    .summary(getTextValue(rootNode, "summary"))
                    .keyPoints(getStringList(rootNode, "keyPoints"))
                    .decisions(getStringList(rootNode, "decisions"))
                    .participants(getStringList(rootNode, "participants"))
                    .durationMinutes(transcription.getDurationSeconds() != null ?
                            transcription.getDurationSeconds() / 60 : 0L)
                    .processedTime(Instant.now())
                    .build();
        }catch (Exception e){
            log.error("Summary parse error: response={}", response, e);
            return createFallbackSummary(transcription);
        }
    }

    /**
     * Response string'inden JSON kısmını çıkarır.
     * 
     * GPT bazen şöyle response verebilir:
     * "Here is the summary:\n{\"title\":...}\nHope this helps!"
     * 
     * Bu metod ilk '{' ve son '}' arasındaki kısmı alır.
     * 
     * @param response GPT raw response
     * @return Sadece JSON kısmı veya original response
     */
    private String extractJsonFromResponse(String response) {
        int start = response.indexOf("{");
        int end = response.lastIndexOf("}");
        if (start >= 0 && end > start) {
            return response.substring(start, end + 1);
        }
        return response;
    }

    /**
     * JSON node'undan string değer okur.
     * 
     * Field yoksa veya null ise empty string döner.
     * 
     * @param node JSON root node
     * @param field Field adı
     * @return String değer veya empty string
     */
    private String getTextValue(JsonNode node, String field) {
        JsonNode fieldNode = node.get(field);
        return fieldNode != null ? fieldNode.asText() : "";
    }

    /**
     * JSON node'undan string array okur.
     * 
     * Field yoksa, null ise veya array değilse empty list döner.
     * 
     * @param node JSON root node
     * @param field Field adı
     * @return String list veya empty list
     */
    private List<String> getStringList(JsonNode node, String field) {
        List<String> result = new ArrayList<>();
        JsonNode arrayNode = node.get(field);
        if (arrayNode != null && arrayNode.isArray()) {
            for (JsonNode item : arrayNode) {
                result.add(item.asText());
            }
        }
        return result;
    }
}

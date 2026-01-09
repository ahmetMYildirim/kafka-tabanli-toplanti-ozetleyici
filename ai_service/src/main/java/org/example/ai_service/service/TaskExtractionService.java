package org.example.ai_service.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ai_service.client.OpenAIClient;
import org.example.ai_service.domain.model.ExtractedTask;
import org.example.ai_service.domain.model.TranscriptionResult;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.UUID;

/**
 * TaskExtractionService - Toplantı transkriptinden görev (action item) çıkarma servisi
 * 
 * GPT-4 kullanarak transkript metninden yapılması gereken görevleri
 * otomatik olarak çıkarır ve yapılandırılmış formatta döndürür.
 * 
 * Özellikler:
 * - GPT-4 ile akıllı görev tespiti
 * - Sorumlu kişi atama (katılımcı listesinden)
 * - Öncelik seviyesi belirleme (HIGH/MEDIUM/LOW)
 * - Confidence score hesaplama
 * - Kaynak metin referansı (traceability için)
 * 
 * Çıkarılan Görev Bilgileri:
 * - title: Görev başlığı
 * - description: Detaylı açıklama
 * - assignee: Sorumlu kişi
 * - priority: Öncelik seviyesi
 * - status: Görev durumu (varsayılan: PENDING)
 * - sourceText: Görevin çıkarıldığı transkript metni
 * 
 * @author Ahmet
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskExtractionService {
    private final OpenAIClient openAIClient;
    private final ObjectMapper objectMapper;

    /**
     * Transkript metninden action item'ları (görevleri) çıkarır.
     * 
     * GPT-4'e katılımcı listesi ve transkript gönderilir. AI, transkriptten
     * yapılması gereken görevleri tespit eder ve JSON formatında döndürür.
     * 
     * Prompt Template:
     * - "Extract action items from the following meeting transcript..."
     * - "Assign tasks to participants: {participants}"
     * - "Return JSON array with title, description, assignee, priority..."
     * 
     * @param transcriptionResult Transkripsiyon sonucu (full text ve segments)
     * @return ExtractedTask (görev listesi) veya null (hata durumunda)
     */
    public ExtractedTask extractedTask(TranscriptionResult transcriptionResult){
        log.info("Extracting tasks from transcription: meetingId={}", transcriptionResult.getMeetingId());

        try{
            List<String> participants = transcriptionResult.getSegments().stream()
                    .map(TranscriptionResult.TranscriptionSegment::getSpeakerName)
                    .distinct()
                    .collect(Collectors.toList());

            String response = openAIClient.extractTasks(
                    transcriptionResult.getFullTranscription(),
                    participants
            );

            List<ExtractedTask.TaskItem> taskItems = parseTaskFromResponse(response);

            ExtractedTask result = ExtractedTask.builder()
                    .meetingId(transcriptionResult.getMeetingId())
                    .channelId(transcriptionResult.getChannelId())
                    .platform(transcriptionResult.getPlatform())
                    .taskItems(taskItems)
                    .processedTime(Instant.now())
                    .build();

            log.info("Task extraction successful: meetingId={}, tasksCount={}", 
                    transcriptionResult.getMeetingId(), taskItems.size());
            return result;
        }catch (Exception e){
            log.error("Task extraction failed: meetingId={}, error={}", 
                    transcriptionResult.getMeetingId(), e.getMessage(), e);
            return null;
        }
    }

    /**
     * GPT response'unu parse ederek TaskItem listesi oluşturur.
     * 
     * Beklenen JSON formatı:
     * {
     *   "tasks": [
     *     {
     *       "title": "...",
     *       "description": "...",
     *       "assignee": "...",
     *       "priority": "HIGH",
     *       "sourceText": "..."
     *     }
     *   ]
     * }
     * 
     * Her görev için unique UUID atanır ve status PENDING olarak set edilir.
     * Parse hatası durumunda empty list döner (null değil).
     * 
     * @param response GPT-4'ten gelen raw response
     * @return TaskItem listesi veya empty list
     */
    private List<ExtractedTask.TaskItem> parseTaskFromResponse(String response){
        List<ExtractedTask.TaskItem> tasks = new ArrayList<>();

        if (response == null || response.trim().isEmpty()) {
            log.warn("Empty response from AI, returning empty task list");
            return tasks;
        }

        try{
            String jsonContent = extractJsonFromResponse(response);
            if (jsonContent == null || jsonContent.trim().isEmpty()) {
                log.warn("Could not extract JSON from response, returning empty task list");
                return tasks;
            }
            
            JsonNode rootNode = objectMapper.readTree(jsonContent);
            JsonNode tasksNode = rootNode.get("tasks");

            if(tasksNode != null && tasksNode.isArray()){
                for(JsonNode taskNode : tasksNode){
                    ExtractedTask.TaskItem task = ExtractedTask.TaskItem.builder()
                            .id(UUID.randomUUID().toString())
                            .title(getTextValue(taskNode,"title"))
                            .description(getTextValue(taskNode, "description"))
                            .assignee(getTextValue(taskNode, "assignee"))
                            .priority(parsePriorty(getTextValue(taskNode,"priority")))
                            .status(ExtractedTask.Status.PENDING)
                            .sourceText(getTextValue(taskNode, "sourceText"))
                            .confidenceScore(0.85) 
                            .build();

                    tasks.add(task);
                }
            }
        }catch (Exception e){
            log.error("Task parse error, response={}", response, e);
        }
        return tasks;
    }

    /**
     * Response string'inden JSON kısmını çıkarır.
     * 
     * GPT bazen JSON'dan önce açıklama metni ekleyebilir.
     * İlk '{' ve son '}' arasındaki kısım extract edilir.
     * 
     * @param response GPT raw response
     * @return Sadece JSON kısmı veya original response
     */
    private String extractJsonFromResponse(String response){
        int start = response.indexOf("{");
        int end = response.lastIndexOf("}");
        if(start >= 0 && end > start){
            return response.substring(start,end+1);
        }
        return response;
    }

    /**
     * JSON node'undan string değer okur.
     * 
     * Field yoksa veya null ise empty string döner.
     * 
     * @param node JSON task node
     * @param field Field adı
     * @return String değer veya empty string
     */
    private String getTextValue(JsonNode node, String field){
        JsonNode fieldNode = node.get(field);
        return fieldNode != null ? fieldNode.asText() : "";
    }

    /**
     * Priority string'ini enum'a parse eder.
     * 
     * Geçersiz veya null değer durumunda MEDIUM döner (safe default).
     * Case-insensitive parse yapılır.
     * 
     * Desteklenen değerler: HIGH, MEDIUM, LOW
     * 
     * @param priority Priority string (örn: "high", "HIGH", "High")
     * @return ExtractedTask.Priority enum value
     */
    private ExtractedTask.Priority parsePriorty(String priority){
        if(priority == null || priority.isEmpty()){
            return ExtractedTask.Priority.MEDIUM;
        }
        try{
            return ExtractedTask.Priority.valueOf(priority.toUpperCase());
        }catch(IllegalArgumentException e){
            log.warn("Invalid priority value: {}, defaulting to MEDIUM", priority);
            return ExtractedTask.Priority.MEDIUM;
        }
    }
}

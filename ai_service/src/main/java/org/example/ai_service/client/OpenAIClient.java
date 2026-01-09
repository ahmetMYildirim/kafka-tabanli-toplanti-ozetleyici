package org.example.ai_service.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * OpenAI API Client (Whisper + ChatGPT)
 * 
 * Pricing:
 * - Whisper: $0.006/minute (~$0.36/hour)
 * - GPT-4o-mini: $0.150/1M input tokens, $0.600/1M output tokens
 * 
 * Limits:
 * - Whisper: 25 MB max file size
 * - Supported formats: mp3, mp4, mpeg, mpga, m4a, wav, webm
 */
@Slf4j
@Component
public class OpenAIClient {

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.api.url:https://api.openai.com/v1}")
    private String baseUrl;

    @Value("${openai.whisper.model:whisper-1}")
    private String whisperModel;

    @Value("${openai.chat.model:gpt-4o-mini}")
    private String chatModel;

    @Value("${openai.language:en}")
    private String language;

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OpenAIClient() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(600, TimeUnit.SECONDS)
                .writeTimeout(600, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Whisper API ile ses dosyasını transkript et
     * 
     * @param audioFilePath Ses dosyası yolu
     * @return Transkripsiyon metni
     * @throws IOException API hatası
     */
    public String transcribeAudio(String audioFilePath) throws IOException {
        log.info("Starting Whisper transcription for file: {}", audioFilePath);
        
        File audioFile = new File(audioFilePath);
        if (!audioFile.exists()) {
            throw new IllegalArgumentException("Audio file not found: " + audioFilePath);
        }

        long fileSize = audioFile.length();
        log.info("File size: {} MB", fileSize / (1024 * 1024));

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", audioFile.getName(),
                        RequestBody.create(audioFile, MediaType.parse("audio/*")))
                .addFormDataPart("model", whisperModel)
                .addFormDataPart("language", language)
                .addFormDataPart("response_format", "verbose_json")
                .build();

        Request request = new Request.Builder()
                .url(baseUrl + "/audio/transcriptions")
                .addHeader("Authorization", "Bearer " + apiKey)
                .post(requestBody)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "No error body";
                log.error("Whisper API error: {} - {}", response.code(), errorBody);
                throw new IOException("Whisper API failed: " + response.code() + " - " + errorBody);
            }

            String responseBody = response.body().string();
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            String text = jsonNode.get("text").asText();
            
            log.info("Whisper transcription completed. Length: {} chars", text.length());
            return text;
            
        } catch (IOException e) {
            log.error("Error calling Whisper API", e);
            throw e;
        }
    }

    /**
     * ChatGPT ile toplantı özeti oluştur (JSON formatında).
     *
     * Dönen response, MeetingSummrayService içinde JSON olarak parse edilir.
     * Placeholder (\"[Insert Date]\" gibi) kullanmaması özellikle istenir.
     */
    public String generateSummary(String transcription) throws IOException {
        if (transcription == null || transcription.trim().isEmpty()) {
            throw new IllegalArgumentException("Transcription cannot be empty");
        }

        log.info("Generating structured summary with ChatGPT (length: {} chars)", transcription.length());

        String systemPrompt =
                "You are an expert meeting assistant. You MUST respond ONLY with a single JSON object. " +
                "Do not include markdown, explanations or placeholders like [Insert Date].";

        String userPrompt =
                "Analyze the following meeting transcription and return a JSON object with this exact schema:\n" +
                "{\n" +
                "  \"title\": string,\n" +
                "  \"summary\": string,\n" +
                "  \"keyPoints\": string[],\n" +
                "  \"decisions\": string[],\n" +
                "  \"participants\": string[]\n" +
                "}\n\n" +
                "Requirements:\n" +
                "- Use natural sentences, no placeholders like \"[Insert Date]\" or \"[Insert Time]\".\n" +
                "- If a field is unknown, omit it or use an empty array, but do not invent square-bracket placeholders.\n" +
                "- The summary should be 1-3 short paragraphs.\n\n" +
                "Transcription:\n" + transcription;

        return callChatGPT(systemPrompt, userPrompt, 1200);
    }

    /**
     * ChatGPT ile görev/aksiyon maddeleri çıkar (JSON formatında).
     *
     * Dönen response, TaskExtractionService içinde JSON olarak parse edilir.
     */
    public String extractTasks(String transcription, List<String> participants) throws IOException {
        if (transcription == null || transcription.trim().isEmpty()) {
            throw new IllegalArgumentException("Transcription cannot be empty");
        }

        log.info("Extracting action items with ChatGPT (structured JSON)");

        StringBuilder participantsList = new StringBuilder();
        if (participants != null && !participants.isEmpty()) {
            participantsList.append(String.join(", ", participants));
        }

        String systemPrompt =
                "You are an expert at extracting action items from meetings. " +
                "You MUST respond ONLY with a single JSON object and no extra text.";

        String userPrompt =
                "From the meeting transcription below, extract all actionable tasks and return JSON with this schema:\n" +
                "{\n" +
                "  \"tasks\": [\n" +
                "    {\n" +
                "      \"title\": string,\n" +
                "      \"description\": string,\n" +
                "      \"assignee\": string,\n" +
                "      \"assigneeId\": string | null,\n" +
                "      \"priority\": \"LOW\" | \"MEDIUM\" | \"HIGH\" | \"URGENT\",\n" +
                "      \"status\": \"PENDING\" | \"IN_PROGRESS\" | \"COMPLETED\" | \"CANCELLED\",\n" +
                "      \"dueDate\": string | null,  // ISO-8601 if available\n" +
                "      \"sourceText\": string,\n" +
                "      \"confidenceScore\": number,\n" +
                "      \"assignmentReason\": string\n" +
                "    }\n" +
                "  ]\n" +
                "}\n\n" +
                "Rules:\n" +
                "- If you are not sure about assignee or dueDate, set them to null or empty string.\n" +
                "- Do NOT return markdown or explanations, only valid JSON.\n" +
                "- Use the participant names if they are clearly responsible.\n" +
                (participantsList.length() > 0
                        ? "\nParticipants in this meeting: " + participantsList + "\n\n"
                        : "\n") +
                "Transcription:\n" + transcription;

        return callChatGPT(systemPrompt, userPrompt, 1200);
    }

    /**
     * ChatGPT API çağrısı yap
     */
    private String callChatGPT(String systemMessage, String userMessage, int maxTokens) throws IOException {
        String jsonBody = String.format(
            "{\"model\":\"%s\"," +
            "\"messages\":[" +
            "{\"role\":\"system\",\"content\":%s}," +
            "{\"role\":\"user\",\"content\":%s}" +
            "]," +
            "\"temperature\":0.7," +
            "\"max_tokens\":%d}",
            chatModel,
            objectMapper.writeValueAsString(systemMessage),
            objectMapper.writeValueAsString(userMessage),
            maxTokens
        );

        RequestBody requestBody = RequestBody.create(
            jsonBody,
            MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url(baseUrl + "/chat/completions")
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "No error body";
                log.error("ChatGPT API error: {} - {}", response.code(), errorBody);
                throw new IOException("ChatGPT API failed: " + response.code());
            }

            String responseBody = response.body().string();
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            String content = jsonNode.get("choices").get(0).get("message").get("content").asText();
            
            log.info("ChatGPT response received. Length: {} chars", content.length());
            return content;
            
        } catch (IOException e) {
            log.error("Error calling ChatGPT API", e);
            throw e;
        }
    }
}

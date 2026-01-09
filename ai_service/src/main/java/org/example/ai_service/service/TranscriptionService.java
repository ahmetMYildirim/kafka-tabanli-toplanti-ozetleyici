package org.example.ai_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ai_service.client.OpenAIClient;
import org.example.ai_service.domain.model.AudioEvent;
import org.example.ai_service.domain.model.TranscriptionResult;
import org.example.ai_service.util.AudioCompressor;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.util.regex.Pattern;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Matcher;

/**
 * TranscriptionService - Ses dosyalarını metne dönüştürme (speech-to-text) servisi
 * 
 * OpenAI Whisper API kullanarak ses kayıtlarını transkribe eder.
 * Konuşmacı ayrıştırma (speaker diarization) ve segment parsing yapar.
 * 
 * Özellikler:
 * - Whisper API ile yüksek doğrulukta transkripsiyon
 * - Konuşmacı tanıma ve ayrıştırma (speaker detection)
 * - Segment bazlı zaman damgası tahmini
 * - Türkçe dil desteği (language: "tr")
 * 
 * Akış:
 * 1. AudioEvent Kafka'dan alınır
 * 2. Audio dosya yolu OpenAI Whisper'a gönderilir
 * 3. Tam transkript metni alınır
 * 4. Konuşmacı pattern'i ile segmentlere ayrılır
 * 5. TranscriptionResult oluşturulur ve Kafka'ya gönderilir
 * 
 * @author Ahmet
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TranscriptionService {

    private final OpenAIClient openAIClient;
    private final ObjectMapper objectMapper;
    private final AudioCompressor audioCompressor;

    private static final Pattern SPEAKER_PATTERN = Pattern.compile("\\[?([^\\]\\:]+)\\]?\\s*:\\s*(.+)");

    /**
     * Ses dosyasını metne dönüştürür (speech-to-text).
     * 
     * OpenAI Whisper API kullanılarak transkripsiyon yapılır.
     * Başarısız durumda null döner, çağıran taraf bu durumu handle etmelidir.
     * 
     * Whisper API Parametreleri:
     * - model: whisper-1
     * - response_format: text
     * 
     * @param audioEvent Kafka'dan gelen audio event (meetingId, channelId, audioUrl içerir)
     * @return TranscriptionResult (full text ve segments) veya null (hata durumunda)
     */
    public TranscriptionResult transcribe(AudioEvent audioEvent){
        
        if (audioEvent == null) {
            log.warn("AudioEvent is null, skipping transcription");
            return null;
        }

        String audioUrl = audioEvent.getAudioUrl();
        if (audioUrl == null || audioUrl.trim().isEmpty()) {
            log.warn("AudioUrl is null or empty, skipping transcription: meetingId={}", audioEvent.getMeetingId());
            return null;
        }

        log.info("Transcription starting: meetingId={}, audioUrl={}", audioEvent.getMeetingId(), audioUrl);

        try{
            java.nio.file.Path path = java.nio.file.Paths.get(audioUrl);

            if (!path.isAbsolute()) {
                String normalized = audioUrl
                        .replace("\\\\", "/")
                        .replaceFirst("^\\./", "")
                        .replaceFirst("^\\.\\\\", "");

                java.nio.file.Path userDir = java.nio.file.Paths.get(System.getProperty("user.dir"));
                java.nio.file.Path projectRoot = userDir.getParent();

                java.util.List<java.nio.file.Path> candidates = new java.util.ArrayList<>();
                if (projectRoot != null) {
                    candidates.add(projectRoot.resolve(normalized).normalize());
                    candidates.add(projectRoot.resolve("collector_service").resolve(normalized).normalize());
                } else {
                    candidates.add(java.nio.file.Paths.get(normalized).toAbsolutePath().normalize());
                }

                for (java.nio.file.Path candidate : candidates) {
                    if (java.nio.file.Files.exists(candidate)) {
                        path = candidate;
                        break;
                    }
                }

                log.debug("Resolved relative audio path. original='{}', resolved='{}'", audioUrl, path);
            }

            File audioFile = path.toFile();
            if (!audioFile.exists()) {
                log.error("Audio file not found after resolution. original='{}', resolved='{}'",
                        audioUrl, audioFile.getAbsolutePath());
                return null;
            }

            File processedFile = audioCompressor.compressIfNeeded(audioFile);
            boolean isCompressed = !processedFile.equals(audioFile);
            
            if (isCompressed) {
                log.info("Audio compressed: {} MB → {} MB",
                        audioCompressor.getFileSizeInMB(audioFile),
                        audioCompressor.getFileSizeInMB(processedFile));
            }

            String filePathToUse = processedFile.getAbsolutePath();
            String transcriptionText = openAIClient.transcribeAudio(filePathToUse);
            
            if (isCompressed) {
                try {
                    Files.deleteIfExists(processedFile.toPath());
                    log.debug("Compressed temporary file deleted: {}", processedFile.getAbsolutePath());
                } catch (IOException e) {
                    log.warn("Failed to delete compressed temporary file: {}", processedFile.getAbsolutePath(), e);
                }
            }
            List<TranscriptionResult.TranscriptionSegment> segments = parseSegments(transcriptionText);

            TranscriptionResult result = TranscriptionResult.builder()
                    .meetingId(audioEvent.getMeetingId())
                    .channelId(audioEvent.getChannelId())
                    .platform(audioEvent.getPlatform())
                    .fullTranscription(transcriptionText)
                    .segments(segments)
                    .language("tr")
                    .confidence(0.95) 
                    .processedTime(String.valueOf(Instant.now()))
                    .build();

            log.info("Transcription completed: meetingId={}, audioUrl={}, segmentCount={}", 
                    audioEvent.getMeetingId(), audioEvent.getAudioUrl(), segments.size());
            return result;
        }catch (Exception e){
            log.error("Transcription failed: meetingId={}, audioUrl={}, error={}", 
                    audioEvent.getMeetingId(), audioEvent.getAudioUrl(), e.getMessage(), e);
            return null;
        }
    }

    /**
     * Transkript metnini segment'lere ayırır ve konuşmacıları tanımlar.

     * @param transcription Whisper'dan gelen tam transkript metni
     * @return Konuşmacı bazlı segment listesi
     */
    private List<TranscriptionResult.TranscriptionSegment> parseSegments(String transcription){
        List<TranscriptionResult.TranscriptionSegment> segments = new ArrayList<>();
        String[] lines = transcription.split("\n");
        long currentTimeMs = 0;

        for(String line : lines){
            line = line.trim();
            if(line.isEmpty()) continue;

            Matcher matcher = SPEAKER_PATTERN.matcher(line);
            if(matcher.matches()){
                String speaker = matcher.group(1).trim();
                String text = matcher.group(2).trim();

                
                long estimatedDuration = text.length() * 50L;

                segments.add(TranscriptionResult.TranscriptionSegment.builder()
                        .speakerName(speaker)
                        .speakerId(generateSpeakerId(speaker))
                        .text(text)
                        .startTimeMs(currentTimeMs)
                        .endTimeMs(currentTimeMs + estimatedDuration)
                        .confidence(0.9)
                        .build());

                
                currentTimeMs += estimatedDuration + 500;
            }
        }
        return  segments;
    }

    /**
     * Konuşmacı adından unique speaker ID oluşturur.
     * @param speakerName Konuşmacının adı (case-insensitive)
     * @return Normalize edilmiş speaker ID
     */
    private String generateSpeakerId(String speakerName) {
        return "speaker_" + speakerName.toLowerCase().replaceAll("\\s+", "_");
    }
}

package org.example.collector_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collector_service.domain.dto.MediaUploadRequest;
import org.example.collector_service.domain.dto.MediaUploadResponse;
import org.example.collector_service.service.MediaIngestService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * MediaUploadController - Medya dosyası yükleme REST API kontrolcüsü
 * 
 * Zoom, Teams ve Discord gibi platformlardan toplantı kayıtlarının
 * (ses/video) yüklenmesini sağlar. Dosya doğrulaması ve deduplication
 * yapılarak güvenli şekilde saklanır.
 * 
 * Desteklenen Dosya Tipleri:
 * - Ses: MP3, WAV, OGG, WebM, M4A
 * - Video: MP4, WebM, MOV, AVI
 * 
 * Maksimum Dosya Boyutu: 500 MB
 * 
 * Endpoint'ler:
 * - POST /api/v1/media/upload - Genel yükleme (metadata ile)
 * - POST /api/v1/media/upload/zoom - Zoom için özelleştirilmiş
 * - POST /api/v1/media/upload/teams - Teams için özelleştirilmiş
 * 
 * @author Ahmet
 * @version 1.0
 */
@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
@Slf4j
public class MediaUploadController {
    private final MediaIngestService mediaIngestService;
    private final ObjectMapper objectMapper;

    /**
     * Genel medya yükleme endpoint'i.
     * 
     * Multipart/form-data ile dosya ve metadata gönderilir.
     * Metadata JSON formatında doğrulanır (@Valid annotation ile).
     * 
     * Duplicate kontrol (checksum ile) ve virus scanning yapılır.
     * 
     * @param file Yüklenecek medya dosyası (audio/video)
     * @param request Toplantı metadata'sı (meetingId, platform, title vs.)
     * @return MediaUploadResponse (fileKey, status, uploadedAt bilgileri)
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MediaUploadResponse> uploadMedia(
            @RequestPart("file") MultipartFile file,
            @RequestPart("metadata") String metadataJson) {
        log.info("Upload request received - File: {}, Size: {} bytes, Metadata: {}", 
                file.getOriginalFilename(), file.getSize(), metadataJson);
        
        try {
            
            MediaUploadRequest request = objectMapper.readValue(metadataJson, MediaUploadRequest.class);
            
            
            if (request.getMeetingId() == null || request.getMeetingId().trim().isEmpty()) {
                log.error("Validation failed: meetingId is required");
                return ResponseEntity.badRequest()
                        .body(MediaUploadResponse.builder()
                                .status("ERROR")
                                .message("Meeting ID is required")
                                .build());
            }
            
            if (request.getPlatform() == null || request.getPlatform().trim().isEmpty()) {
                log.error("Validation failed: platform is required");
                return ResponseEntity.badRequest()
                        .body(MediaUploadResponse.builder()
                                .status("ERROR")
                                .message("Platform is required")
                                .build());
            }
            
            log.info("Metadata parsed - MeetingId: {}, Platform: {}, Title: {}", 
                    request.getMeetingId(), request.getPlatform(), request.getMeetingTitle());
            
            return ResponseEntity.ok(mediaIngestService.uploadMedia(file, request));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            log.error("Failed to parse metadata JSON: {}", metadataJson, e);
            return ResponseEntity.badRequest()
                    .body(MediaUploadResponse.builder()
                            .status("ERROR")
                            .message("Invalid JSON format: " + e.getMessage())
                            .build());
        } catch (Exception e) {
            log.error("Unexpected error processing upload: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(MediaUploadResponse.builder()
                            .status("ERROR")
                            .message("Upload failed: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Zoom toplantı kaydı yükleme endpoint'i.
     * 
     * Zoom'a özel basitleştirilmiş endpoint. Platform otomatik olarak "ZOOM" set edilir.
     * Meeting ID zorunlu, diğer alanlar opsiyoneldir.
     * 
     * Kullanım Senaryosu:
     * - Zoom Cloud Recording'den indirilen dosyalar
     * - Zoom local recording'ler
     * - Zoom webhook ile otomatik yükleme
     * 
     * @param file Zoom toplantı kaydı (MP4, M4A vb.)
     * @param meetingId Zoom meeting ID (numeric ID)
     * @param meetingTitle Toplantı başlığı (opsiyonel)
     * @param hostName Host adı (opsiyonel)
     * @return MediaUploadResponse (fileKey, status, uploadedAt)
     */
    @PostMapping(value = "/upload/zoom", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MediaUploadResponse> uploadZoomMedia(
        @RequestPart("file") MultipartFile file,
        @RequestPart String meetingId,
        @RequestPart(required = false) String meetingTitle,
        @RequestPart(required = false) String hostName
    ){
        MediaUploadRequest request = MediaUploadRequest.builder()
                .meetingId(meetingId)
                .platform("ZOOM")
                .meetingTitle(meetingTitle)
                .hostName(hostName)
                .build();

        return ResponseEntity.ok(mediaIngestService.uploadMedia(file, request));
    }

    /**
     * Microsoft Teams toplantı kaydı yükleme endpoint'i.
     * 
     * Teams'e özel basitleştirilmiş endpoint. Platform otomatik olarak "TEAMS" set edilir.
     * 
     * Kullanım Senaryosu:
     * - Teams Cloud Recording'den indirilen dosyalar
     * - Teams Graph API ile otomatik yükleme
     * 
     * @param file Teams toplantı kaydı (MP4 vb.)
     * @param meetingId Teams meeting ID (UUID formatında)
     * @param meetingTitle Toplantı başlığı (opsiyonel)
     * @return MediaUploadResponse (fileKey, status, uploadedAt)
     */
    @PostMapping(value = "/upload/teams", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MediaUploadResponse> uploadTeamsMedia(
            @RequestPart("file") MultipartFile file,
            @RequestPart String meetingId,
            @RequestPart(required = false) String meetingTitle
    ){
        MediaUploadRequest request = MediaUploadRequest.builder()
                .meetingId(meetingId)
                .platform("TEAMS")
                .meetingTitle(meetingTitle)
                .build();

        return ResponseEntity.ok(mediaIngestService.uploadMedia(file, request));
    }
}

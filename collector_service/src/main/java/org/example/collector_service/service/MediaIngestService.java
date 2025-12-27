package org.example.collector_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collector_service.domain.dto.MediaUploadRequest;
import org.example.collector_service.domain.dto.MediaUploadResponse;
import org.example.collector_service.domain.model.MediaAsset;
import org.example.collector_service.domain.model.MeetingMedia;
import org.example.collector_service.domain.model.OutBoxEvent;
import org.example.collector_service.exception.*;
import org.example.collector_service.repository.MediaAssetRepository;
import org.example.collector_service.repository.MeetingMediaRepository;
import org.example.collector_service.repository.OutBoxEventRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.HexFormat;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaIngestService {
    private final OutBoxEventRepository outBoxEventRepository;
    private final MediaAssetRepository mediaAssetRepository;
    private final MeetingMediaRepository meetingMediaRepository;
    private final ObjectMapper objectMapper;

    @Value("${media.storage.path}")
    private String mediaStoragePath;

    private static final Set<String> ALLOWED_FILE_TYPES = Set.of(
            "audio/mpeg", "audio/wav", "audio/ogg", "audio/webm", "audio/mp4",
            "video/mp4", "video/webm", "video/quicktime", "video/x-msvideo"
    );

    private static final long MAX_FILE_SIZE_BYTES = 500 * 1024 * 1024;

    @Transactional
    public MediaUploadResponse uploadMedia(MultipartFile file, MediaUploadRequest request){
            validateFile(file);

            try{
                String checksum = calculateCheckSum(file.getBytes());

                if(mediaAssetRepository.existsByChecksum(checksum)){
                    log.warn("Duplicate file upload attempt detected. Checksum: {}", checksum);
                    throw new DuplicateFileException("This file has already been uploaded. Checksum: " + checksum);
                }

                String fileKey = generateFileKey(request.getPlatform());
                String savedPath = saveFile(file, fileKey, request.getPlatform());

                MediaAsset mediaAsset = MediaAsset.builder()
                        .fileKey(fileKey)
                        .mimeType(file.getContentType())
                        .originalFileName(file.getOriginalFilename())
                        .fileSize(file.getSize())
                        .checksum(checksum)
                        .storagePath(savedPath)
                        .status(MediaAsset.MediaStatus.PENDING)
                        .build();

                mediaAsset = mediaAssetRepository.save(mediaAsset);

                MeetingMedia meetingMedia = MeetingMedia.builder()
                        .meetingId(request.getMeetingId())
                        .platform(request.getPlatform().toUpperCase())
                        .mediaAsset(mediaAsset)
                        .meetingTitle(request.getMeetingTitle())
                        .hostname(request.getHostName())
                        .meetingStartTime(request.getMeetingStartTime())
                        .meetingEndTime(request.getMeetingEndTime())
                        .participantCount(request.getParticipantCount())
                        .uploadedBy(request.getUploadedBy())
                        .uploadedAt(LocalDateTime.now())
                        .build();

                meetingMedia = meetingMediaRepository.save(meetingMedia);

                saveToOutbox(mediaAsset,meetingMedia,request);
                log.info("Media uploaded successfully. FileKey: {}", fileKey);

                return MediaUploadResponse.builder()
                        .mediaStatusId(mediaAsset.getId())
                        .fileKey(fileKey)
                        .status("SUCCESS")
                        .message("File uploaded successfully.")
                        .uploadedAt(LocalDateTime.now())
                        .build();

            }catch (IOException e){
                log.error("File storage failed", e);
                throw new StorageException("Failed to store file", e);
            } catch (Exception e) {
                log.error("File upload failed", e);
                throw new FileUploadException("File upload failed", e);
            }
    }

    private void validateFile(MultipartFile file){
        if(file.isEmpty()){
            throw new InvalidFileException("File is empty");
        }
        if(file.getSize() > MAX_FILE_SIZE_BYTES){
            throw new FileSizeExceededException("File size exceeds the maximum limit of 500MB");
        }
        String contentType = file.getContentType();
        if(contentType == null || !ALLOWED_FILE_TYPES.contains(contentType)){
            throw new InvalidFileException("Unsupported file type: " + contentType);
        }
    }

    private String calculateCheckSum(byte[] data){
        try{
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            return HexFormat.of().formatHex(hash);
        }catch (NoSuchAlgorithmException e){
            log.error("Error calculating checksum", e);
            throw new StorageException("Error calculating checksum", e);
        }
    }

    private String generateFileKey(String platform){
        return platform.toLowerCase() + "_" + UUID.randomUUID();
    }

    private String saveFile(MultipartFile file, String fileKey, String platform) throws Exception{
        Path platformDir = Paths.get(mediaStoragePath, platform.toLowerCase());
        Files.createDirectories(platformDir);

        String fileExtension = getFileExtension(file.getOriginalFilename());
        Path filePath = platformDir.resolve(fileKey + fileExtension);
        Files.write(filePath, file.getBytes());

        return filePath.toString();
    }

    private String getFileExtension(String filename){
        if(filename == null ||!filename.contains(".")){
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    private void saveToOutbox(MediaAsset asset, MeetingMedia media, MediaUploadRequest request){
        try{
            Map<String, Object> payload = new HashMap<>();
            payload.put("eventId", UUID.randomUUID().toString());
            payload.put("eventType", "MEDİA_UPLOADED");
            payload.put("platform", request.getPlatform().toUpperCase());
            payload.put("meetingId", request.getMeetingId());
            payload.put("fileKey", asset.getFileKey());
            payload.put("storagePath", asset.getStoragePath());
            payload.put("mimeType", asset.getMimeType());
            payload.put("fileSize", asset.getFileSize());
            payload.put("meetingTitle", request.getMeetingTitle());
            payload.put("hostName", request.getHostName());
            payload.put("meetingStartTime", request.getMeetingStartTime());
            payload.put("meetingEndTime", request.getMeetingEndTime());
            payload.put("participantCount", request.getParticipantCount());
            payload.put("uploadedBy", request.getUploadedBy());
            payload.put("timestamp", System.currentTimeMillis());
            payload.put("originalFileName", asset.getOriginalFileName());
            payload.put("checksum", asset.getChecksum());

            String payloadJson = objectMapper.writeValueAsString(payload);

            OutBoxEvent outBoxEvent = OutBoxEvent.builder()
                    .aggregateType("MeetingMedia")
                    .aggregateId(asset.getFileKey())
                    .eventType("MEDIA_UPLOADED")
                    .payload(payloadJson)
                    .build();

            outBoxEventRepository.save(outBoxEvent);
            log.info("Outbox event saved for media upload: {}", outBoxEvent.getAggregateId());

        }catch (Exception e){
            log.error("Failed to save outbox event", e);
            throw new OutboxEventPublisherException("Failed to save outbox event", e);
        }
    }

    @Transactional
    public void updateMediaStatus(String fileKey, MediaAsset.MediaStatus status){
        MediaAsset mediaAsset = mediaAssetRepository.findByFileKey(fileKey)
                .orElseThrow(() -> new MediaAssetNotFoundException("Media asset not found with fileKey: " + fileKey));

        mediaAsset.setStatus(status);
        if(status == MediaAsset.MediaStatus.COMPLETED){
            mediaAsset.setProcessedAt(LocalDateTime.now());
        }
        mediaAssetRepository.save(mediaAsset);
        log.info("Media asset status updated. FileKey: {}, New Status: {}", fileKey, status);
    }
}

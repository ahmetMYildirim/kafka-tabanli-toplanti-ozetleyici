package org.example.collector_service.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
public class MediaUploadController {
    private final MediaIngestService mediaIngestService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MediaUploadResponse> uploadMedia(
            @RequestPart("file")MultipartFile file,
            @RequestPart("metadata") @Valid MediaUploadRequest request) {
        return ResponseEntity.ok(mediaIngestService.uploadMedia(file, request));
    }

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

    @PostMapping(value = "/upload/googlemeet", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MediaUploadResponse> uploadGoogleMeetMedia(
            @RequestPart("file") MultipartFile file,
            @RequestPart String meetingId,
            @RequestPart(required = false) String meetingTitle
    ){
        MediaUploadRequest request = MediaUploadRequest.builder()
                .meetingId(meetingId)
                .platform("GOOGLE_MEET")
                .meetingTitle(meetingTitle)
                .build();

        return ResponseEntity.ok(mediaIngestService.uploadMedia(file, request));
    }
}

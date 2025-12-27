package org.example.collector_service.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaUploadResponse {
    private Long mediaStatusId;
    private String fileKey;
    private String status;
    private String message;
    private LocalDateTime uploadedAt;
}

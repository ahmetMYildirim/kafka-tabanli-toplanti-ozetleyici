package org.example.collector_service.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaUploadRequest {

    @NotBlank(message = "Meeting ID is required")
    private String meetingId;

    @NotBlank(message = "Platform is required")
    private String platform;

    private String meetingTitle;
    private String hostName;
    private LocalDateTime meetingStartTime;
    private LocalDateTime meetingEndTime;
    private Integer participantCount;
    private String uploadedBy;
}

package entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "transcriptions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranscriptionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "audio_message_id")
    private Long audioMessageId;

    @Column(name = "meeting_id")
    private Long meetingId;

    @Column(name = "full_text", columnDefinition = "TEXT")
    private String fullText;

    @Column(name = "language")
    private String language;

    @Column(name = "confidence_score")
    private Double confidenceScore;

    @Column(name = "word_count")
    private Integer wordCount;

    @Column(name = "processing_time_ms")
    private Long processingTimeMs;

    @Column(name = "ai_model")
    private String aiModel;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (language == null) {
            language = "en";
        }
        if (aiModel == null) {
            aiModel = "whisper-1";
        }
    }
}


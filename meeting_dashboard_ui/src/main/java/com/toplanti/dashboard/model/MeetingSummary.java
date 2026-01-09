package com.toplanti.dashboard.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * MeetingSummary - Toplantı özet modeli
 * @author Ömer
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeetingSummary {

    /** Toplantının benzersiz kimliği */
    private String meetingId;

    /** Discord kanal ID veya Zoom meeting ID */
    private String channelId;

    /** Toplantı platformu: DISCORD veya ZOOM */
    private String platform;

    /** Toplantı başlığı veya konusu */
    private String title;

    /** AI tarafından oluşturulan toplantı özeti */
    private String summary;

    /** Toplantının ana konuları ve alınan kararlar listesi */
    private List<String> keyPoints;

    /** Toplantıya katılan kişilerin listesi */
    private List<String> participants;

    /** Toplantının başlangıç zamanı */
    private Instant meetingStartTime;

    /** Toplantının bitiş zamanı */
    private Instant meetingEndTime;

    /** AI işlem tamamlanma zamanı */
    private Instant processedTime;
}


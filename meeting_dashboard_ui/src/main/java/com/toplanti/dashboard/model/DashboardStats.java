package com.toplanti.dashboard.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DashboardStats - Dashboard istatistik modeli
 * @author Ömer
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStats {

    /** Toplam toplantı sayısı */
    private int totalMeetings;

    /** Toplam transkript sayısı */
    private int totalTranscriptions;

    /** Toplam görev sayısı */
    private int totalActionItems;

    /** Discord toplantı sayısı */
    private int discordMeetings;

    /** Zoom toplantı sayısı */
    private int zoomMeetings;
}


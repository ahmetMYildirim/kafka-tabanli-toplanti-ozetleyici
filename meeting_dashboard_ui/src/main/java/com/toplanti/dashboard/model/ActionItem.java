package com.toplanti.dashboard.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * ActionItem - Görev listesi modeli
 * @author Ömer
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActionItem {

    /** Toplantının benzersiz kimliği */
    private String meetingId;

    /** Toplantıdan çıkarılan görevlerin listesi */
    private List<String> actionItems;

    /** AI işlem tamamlanma zamanı */
    private Instant processedTime;

    /**
     * ActionItemDetail - Tek bir görevi temsil eden iç sınıf
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActionItemDetail {

        /** Yapılacak görev açıklaması */
        private String task;

        /** Görevin atandığı kişi */
        private String assign;

        /** Öncelik seviyesi: HIGH, MEDIUM, LOW */
        private String priority;

        /** Teslim tarihi */
        private String dueDate;

        /** Görevin toplantı içindeki bağlamı */
        private String context;
    }
}


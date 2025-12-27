package org.example.collector_service.repository;

import org.example.collector_service.domain.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.*;

/**
 * MessageRepository - Mesaj veri erişim katmanı
 * 
 * Message entity için CRUD işlemleri ve gelişmiş sorgulama metodları.
 * Platform, tarih aralığı, yazar ve içerik bazlı aramalar yapabilir.
 * 
 * @author Ahmet
 * @version 1.0
 */
@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {


    /**
     * Belirli bir platforma ait tüm mesajları getirir.
     *
     * @param platform Platform adı (DISCORD veya ZOOM)
     * @return Bulunan mesajların listesi
     */
    List<Message> findByPlatform(String platform);

    /**
     * Belirli bir tarih aralığındaki mesajları getirir.
     *
     * @param start Başlangıç tarihi
     * @param end   Bitiş tarihi
     * @return Tarih aralığındaki mesajlar
     */
    List<Message> findByTimestampBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Platform ve tarih aralığına göre mesajları filtreler.
     * Belirli bir toplantı dönemindeki mesajları getirmek için kullanılır.
     *
     * @param platform Platform adı (DISCORD veya ZOOM)
     * @param start    Başlangıç tarihi
     * @param end      Bitiş tarihi
     * @return Filtrelenmiş mesaj listesi
     */
    List<Message> findByPlatformAndTimestampBetween(
      String platform,
      LocalDateTime start,
      LocalDateTime end
    );

    /**
     * Belirli bir yazara ait tüm mesajları getirir.
     *
     * @param author Yazar kullanıcı adı
     * @return Yazara ait mesajlar
     */
    List<Message> findByAuthor(String author);

    /**
     * Belirli bir platformdan son 10 mesajı getirir.
     * En son gönderilen mesajlar önce gelecek şekilde sıralanır.
     *
     * @param platform Platform adı (DISCORD veya ZOOM)
     * @return Son 10 mesaj
     */
    List<Message> findTop10ByPlatformOrderByTimestampDesc(String platform);

    /**
     * Mesaj içeriğinde anahtar kelime araması yapar.
     * LIKE operatörü ile kısmi eşleşme sağlar.
     *
     * @param keyword Aranacak anahtar kelime
     * @return Eşleşen mesajların listesi
     */
    @Query("SELECT m FROM Message m WHERE m.content LIKE %:keyword%")
    List<Message> SearchbyContent(@Param("keyword") String keyword);
}

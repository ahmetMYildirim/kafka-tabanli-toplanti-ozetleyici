package org.example.collector_service.bot;

import org.example.collector_service.domain.model.AudioMessage;
import org.example.collector_service.domain.model.UserAudioData;
import org.example.collector_service.service.AudioMessageService;
import net.dv8tion.jda.api.audio.AudioReceiveHandler;
import net.dv8tion.jda.api.audio.UserAudio;

import java.io.File;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * DiscordAudioReceiver - Discord ses akışı alıcısı
 * 
 * Discord ses kanallarından gelen PCM audio verilerini yakalar ve işler.
 * Her kullanıcı için ayrı audio stream'i yönetir ve MP3'e dönüştürür.
 * 
 * Özellikler:
 * - Kullanıcı bazında ses verisi toplama
 * - PCM -> MP3 dönüşümü (FFmpeg ile)
 * - Veritabanına ses mesajı kaydetme
 * 
 * Audio Format: 48kHz, Stereo, 16-bit signed big-endian PCM
 * 
 * @author Ahmet
 * @version 1.0
 */
public class DiscordAudioReceiver implements AudioReceiveHandler {

    private final AudioMessageService messageService;
    
    /** Kullanıcı ID -> Audio verisi eşlemesi */
    private final Map<String, UserAudioData> userAudioMap = new HashMap<>();
    
    /** Aktif ses kanalının ID'si */
    private String channleId;
    
    /** Temizleme işlemi devam ediyor mu? */
    private volatile boolean isCleaningUp = false;

    /**
     * DiscordAudioReceiver constructor.
     *
     * @param messageService Ses mesajı kaydetme servisi
     */
    public DiscordAudioReceiver(AudioMessageService messageService) {
        this.messageService = messageService;
    }

    @Override
    public boolean canReceiveUser() {
        return true;
    }

    @Override
    public void handleUserAudio(UserAudio userAudio) {
        if (isCleaningUp) {
            System.out.println("Cleanup in progress, ignoring audio packet");
            return;
        }

        byte[] audioData = userAudio.getAudioData(1.0);
        String userId = userAudio.getUser().getId();
        String userName = userAudio.getUser().getName();

        try{
            new File("audio_storage").mkdirs();

            userAudioMap.computeIfAbsent(userId, k -> new UserAudioData(userId, userName));

            UserAudioData userAudioData = userAudioMap.get(userId);
            
            if (!userAudioData.isClosed()) {
                userAudioData.writeAudioData(audioData);
            }

        }catch(java.io.IOException e){

            String message = e.getMessage();
            if (message != null && (message.contains("Stream Closed") || message.contains("stream is already closed"))) {
                return;
            }
            throw new RuntimeException("Could not handle user audio", e);
        }catch(Exception e){
            throw new RuntimeException("Could not handle user audio", e);
        }
    }

    /**
     * Ses kaydını sonlandırır ve tüm kullanıcı verilerini MP3'e dönüştürür.
     * Kanal ayrılmadan önce çağrılmalıdır.
     */
    public void cleanUp(){
        System.out.println("Converting starting...");
        
        isCleaningUp = true;
        
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        for(UserAudioData userData : userAudioMap.values()){
            try{
                userData.closeOutputStream();
                convertToMp3(userData);
            }catch(Exception e){
                throw new RuntimeException("Could not convert user audio data", e);
            }
        }
        userAudioMap.clear();
        isCleaningUp = false;

    }

    private void convertToMp3(UserAudioData userData) throws Exception {
        File pcmFile = userData.getTempPcmData();

        if(!pcmFile.exists()||pcmFile.length()==0){
            System.out.printf("PCM file empty or doesn't exist!!");
            return;
        }

        String mp3FilePath = "audio_storage/audio_" + userData.getUserId() + "_" + System.currentTimeMillis() + ".mp3";

        ProcessBuilder processBuilder = new ProcessBuilder(
                "ffmpeg",
                "-f", "s16be",
                "-ar", "48000",
                "-ac", "2",
                "-i", pcmFile.getAbsolutePath(),
                "-y",
                "-b:a", "128k",
                mp3FilePath
        );

        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        int exitVal = process.waitFor();

        if(exitVal != 0){
            System.out.println("Ffmpeg converted is failed with exitcode: " + exitVal + "for user: "  + userData.getUserId());
        }
        else{
            System.out.println("Ffmpeg converted is successful for user: " + userData.getUserId());

            saveAudioMessageDB(userData, mp3FilePath);
            if(pcmFile.delete()){
                System.out.println("PCM file deleted successfully! " + pcmFile.getName());
            }
        }
    }

    private void saveAudioMessageDB(UserAudioData userAudioData, String mp3FilePath) {
        try{
            AudioMessage audioMessage = new AudioMessage();
            audioMessage.setPlatform("Discord");
            audioMessage.setChannelId(this.channleId);
            audioMessage.setAuthor(userAudioData.getUserName());
            audioMessage.setAudioUrl(mp3FilePath);
            audioMessage.setTimestamp(LocalDateTime.now());
            audioMessage.setVoiceSessionId(userAudioData.getUserId());

            messageService.processAndSaveAudioMessage(audioMessage);

            System.out.println("Audio Message saved to database");
        }catch(Exception e){
            System.err.println("Failed to save audio message to database");
            e.printStackTrace();
        }
    }

    /**
     * Aktif ses kanalının ID'sini ayarlar.
     *
     * @param channleId Discord ses kanalı ID'si
     */
    public void setChannleId(String channleId) {
        this.channleId = channleId;
    }
}

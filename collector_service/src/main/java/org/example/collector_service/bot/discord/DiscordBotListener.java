package org.example.collector_service.bot.discord;

import org.example.collector_service.bot.DiscordAudioReceiver;
import org.example.collector_service.domain.model.Message;
import org.example.collector_service.service.AudioMessageService;
import org.example.collector_service.service.MessageService;
import org.example.collector_service.service.VoiceSessionService;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

import java.time.LocalDateTime;

/**
 * DiscordBotListener - Discord bot olay dinleyicisi
 * 
 * Discord sunucusundaki mesajları ve ses kanalı olaylarını dinler.
 * Bot komutlarını işler (!join, !leave) ve mesajları veritabanına kaydeder.
 * 
 * Desteklenen Komutlar:
 * - !join  : Botun ses kanalına katılmasını sağlar
 * - !leave : Botun ses kanalından ayrılmasını sağlar
 * 
 * Gerekli Gateway Intent'leri:
 * - MESSAGE_CONTENT
 * - GUILD_MESSAGES
 * - GUILD_VOICE_STATES
 * 
 * @author Ahmet
 * @version 1.0
 */
@Component
@ConditionalOnProperty(name = "discord.bot.enabled", havingValue = "true", matchIfMissing = false)
public class DiscordBotListener extends ListenerAdapter {

    @Value("${discord.bot.token}")
    private String token;

    private final MessageService messageService;
    private final VoiceSessionService voiceSessionService;
    private final AudioMessageService audioMessageService;
    private final DiscordAudioReceiver discordAudioReceiver;

    /**
     * DiscordBotListener constructor.
     * Gerekli servisleri inject eder ve audio receiver'ı başlatır.
     *
     * @param messageService      Mesaj kaydetme servisi
     * @param voiceSessionService Ses oturumu yönetim servisi
     * @param audioMessageService Ses mesajı kaydetme servisi
     */
    public DiscordBotListener(MessageService messageService, VoiceSessionService voiceSessionService, AudioMessageService audioMessageService) {
        this.messageService = messageService;
        this.voiceSessionService = voiceSessionService;
        this.audioMessageService = audioMessageService;
        this.discordAudioReceiver = new DiscordAudioReceiver(audioMessageService);
    }

    /**
     * Discord bot'unu başlatır.
     * JDA builder ile bağlantı kurar ve gerekli intent'leri aktif eder.
     */
    @PostConstruct
    public void startBot() {
        try{
            JDABuilder.createDefault(token)
                    .enableIntents(
                            GatewayIntent.MESSAGE_CONTENT,
                            GatewayIntent.GUILD_MESSAGES,
                            GatewayIntent.GUILD_VOICE_STATES
                    )
                    .addEventListeners(this)
                    .build();
            System.out.println("Discord Bot started...");
        }catch (Exception e) {
            
            System.err.println("WARNING: Discord Bot failed to start: " + e.getMessage());
            System.err.println("Discord Bot is disabled. File upload via UI will still work.");
            
        }
    }

    public void onMessageReceived(MessageReceivedEvent event) {
        if(event.getAuthor().isBot()) return;

        String author = event.getAuthor().getName();
        String content = event.getMessage().getContentRaw();
        String platform = "Discord";
        String channelId = event.getChannel().getId();
        String channelName = event.getChannel().getName();

        if(content.equalsIgnoreCase("!join")){
            joinVoiceChannel(event);
            return;
        } else if(content.equalsIgnoreCase("!leave")){
            leftVoiceChannel(event);
            return;
        }

        Message message = new Message();
        message.setPlatform(platform);
        message.setAuthor(author);
        message.setContent(content);
        message.setTimestamp(LocalDateTime.now());
        message.setChannelId(channelId);
        message.setChannelName(channelName);

        messageService.processAndSaveMessage(message);

        System.out.printf("Message received in channel %s (%s) from %s: %s%n", channelName, channelId, author, content);
    }

    private void joinVoiceChannel(MessageReceivedEvent event) {
        Member member = event.getMember();
        if(member == null) return;

        var voiceState = member.getVoiceState();
        if(voiceState == null || !voiceState.inAudioChannel()){
            event.getChannel().sendMessage("You need to be in a voice channel for me to join!").queue();
            return;
        }

        var audioChannel = voiceState.getChannel();
        var audioManager = event.getGuild().getAudioManager();

        this.discordAudioReceiver.setChannleId(audioChannel.getId());
        audioManager.openAudioConnection(audioChannel);
        audioManager.setReceivingHandler(this.discordAudioReceiver);

        event.getChannel().sendMessage("I joined the voice channel! Conversations are being recorded!!!" + audioChannel.getId()).queue();
        System.out.println("Joined voice channel: " + audioChannel.getName() + " ID: " + audioChannel.getId());
    }

    private void leftVoiceChannel(MessageReceivedEvent event) {
        var audioManager = event.getGuild().getAudioManager();
        if(audioManager.isConnected()){
            this.discordAudioReceiver.cleanUp();
            audioManager.closeAudioConnection();
            event.getChannel().sendMessage("I left the voice channel!").queue();
            System.out.println("Left voice channel");
        } else {
            event.getChannel().sendMessage("I'm not in a voice channel!").queue();
        }
    }
}

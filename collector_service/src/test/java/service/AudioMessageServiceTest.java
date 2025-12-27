package service;

import org.example.collector_service.domain.model.AudioMessage;
import org.example.collector_service.repository.AudioMessageRepository;
import org.example.collector_service.service.AudioMessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
@DisplayName("AudioMessageService Tests")
public class AudioMessageServiceTest {

    @Mock
    private AudioMessageRepository audioMessageRepository;

    @InjectMocks
    private AudioMessageService audioMessageService;

    private AudioMessage testAudioMessage;

    @BeforeEach
    void setUp(){
        testAudioMessage = new AudioMessage();
        testAudioMessage.setId(1L);
        testAudioMessage.setTimestamp(LocalDateTime.now());
    }

    @Test
    @DisplayName("Get all audio messages - success")
    void getAllAudioMessages_success() {
        when(audioMessageRepository.findAll()).thenReturn(Arrays.asList(testAudioMessage));
    }
}

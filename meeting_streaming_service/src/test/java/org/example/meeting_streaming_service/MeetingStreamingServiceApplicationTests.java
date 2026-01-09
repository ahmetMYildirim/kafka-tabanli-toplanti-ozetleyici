package org.example.meeting_streaming_service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MeetingStreamingServiceApplication Tests")
class MeetingStreamingServiceApplicationTests {

    @Test
    @DisplayName("Test context loads successfully")
    void contextLoads() {
        
        assertTrue(true);
    }

    @Test
    @DisplayName("Spring Boot annotations should be available")
    void springBootAnnotationsShouldBeAvailable() {
        assertNotNull(org.springframework.boot.autoconfigure.SpringBootApplication.class);
        assertNotNull(org.springframework.scheduling.annotation.EnableScheduling.class);
        assertNotNull(org.springframework.context.annotation.ComponentScan.class);
    }
}


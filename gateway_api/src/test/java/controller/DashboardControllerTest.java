package controller;

import cache.dataStore;
import model.dto.ApiResponse;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DashboardController Unit Tests")
class DashboardControllerTest {

    @Mock
    private dataStore dataStore;

    @InjectMocks
    private DashboardController dashboardController;

    @Nested
    @DisplayName("Dashboard Stats Tests")
    class DashboardStatsTests {

        @Test
        @DisplayName("Should return dashboard statistics")
        void getDashboardStats_ShouldReturnStats() {
            // Given
            Map<String, Object> mockStats = new HashMap<>();
            mockStats.put("totalMeetings", 10);
            mockStats.put("totalTranscriptions", 8);
            mockStats.put("totalActionItems", 25);
            mockStats.put("discordMeetings", 6);
            mockStats.put("zoomMeetings", 4);

            when(dataStore.getUserStatistics()).thenReturn(mockStats);

            // When
            ResponseEntity<ApiResponse<Map<String, Object>>> response =
                    dashboardController.getDashboardStats();

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isTrue();
            assertThat(response.getBody().getData()).containsKey("totalMeetings");
            assertThat(response.getBody().getData().get("totalMeetings")).isEqualTo(10);
            assertThat(response.getBody().getData().get("discordMeetings")).isEqualTo(6);
            assertThat(response.getBody().getData().get("zoomMeetings")).isEqualTo(4);
            verify(dataStore).getUserStatistics();
        }

        @Test
        @DisplayName("Should return empty stats when no data")
        void getDashboardStats_WhenNoData_ShouldReturnEmptyStats() {
            // Given
            Map<String, Object> emptyStats = new HashMap<>();
            emptyStats.put("totalMeetings", 0);
            emptyStats.put("totalTranscriptions", 0);
            emptyStats.put("totalActionItems", 0);
            emptyStats.put("discordMeetings", 0);
            emptyStats.put("zoomMeetings", 0);

            when(dataStore.getUserStatistics()).thenReturn(emptyStats);

            // When
            ResponseEntity<ApiResponse<Map<String, Object>>> response =
                    dashboardController.getDashboardStats();

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getData().get("totalMeetings")).isEqualTo(0);
        }
    }
}


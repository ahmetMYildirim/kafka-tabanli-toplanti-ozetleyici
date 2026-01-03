package cache;

import model.event.ProcessedActionItem;
import model.event.ProcessedSummary;
import model.event.ProcessedTranscription;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;


@ExtendWith(MockitoExtension.class)
@DisplayName("DataStore Unit Tests")
class DataStoreTest {

    private dataStore datastore;

    @BeforeEach
    void setUp(){
        datastore = new dataStore();
    }

    @Nested
    @DisplayName("Summary Operations")
    class SummaryTests {

        @Test
        @DisplayName("Should save and retrieve summary successfully")
        void saveSummary_ShouldStoreAndRetrieve(){
            ProcessedSummary summary = ProcessedSummary.builder()
                    .meetingId("meeting-123")
                    .platform("DISCORD")
                    .summary("Test özeti")
                    .processedTime(Instant.now())
                    .build();

            datastore.saveSummary(summary);
            Optional<ProcessedSummary> result = datastore.getSummary("meeting-123");

            assertThat(result).isPresent();
            assertThat(result.get().getMeetingId()).isEqualTo("meeting-123");
            assertThat(result.get().getPlatform()).isEqualTo("DISCORD");
        }

        @Test
        @DisplayName("Should return empty when summary not found")
        void getSummary_WhenNotExists_ShouldReturnEmpty(){
            Optional<ProcessedSummary> result = datastore.getSummary("not-existent");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should filter summaries by platform")
        void getSummariesByPlatform_ShouldFilterCorrectly(){
            ProcessedSummary discordSummary = ProcessedSummary.builder()
                    .meetingId("discord-1")
                    .platform("DISCORD")
                    .processedTime(Instant.now())
                    .build();

            ProcessedSummary zoomSummary = ProcessedSummary.builder()
                    .meetingId("zoom-1")
                    .platform("ZOOM")
                    .processedTime(Instant.now())
                    .build();

            datastore.saveSummary(discordSummary);
            datastore.saveSummary(zoomSummary);

            List<ProcessedSummary> discordResults = datastore.getSummariesByPlatform("DISCORD");

            assertThat(discordResults).hasSize(1);
            assertThat(discordResults.get(0).getPlatform()).isEqualTo("DISCORD");
        }

        @Test
        @DisplayName("Should return last summaries with limit")
        void getLastSummaries_ShouldRespectLimits(){
            for(int i = 0;i<5;i++){
                ProcessedSummary summary = ProcessedSummary.builder()
                        .meetingId("meeting-"+i)
                        .platform("DISCORD")
                        .processedTime(Instant.now())
                        .build();
                datastore.saveSummary(summary);
            }

            List<ProcessedSummary> results = datastore.getLastSummaries(3);

            assertThat(results).hasSize(3);
        }
    }

    @Nested
    @DisplayName("Transcription Operations")
    class TranscriptionTests{
        @Test
        @DisplayName("Should save and retrieve transcription successfully")
        void saveTranscription_ShouldStoreAndRetrieve(){
            ProcessedTranscription transcription = ProcessedTranscription.builder()
                    .meetingId("meeting-456")
                    .fullTranscription("Bu bir test transkriptidir.")
                    .processedTime(Instant.now())
                    .build();

            datastore.saveTranscription(transcription);
            Optional<ProcessedTranscription> result = datastore.getTranscription("meeting-456");

            assertThat(result).isPresent();
            assertThat(result.get().getFullTranscription()).isEqualTo("Bu bir test transkriptidir.");
        }

        @Test
        @DisplayName("Should return empty when transcription not found")
        void getTranscription_WhenNotExists_ShouldReturnEmpty(){
            Optional<ProcessedTranscription> result = datastore.getTranscription("not-existent-transcription");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("ActionItem Operations")
    class ActionItemTests{
        @Test
        @DisplayName("Should save and retrieve action items")
        void saveActionItems_ShouldStoreAndRetrieve() {
            ProcessedActionItem actionItem = ProcessedActionItem.builder()
                    .meetingId("meeting-789")
                    .actionItems(List.of("Görev 1", "Görev 2"))
                    .build();

            datastore.saveActionItems(actionItem);
            Optional<ProcessedActionItem> result = datastore.getActionItem("meeting-789");

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getActionItems()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Statistics Operations")
    class StatisticsTests {

        @Test
        @DisplayName("Should calculate user statistics correctly")
        void getUserStatistics_ShouldReturnCorrectStats() {
            ProcessedSummary summary = ProcessedSummary.builder()
                    .meetingId("meeting-1")
                    .platform("DISCORD")
                    .processedTime(Instant.now())
                    .build();
            datastore.saveSummary(summary);

            Map<String, Object> stats = datastore.getUserStatistics();

            assertThat(stats).containsKey("totalMeetings");
            assertThat(stats.get("totalMeetings")).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Cache Operations")
    class CacheTests {

        @Test
        @DisplayName("Should clear cache for specific meeting")
        void clearCache_ShouldRemoveAllMeetingData() {
            String meetingId = "meeting-clear-test";

            datastore.saveSummary(ProcessedSummary.builder()
                    .meetingId(meetingId)
                    .platform("DISCORD")
                    .processedTime(Instant.now())
                    .build());

            datastore.saveTranscription(ProcessedTranscription.builder()
                    .meetingId(meetingId)
                    .fullTranscription("Test")
                    .build());

            datastore.clearCache(meetingId);

            assertThat(datastore.getSummary(meetingId)).isEmpty();
            assertThat(datastore.getTranscription(meetingId)).isEmpty();
        }
    }
}

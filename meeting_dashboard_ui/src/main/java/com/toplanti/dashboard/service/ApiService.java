package com.toplanti.dashboard.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.reflect.TypeToken;
import com.toplanti.dashboard.model.*;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class ApiService {

    private static final Logger log = LoggerFactory.getLogger(ApiService.class);
    private static final String DEFAULT_BASE_URL = "http://localhost:8084/api/v1";

    private final OkHttpClient client;
    private final Gson gson;
    private final String baseUrl;
    private String authToken;

    public ApiService() {
        this(DEFAULT_BASE_URL);
    }

    public ApiService(String baseUrl) {
        this.baseUrl = baseUrl;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(300, TimeUnit.SECONDS)  
                .writeTimeout(300, TimeUnit.SECONDS)  
                .build();

        this.gson = new GsonBuilder()
                .registerTypeAdapter(Instant.class, (JsonDeserializer<Instant>)
                    (json, type, context) -> Instant.parse(json.getAsString()))
                .create();
    }

    public void setAuthToken(String token) {
        this.authToken = token;
    }

    public CompletableFuture<Map<String, Object>> getDashboardStats() {
        Type type = new TypeToken<ApiResponse<Map<String, Object>>>(){}.getType();
        return makeGetRequest("/dashboard/stats", type)
                .thenApply(response -> {
                    @SuppressWarnings("unchecked")
                    ApiResponse<Map<String, Object>> apiResponse = (ApiResponse<Map<String, Object>>) response;
                    return apiResponse.getData();
                });
    }

    public CompletableFuture<List<MeetingSummary>> getMeetings(String platform, int limit) {
        StringBuilder url = new StringBuilder("/meetings?limit=").append(limit);
        if (platform != null && !platform.isEmpty()) {
            url.append("&platform=").append(platform);
        }

        Type type = new TypeToken<ApiResponse<List<MeetingSummary>>>(){}.getType();
        return makeGetRequest(url.toString(), type)
                .thenApply(response -> {
                    @SuppressWarnings("unchecked")
                    ApiResponse<List<MeetingSummary>> apiResponse = (ApiResponse<List<MeetingSummary>>) response;
                    return apiResponse.getData();
                });
    }

    public CompletableFuture<MeetingDetail> getMeetingDetail(String meetingId) {
        Type type = new TypeToken<ApiResponse<MeetingDetail>>(){}.getType();
        return makeGetRequest("/meetings/" + meetingId, type)
                .thenApply(response -> {
                    @SuppressWarnings("unchecked")
                    ApiResponse<MeetingDetail> apiResponse = (ApiResponse<MeetingDetail>) response;
                    return apiResponse.getData();
                });
    }

    public CompletableFuture<MeetingSummary> getMeetingSummary(String meetingId) {
        Type type = new TypeToken<ApiResponse<MeetingSummary>>(){}.getType();
        return makeGetRequest("/meetings/" + meetingId + "/summary", type)
                .thenApply(response -> {
                    @SuppressWarnings("unchecked")
                    ApiResponse<MeetingSummary> apiResponse = (ApiResponse<MeetingSummary>) response;
                    return apiResponse.getData();
                });
    }

    public CompletableFuture<Transcription> getTranscription(String meetingId) {
        Type type = new TypeToken<ApiResponse<Transcription>>(){}.getType();
        return makeGetRequest("/meetings/" + meetingId + "/transcription", type)
                .thenApply(response -> {
                    @SuppressWarnings("unchecked")
                    ApiResponse<Transcription> apiResponse = (ApiResponse<Transcription>) response;
                    return apiResponse.getData();
                });
    }

    public CompletableFuture<ActionItem> getActionItems(String meetingId) {
        Type type = new TypeToken<ApiResponse<ActionItem>>(){}.getType();
        return makeGetRequest("/meetings/" + meetingId + "/action-items", type)
                .thenApply(response -> {
                    @SuppressWarnings("unchecked")
                    ApiResponse<ActionItem> apiResponse = (ApiResponse<ActionItem>) response;
                    return apiResponse.getData();
                });
    }

    public CompletableFuture<List<ActionItem>> getAllActionItems() {
        Type type = new TypeToken<ApiResponse<List<ActionItem>>>(){}.getType();
        return makeGetRequest("/meetings/action-items", type)
                .thenApply(response -> {
                    @SuppressWarnings("unchecked")
                    ApiResponse<List<ActionItem>> apiResponse = (ApiResponse<List<ActionItem>>) response;
                    return apiResponse.getData();
                });
    }

    /**
     * Ses/video dosyası yükler (Collector Service'e)
     * Flow: UI → Collector → Kafka → AI → MySQL → Gateway
     */
    public CompletableFuture<Map<String, Object>> uploadMedia(File file, String meetingTitle) {
        return CompletableFuture.supplyAsync(() -> {
            try {

                String collectorUrl = "http://localhost:8081/api/v1/media/upload";

                String metadataJson = String.format("{\"meetingId\":\"%s\",\"platform\":\"UI\",\"meetingTitle\":\"%s\"}",
                        java.util.UUID.randomUUID().toString(), meetingTitle);

                
                String fileName = file.getName().toLowerCase();
                String mimeType = "application/octet-stream";
                if (fileName.endsWith(".mp3")) {
                    mimeType = "audio/mpeg";
                } else if (fileName.endsWith(".wav")) {
                    mimeType = "audio/wav";
                } else if (fileName.endsWith(".mp4")) {
                    mimeType = "video/mp4";
                } else if (fileName.endsWith(".webm")) {
                    mimeType = "video/webm";
                } else if (fileName.endsWith(".ogg")) {
                    mimeType = "audio/ogg";
                } else if (fileName.endsWith(".m4a")) {
                    mimeType = "audio/mp4";
                }

                RequestBody requestBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("file", file.getName(),
                                RequestBody.create(file, MediaType.parse(mimeType)))
                        .addFormDataPart("metadata", metadataJson,
                                RequestBody.create(metadataJson, MediaType.parse("application/json")))
                        .build();

                Request request = new Request.Builder()
                        .url(collectorUrl)
                        .post(requestBody)
                        .build();

                log.info("Dosya yükleniyor: {} → Collector Service", file.getName());

                try (Response response = client.newCall(request).execute()) {
                    String responseBody = response.body() != null ? response.body().string() : "No body";

                    log.info("Upload response - HTTP {}: {}", response.code(), responseBody);

                    if (response.isSuccessful()) {
                        log.info("Dosya başarıyla yüklendi: {}", responseBody);

                        Type type = new TypeToken<Map<String, Object>>(){}.getType();
                        return gson.fromJson(responseBody, type);
                    } else {
                        log.error("Dosya yükleme başarısız: HTTP {} - Body: {}", response.code(), responseBody);
                        throw new RuntimeException("Dosya yükleme başarısız: HTTP " + response.code() + " - " + responseBody);
                    }
                }
            } catch (java.net.ConnectException e) {
                log.error("Collector Service'e bağlanılamadı. Servis çalışıyor mu? Port 8081", e);
                throw new RuntimeException("Collector Service'e bağlanılamadı. Lütfen servisin çalıştığından emin olun (Port 8081)", e);
            } catch (java.net.SocketTimeoutException e) {
                log.error("Dosya yükleme zaman aşımına uğradı", e);
                throw new RuntimeException("Dosya yükleme zaman aşımına uğradı. Dosya çok büyük olabilir veya ağ bağlantısı yavaş olabilir.", e);
            } catch (IOException e) {
                log.error("Dosya yükleme hatası", e);
                String errorMsg = e.getMessage();
                if (errorMsg != null && errorMsg.contains("Connection refused")) {
                    throw new RuntimeException("Collector Service'e bağlanılamadı. Servis çalışıyor mu? (Port 8081)", e);
                } else if (errorMsg != null && errorMsg.contains("aborted")) {
                    throw new RuntimeException("Bağlantı kesildi. Dosya çok büyük olabilir veya servis yanıt vermiyor.", e);
                }
                throw new RuntimeException("Dosya yükleme hatası: " + (errorMsg != null ? errorMsg : "Bilinmeyen hata"), e);
            }
        });
    }

    private <T> CompletableFuture<T> makeGetRequest(String endpoint, Type type) {
        return CompletableFuture.supplyAsync(() -> {
            Request.Builder builder = new Request.Builder()
                    .url(baseUrl + endpoint)
                    .get()
                    .addHeader("Accept", "application/json");

            if (authToken != null && !authToken.isEmpty()) {
                builder.addHeader("Authorization", "Bearer " + authToken);
            }

            Request request = builder.build();

            log.debug("API isteği: GET {}", baseUrl + endpoint);

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    log.debug("API yanıtı: {}", responseBody);
                    return gson.fromJson(responseBody, type);
                } else {
                    log.error("API isteği başarısız: {} - {}", response.code(), response.message());
                    throw new RuntimeException("API isteği başarısız: " + response.code());
                }
            } catch (java.net.ConnectException e) {
                log.error("Gateway API'ye bağlanılamadı. Servis çalışıyor mu? Port 8084", e);
                throw new RuntimeException("Gateway API'ye bağlanılamadı. Lütfen servisin çalıştığından emin olun (Port 8084)", e);
            } catch (java.net.SocketTimeoutException e) {
                log.error("API isteği zaman aşımına uğradı", e);
                throw new RuntimeException("API isteği zaman aşımına uğradı. Servis yanıt vermiyor olabilir.", e);
            } catch (IOException e) {
                log.error("API isteği hatası", e);
                String errorMsg = e.getMessage();
                if (errorMsg != null && errorMsg.contains("Connection refused")) {
                    throw new RuntimeException("Gateway API'ye bağlanılamadı. Servis çalışıyor mu? (Port 8084)", e);
                }
                throw new RuntimeException("API isteği hatası: " + (errorMsg != null ? errorMsg : "Bilinmeyen hata"), e);
            }
        });
    }

    public static class ApiResponse<T> {
        private boolean success;
        private T data;
        private String message;
        private String timestamp;

        public T getData() {
            return data;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public String getTimestamp() {
            return timestamp;
        }
    }
}

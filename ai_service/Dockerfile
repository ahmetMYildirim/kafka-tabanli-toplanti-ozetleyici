# ===================================================================
# AI Service - Dockerfile
# OpenAI Whisper + GPT-4 based AI processing service
# ===================================================================

# Stage 1: Build
FROM gradle:8.5-jdk17 AS builder

WORKDIR /app

# Copy Gradle files first for caching
COPY build.gradle.kts settings.gradle.kts ./
COPY gradle ./gradle
COPY gradlew gradlew.bat ./

# Download dependencies (cached if gradle files don't change)
RUN gradle dependencies --no-daemon || true

# Copy source code
COPY src ./src

# Build the application
RUN gradle -Dhttps.protocols=TLSv1.2,TLSv1.1,TLSv1 bootJar --no-daemon -x test

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-jammy

# Labels
LABEL maintainer="Toplanti Ozetleyici Team"
LABEL description="AI Service - OpenAI Whisper (transcription) + GPT-4 (summarization & task extraction)"
LABEL version="1.0.0"

WORKDIR /app

# Install curl for health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Create non-root user for security
RUN groupadd -r appgroup && useradd -r -g appgroup appuser

# Copy the built JAR from builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Create directories for logs and audio storage
RUN mkdir -p /app/logs /app/audio_storage && \
    chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Expose port
EXPOSE 8083

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8083/actuator/health || exit 1

# JVM options for containerized environment
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:InitialRAMPercentage=50.0 \
    -XX:MaxRAMPercentage=75.0 \
    -Djava.security.egd=file:/dev/./urandom \
    -Dspring.profiles.active=docker"

# Entry point
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

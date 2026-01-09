# Multi-stage build for Collector Service

# Stage 1: Build Java application
FROM gradle:8.5-jdk17 AS builder

WORKDIR /app

# Copy Gradle files first for caching
COPY build.gradle.kts settings.gradle.kts gradle.properties ./
COPY gradle ./gradle

# Download dependencies (cached if gradle files don't change)
RUN gradle dependencies --no-daemon || true

# Copy source code
COPY src ./src

# Build the application
RUN gradle -Dhttps.protocols=TLSv1.2,TLSv1.1,TLSv1 bootJar --no-daemon -x test

# Stage 2: Runtime image
FROM eclipse-temurin:17-jre-jammy

# Install curl and FFmpeg
RUN apt-get update && apt-get install -y \
    curl \
    ffmpeg \
    ca-certificates \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Create audio storage directory
RUN mkdir -p /app/audio_storage /app/media-storage

# Set environment variables
ENV SPRING_PROFILES_ACTIVE=docker
ENV JAVA_OPTS="-Xmx512m -Xms256m"

# Expose port
EXPOSE 8081

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8081/actuator/health || exit 1

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
